/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema

import com.google.common.collect.List
import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import com.squareup.wire.schema.internal.Util
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashMap

/** Links local field types and option types to the corresponding declarations.  */
internal class Linker {
    private val protoFiles: List<ProtoFile>
    private val protoTypeNames: MutableMap<String, Type>
    private val imports: Multimap<String, String>
    private val errors: MutableList<String>
    private val contextStack: List<Any>

    constructor(protoFiles: Iterable<ProtoFile>) {
        this.protoFiles = List.copyOf(protoFiles)
        this.protoTypeNames = LinkedHashMap()
        this.imports = LinkedHashMultimap.create()
        this.contextStack = emptyList<Any>()
        this.errors = ArrayList()
    }

    private constructor(enclosing: Linker, additionalContext: Any) {
        this.protoFiles = enclosing.protoFiles
        this.protoTypeNames = enclosing.protoTypeNames
        this.imports = enclosing.imports
        this.contextStack = Util.concatenate(enclosing.contextStack, additionalContext)
        this.errors = enclosing.errors
    }

    fun link(): Schema {
        // Register the types.
        for (protoFile in protoFiles) {
            for (type in protoFile.types()) {
                register(type)
            }
        }

        // Link extensions. This depends on type registration.
        for (protoFile in protoFiles) {
            val linker = withContext(protoFile)
            for (extend in protoFile.extendList()) {
                extend.link(linker)
            }
        }

        // Link proto types and services.
        for (protoFile in protoFiles) {
            val linker = withContext(protoFile)
            for (type in protoFile.types()) {
                type.link(linker)
            }
            for (service in protoFile.services()) {
                service.link(linker)
            }
        }

        // Link options. We can't link any options until we've linked all fields!
        for (protoFile in protoFiles) {
            val linker = withContext(protoFile)
            protoFile.linkOptions(linker)
            for (type in protoFile.types()) {
                type.linkOptions(linker)
            }
            for (service in protoFile.services()) {
                service.linkOptions(linker)
            }
        }

        // Compute public imports so we know that importing a.proto also imports b.proto and c.proto.
        val publicImports = LinkedHashMultimap.create<String, String>()
        for (protoFile in protoFiles) {
            publicImports.putAll(protoFile.location().path(), protoFile.publicImports())
        }
        // For each proto, gather its imports and its transitive imports.
        for (protoFile in protoFiles) {
            val sink = imports.get(protoFile.location().path())
            addImports(sink, protoFile.imports(), publicImports)
            addImports(sink, protoFile.publicImports(), publicImports)
        }

        // Validate the linked schema.
        for (protoFile in protoFiles) {
            val linker = withContext(protoFile)
            for (type in protoFile.types()) {
                type.validate(linker)
            }
            for (service in protoFile.services()) {
                service.validate(linker)
            }
            for (extend in protoFile.extendList()) {
                extend.validate(linker)
            }
        }

        if (!errors.isEmpty()) {
            throw SchemaException(errors)
        }

        return Schema(protoFiles)
    }

    /** Add all paths in `paths` to `sink`, plus their public imports, recursively.  */
    private fun addImports(sink: MutableCollection<String>,
                           paths: Collection<String>, publicImports: Multimap<String, String>) {
        for (path in paths) {
            if (sink.add(path)) {
                addImports(sink, publicImports.get(path), publicImports)
            }
        }
    }

    private fun register(type: Type) {
        protoTypeNames[type.type().toString()] = type
        for (nestedType in type.nestedTypes()) {
            register(nestedType)
        }
    }

    /** Returns the type name for the scalar, relative or fully-qualified name `name`.  */
    fun resolveType(name: String): ProtoType {
        return resolveType(name, false)
    }

    /** Returns the type name for the relative or fully-qualified name `name`.  */
    fun resolveMessageType(name: String): ProtoType {
        return resolveType(name, true)
    }

    private fun resolveType(name: String, messageOnly: Boolean): ProtoType {
        val type = ProtoType.get(name)
        if (type.isScalar) {
            if (messageOnly) {
                addError("expected a message but was %s", name)
            }
            return type
        }

        if (type.isMap) {
            if (messageOnly) {
                addError("expected a message but was %s", name)
            }
            val keyType = resolveType(type.keyType()!!.toString(), false)
            val valueType = resolveType(type.valueType()!!.toString(), false)
            return ProtoType(keyType, valueType, name)
        }

        val resolved = resolve(name, protoTypeNames)
        if (resolved == null) {
            addError("unable to resolve %s", name)
            return ProtoType.BYTES // Just return any placeholder.
        }

        if (messageOnly && resolved !is MessageType) {
            addError("expected a message but was %s", name)
            return ProtoType.BYTES // Just return any placeholder.
        }

        return resolved.type()
    }

    fun <T> resolve(name: String, map: Map<String, T>): T? {
        if (name.startsWith(".")) {
            // If name starts with a '.', the rest of it is fully qualified.
            val result = map[name.substring(1)]
            if (result != null) return result
        } else {
            // We've got a name suffix, like 'Person' or 'protos.Person'. Start the search from with the
            // longest prefix like foo.bar.Baz.Quux, shortening the prefix until we find a match.
            var prefix = resolveContext()
            while (!prefix.isEmpty()) {
                val result = map[prefix + '.'.toString() + name]
                if (result != null) return result

                // Strip the last nested class name or package name from the end and try again.
                val dot = prefix.lastIndexOf('.')
                prefix = if (dot != -1) prefix.substring(0, dot) else ""
            }
            val result = map[name]
            if (result != null) return result
        }
        return null
    }

    private fun resolveContext(): String {
        for (i in contextStack.indices.reversed()) {
            val context = contextStack[i]
            if (context is Type) {
                return context.type().toString()
            } else if (context is ProtoFile) {
                val packageName = context.packageName()
                return packageName ?: ""
            } else if (context is Field && context.isExtension) {
                val packageName = context.packageName()
                return packageName ?: ""
            }
        }
        throw IllegalStateException()
    }

    /** Returns the current package name from the context stack.  */
    fun packageName(): String? {
        for (context in contextStack) {
            if (context is ProtoFile) return context.packageName()
        }
        return null
    }

    /** Returns the type or null if it doesn't exist.  */
    operator fun get(protoType: ProtoType): Type {
        return protoTypeNames[protoType.toString()]
    }

    /** Returns the field named `field` on the message type of `self`.  */
    fun dereference(self: Field, field: String): Field? {
        var field = field
        if (field.startsWith("[") && field.endsWith("]")) {
            field = field.substring(1, field.length - 1)
        }

        val type = protoTypeNames[self.type()!!.toString()]
        if (type is MessageType) {
            val messageField = type.field(field)
            if (messageField != null) return messageField

            val typeExtensions = type.extensionFieldsMap()
            val extensionField = resolve(field, typeExtensions)
            if (extensionField != null) return extensionField
        }

        return null // Unable to traverse this field path.
    }

    /** Validate that the tags of `fields` are unique and in range.  */
    fun validateFields(fields: Iterable<Field>, reserveds: List<Reserved>) {
        val tagToField = LinkedHashMultimap.create<Int, Field>()
        val nameToField = LinkedHashMultimap.create<String, Field>()
        for (field in fields) {
            val tag = field.tag()
            if (!Util.isValidTag(tag)) {
                withContext(field).addError("tag is out of range: %s", tag)
            }

            for (reserved in reserveds) {
                if (reserved.matchesTag(tag)) {
                    withContext(field).addError("tag %s is reserved (%s)", tag, reserved.location())
                }
                if (reserved.matchesName(field.name())) {
                    withContext(field).addError("name '%s' is reserved (%s)", field.name(),
                            reserved.location())
                }
            }

            tagToField.put(tag, field)
            nameToField.put(field.qualifiedName(), field)
        }

        for ((key, value) in tagToField.asMap()) {
            if (value.size > 1) {
                val error = StringBuilder()
                error.append(String.format("multiple fields share tag %s:", key))
                var index = 1
                for (field in value) {
                    error.append(String.format("\n  %s. %s (%s)",
                            index++, field.name(), field.location()))
                }
                addError("%s", error)
            }
        }

        for (collidingFields in nameToField.asMap().values) {
            if (collidingFields.size > 1) {
                val first = collidingFields.iterator().next()
                val error = StringBuilder()
                error.append(String.format("multiple fields share name %s:", first.name()))
                var index = 1
                for (field in collidingFields) {
                    error.append(String.format("\n  %s. %s (%s)",
                            index++, field.name(), field.location()))
                }
                addError("%s", error)
            }
        }
    }

    fun validateEnumConstantNameUniqueness(nestedTypes: Iterable<Type>) {
        val nameToType = LinkedHashMultimap.create<String, EnumType>()
        for (type in nestedTypes) {
            if (type is EnumType) {
                for (enumConstant in type.constants()) {
                    nameToType.put(enumConstant.name(), type)
                }
            }
        }

        for ((constant, value) in nameToType.asMap()) {
            if (value.size > 1) {
                val error = StringBuilder()
                var index = 1
                error.append(String.format("multiple enums share constant %s:", constant))
                for (enumType in value) {
                    error.append(String.format("\n  %s. %s.%s (%s)",
                            index++, enumType.type(), constant, enumType.constant(constant)!!.location()))
                }
                addError("%s", error)
            }
        }
    }

    fun validateImport(location: Location, type: ProtoType) {
        var type = type
        // Map key type is always scalar. No need to validate it.
        if (type.isMap) type = type.valueType()

        if (type.isScalar) return

        val path = location.path()
        val requiredImport = get(type).location().path()
        if (path != requiredImport && !imports.containsEntry(path, requiredImport)) {
            addError("%s needs to import %s", path, requiredImport)
        }
    }

    /** Returns a new linker that uses `context` to resolve type names and report errors.  */
    fun withContext(context: Any): Linker {
        return Linker(this, context)
    }

    fun addError(format: String, vararg args: Any) {
        val error = StringBuilder()
        error.append(String.format(format, *args))

        for (i in contextStack.indices.reversed()) {
            val context = contextStack[i]
            val prefix = if (i == contextStack.size - 1) "\n  for" else "\n  in"

            if (context is Rpc) {
                error.append(String.format("%s rpc %s (%s)", prefix, context.name(), context.location()))

            } else if (context is Extend) {
                val type = context.type()
                error.append(if (type != null)
                    String.format("%s extend %s (%s)", prefix, type, context.location())
                else
                    String.format("%s extend (%s)", prefix, context.location()))

            } else if (context is Field) {
                error.append(String.format("%s field %s (%s)", prefix, context.name(), context.location()))

            } else if (context is MessageType) {
                error.append(String.format("%s message %s (%s)",
                        prefix, context.type(), context.location()))

            } else if (context is EnumType) {
                error.append(String.format("%s enum %s (%s)",
                        prefix, context.type(), context.location()))

            } else if (context is Service) {
                error.append(String.format("%s service %s (%s)",
                        prefix, context.type(), context.location()))

            } else if (context is Extensions) {
                error.append(String.format("%s extensions (%s)",
                        prefix, context.location()))
            }
        }

        errors.add(error.toString())
    }
}
