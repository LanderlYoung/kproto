/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.wire.schema.internal.parser

import com.google.common.collect.List
import com.google.common.collect.Range
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.internal.Util
import java.util.ArrayList

/** Basic parser for `.proto` schema declarations.  */
class ProtoParser internal constructor(location: Location, data: CharArray) {
    private val reader: SyntaxReader

    private val fileBuilder: ProtoFileElement.Builder
    private val publicImports = List.builder<String>()
    private val imports = List.builder<String>()
    private val nestedTypes = List.builder<TypeElement>()
    private val services = List.builder<ServiceElement>()
    private val extendsList = List.builder<ExtendElement>()
    private val options = List.builder<OptionElement>()

    /** The number of declarations defined in the current file.  */
    private var declarationCount = 0

    /** The syntax of the file, or null if none is defined.  */
    private var syntax: ProtoFile.Syntax? = null

    /** Output package name, or null if none yet encountered.  */
    private var packageName: String? = null

    /** The current package name + nested type names, separated by dots.  */
    private var prefix = ""

    init {
        this.reader = SyntaxReader(data, location)
        this.fileBuilder = ProtoFileElement.builder(location)
    }

    internal fun readProtoFile(): ProtoFileElement {
        while (true) {
            val documentation = reader.readDocumentation()
            if (reader.exhausted()) {
                return fileBuilder.syntax(syntax)
                        .publicImports(publicImports.build())
                        .imports(imports.build())
                        .types(nestedTypes.build())
                        .services(services.build())
                        .extendDeclarations(extendsList.build())
                        .options(options.build())
                        .build()
            }
            val declaration = readDeclaration(documentation, Context.FILE)
            if (declaration is TypeElement) {
                nestedTypes.add((declaration as TypeElement?)!!)
            } else if (declaration is ServiceElement) {
                services.add((declaration as ServiceElement?)!!)
            } else if (declaration is OptionElement) {
                options.add((declaration as OptionElement?)!!)
            } else if (declaration is ExtendElement) {
                extendsList.add((declaration as ExtendElement?)!!)
            }
        }
    }

    private fun readDeclaration(documentation: String, context: Context): Any? {
        val index = declarationCount++

        // Skip unnecessary semicolons, occasionally used after a nested message declaration.
        if (reader.peekChar(';')) return null

        val location = reader.location()
        val label = reader.readWord()

        if (label == "package") {
            if (!context.permitsPackage()) throw reader.unexpected(location, "'package' in $context")
            if (packageName != null) throw reader.unexpected(location, "too many package names")
            packageName = reader.readName()
            fileBuilder.packageName(packageName)
            prefix = packageName!! + "."
            reader.require(';')
            return null
        } else if (label == "import") {
            if (!context.permitsImport()) throw reader.unexpected(location, "'import' in $context")
            val importString = reader.readString()
            if ("public" == importString) {
                publicImports.add(reader.readString())
            } else {
                imports.add(importString)
            }
            reader.require(';')
            return null
        } else if (label == "syntax") {
            if (!context.permitsSyntax()) throw reader.unexpected(location, "'syntax' in $context")
            reader.require('=')
            if (index != 0) {
                throw reader.unexpected(
                        location, "'syntax' element must be the first declaration in a file")
            }
            val syntaxString = reader.readQuotedString()
            try {
                syntax = ProtoFile.Syntax.get(syntaxString)
            } catch (e: IllegalArgumentException) {
                throw reader.unexpected(location, e.message)
            }

            reader.require(';')
            return null
        } else if (label == "option") {
            val result = OptionReader(reader).readOption('=')
            reader.require(';')
            return result
        } else if (label == "reserved") {
            return readReserved(location, documentation)
        } else if (label == "message") {
            return readMessage(location, documentation)
        } else if (label == "enum") {
            return readEnumElement(location, documentation)
        } else if (label == "service") {
            return readService(location, documentation)
        } else if (label == "extend") {
            return readExtend(location, documentation)
        } else if (label == "rpc") {
            if (!context.permitsRpc()) throw reader.unexpected(location, "'rpc' in $context")
            return readRpc(location, documentation)
        } else if (label == "oneof") {
            if (!context.permitsOneOf()) {
                throw reader.unexpected(location, "'oneof' must be nested in message")
            }
            return readOneOf(documentation)
        } else if (label == "extensions") {
            if (!context.permitsExtensions()) {
                throw reader.unexpected(location, "'extensions' must be nested")
            }
            return readExtensions(location, documentation)
        } else return if (context == Context.MESSAGE || context == Context.EXTEND) {
            readField(documentation, location, label)
        } else if (context == Context.ENUM) {
            readEnumConstant(documentation, location, label)
        } else {
            throw reader.unexpected(location, "unexpected label: $label")
        }
    }

    /** Reads a message declaration.  */
    private fun readMessage(location: Location, documentation: String): MessageElement {
        val name = reader.readName()
        val builder = MessageElement.builder(location)
                .name(name)
                .documentation(documentation)

        val previousPrefix = prefix
        prefix = "$prefix$name."

        val fields = List.builder<FieldElement>()
        val oneOfs = List.builder<OneOfElement>()
        val nestedTypes = List.builder<TypeElement>()
        val extensions = List.builder<ExtensionsElement>()
        val options = List.builder<OptionElement>()
        val reserveds = List.builder<ReservedElement>()
        val groups = List.builder<GroupElement>()

        reader.require('{')
        while (true) {
            val nestedDocumentation = reader.readDocumentation()
            if (reader.peekChar('}')) break

            val declared = readDeclaration(nestedDocumentation, Context.MESSAGE)
            if (declared is FieldElement) {
                fields.add((declared as FieldElement?)!!)
            } else if (declared is OneOfElement) {
                oneOfs.add((declared as OneOfElement?)!!)
            } else if (declared is GroupElement) {
                groups.add((declared as GroupElement?)!!)
            } else if (declared is TypeElement) {
                nestedTypes.add((declared as TypeElement?)!!)
            } else if (declared is ExtensionsElement) {
                extensions.add((declared as ExtensionsElement?)!!)
            } else if (declared is OptionElement) {
                options.add((declared as OptionElement?)!!)
            } else if (declared is ExtendElement) {
                // Extend declarations always add in a global scope regardless of nesting.
                extendsList.add((declared as ExtendElement?)!!)
            } else if (declared is ReservedElement) {
                reserveds.add((declared as ReservedElement?)!!)
            }
        }
        prefix = previousPrefix

        return builder.fields(fields.build())
                .oneOfs(oneOfs.build())
                .nestedTypes(nestedTypes.build())
                .extensions(extensions.build())
                .options(options.build())
                .reserveds(reserveds.build())
                .groups(groups.build())
                .build()
    }

    /** Reads an extend declaration.  */
    private fun readExtend(location: Location, documentation: String): ExtendElement {
        val name = reader.readName()
        val builder = ExtendElement.builder(location)
                .name(name)
                .documentation(documentation)

        reader.require('{')
        val fields = List.builder<FieldElement>()
        while (true) {
            val nestedDocumentation = reader.readDocumentation()
            if (reader.peekChar('}')) break

            val declared = readDeclaration(nestedDocumentation, Context.EXTEND)
            if (declared is FieldElement) {
                fields.add((declared as FieldElement?)!!)
            }
        }
        return builder.fields(fields.build())
                .build()
    }

    /** Reads a service declaration and returns it.  */
    private fun readService(location: Location, documentation: String): ServiceElement {
        val name = reader.readName()
        val builder = ServiceElement.builder(location)
                .name(name)
                .documentation(documentation)

        reader.require('{')
        val rpcs = List.builder<RpcElement>()
        val options = List.builder<OptionElement>()
        while (true) {
            val rpcDocumentation = reader.readDocumentation()
            if (reader.peekChar('}')) break

            val declared = readDeclaration(rpcDocumentation, Context.SERVICE)
            if (declared is RpcElement) {
                rpcs.add((declared as RpcElement?)!!)
            } else if (declared is OptionElement) {
                options.add((declared as OptionElement?)!!)
            }
        }
        return builder.options(options.build())
                .rpcs(rpcs.build())
                .build()
    }

    /** Reads an enumerated type declaration and returns it.  */
    private fun readEnumElement(location: Location, documentation: String): EnumElement {
        val name = reader.readName()
        val builder = EnumElement.builder(location)
                .name(name)
                .documentation(documentation)

        val constants = List.builder<EnumConstantElement>()
        val options = List.builder<OptionElement>()
        reader.require('{')
        while (true) {
            val valueDocumentation = reader.readDocumentation()
            if (reader.peekChar('}')) break

            val declared = readDeclaration(valueDocumentation, Context.ENUM)
            if (declared is EnumConstantElement) {
                constants.add((declared as EnumConstantElement?)!!)
            } else if (declared is OptionElement) {
                options.add((declared as OptionElement?)!!)
            }
        }
        return builder.options(options.build())
                .constants(constants.build())
                .build()
    }

    private fun readField(documentation: String, location: Location, word: String): Any {
        val label: Field.Label?
        val type: String
        when (word) {
            "required" -> {
                if (syntax == ProtoFile.Syntax.PROTO_3) {
                    throw reader.unexpected(
                            location, "'required' label forbidden in proto3 field declarations")
                }
                label = Field.Label.REQUIRED
                type = reader.readDataType()
            }

            "optional" -> {
                if (syntax == ProtoFile.Syntax.PROTO_3) {
                    throw reader.unexpected(
                            location, "'optional' label forbidden in proto3 field declarations")
                }
                label = Field.Label.OPTIONAL
                type = reader.readDataType()
            }

            "repeated" -> {
                label = Field.Label.REPEATED
                type = reader.readDataType()
            }

            else -> {
                if (syntax != ProtoFile.Syntax.PROTO_3 && (word != "map" || reader.peekChar() != '<')) {
                    throw reader.unexpected(location, "unexpected label: $word")
                }
                label = null
                type = reader.readDataType(word)
            }
        }

        if (type.startsWith("map<") && label != null) {
            throw reader.unexpected(location, "'map' type cannot have label")
        }
        return if (type == "group") {
            readGroup(location, documentation, label)
        } else readField(location, documentation, label, type)

    }

    /** Reads an field declaration and returns it.  */
    private fun readField(
            location: Location, documentation: String, @Nullable label: Field.Label?, type: String): FieldElement {
        var documentation = documentation
        val name = reader.readName()
        reader.require('=')
        val tag = reader.readInt()

        val builder = FieldElement.builder(location)
                .label(label)
                .type(type)
                .name(name)
                .tag(tag)

        var options: MutableList<OptionElement> = OptionReader(reader).readOptions()
        reader.require(';')

        options = ArrayList(options) // Mutable copy for extractDefault.
        val defaultValue = stripDefault(options)

        documentation = reader.tryAppendTrailingDocumentation(documentation)
        return builder.documentation(documentation)
                .defaultValue(defaultValue)
                .options(List.copyOf(options))
                .build()
    }

    /**
     * Defaults aren't options. This finds an option named "default", removes, and returns it. Returns
     * null if no default option is present.
     */
    @Nullable
    private fun stripDefault(options: MutableList<OptionElement>): String? {
        var result: String? = null
        val i = options.iterator()
        while (i.hasNext()) {
            val option = i.next()
            if (option.name() == "default") {
                i.remove()
                result = option.value().toString() // Defaults aren't options!
            }
        }
        return result
    }

    private fun readOneOf(documentation: String): OneOfElement {
        val builder = OneOfElement.builder()
                .name(reader.readName())
                .documentation(documentation)
        val fields = List.builder<FieldElement>()
        val groups = List.builder<GroupElement>()

        reader.require('{')
        while (true) {
            val nestedDocumentation = reader.readDocumentation()
            if (reader.peekChar('}')) break

            val location = reader.location()
            val type = reader.readDataType()
            if (type == "group") {
                groups.add(readGroup(location, nestedDocumentation, null))
            } else {
                fields.add(readField(location, nestedDocumentation, null, type))
            }
        }
        return builder.fields(fields.build())
                .groups(groups.build())
                .build()
    }

    private fun readGroup(location: Location, documentation: String, label: Field.Label?): GroupElement {
        val name = reader.readWord()
        reader.require('=')
        val tag = reader.readInt()

        val builder = GroupElement.builder(location)
                .label(label)
                .name(name)
                .tag(tag)
                .documentation(documentation)
        val fields = List.builder<FieldElement>()

        reader.require('{')
        while (true) {
            val nestedDocumentation = reader.readDocumentation()
            if (reader.peekChar('}')) break

            val fieldLocation = reader.location()
            val fieldLabel = reader.readWord()
            val field = readField(nestedDocumentation, fieldLocation, fieldLabel)
            if (field !is FieldElement) {
                throw reader.unexpected("expected field declaration, was $field")
            }
            fields.add(field)
        }

        return builder.fields(fields.build())
                .build()
    }

    /** Reads a reserved tags and names list like "reserved 10, 12 to 14, 'foo';".  */
    private fun readReserved(location: Location, documentation: String): ReservedElement {
        val valuesBuilder = List.builder<Any>()

        while (true) {
            var c = reader.peekChar()
            if (c == '"' || c == '\'') {
                valuesBuilder.add(reader.readQuotedString())
            } else {
                val tagStart = reader.readInt()

                c = reader.peekChar()
                if (c != ',' && c != ';') {
                    if (reader.readWord() != "to") {
                        throw reader.unexpected("expected ',', ';', or 'to'")
                    }
                    val tagEnd = reader.readInt()
                    valuesBuilder.add(Range.closed(tagStart, tagEnd))
                } else {
                    valuesBuilder.add(tagStart)
                }
            }
            c = reader.readChar()
            if (c == ';') break
            if (c != ',') throw reader.unexpected("expected ',' or ';'")
        }

        val values = valuesBuilder.build()
        if (values.isEmpty()) {
            throw reader.unexpected("'reserved' must have at least one field name or tag")
        }
        return ReservedElement.create(location, documentation, values)
    }

    /** Reads extensions like "extensions 101;" or "extensions 101 to max;".  */
    private fun readExtensions(location: Location, documentation: String): ExtensionsElement {
        val start = reader.readInt() // Range start.
        var end = start
        if (reader.peekChar() != ';') {
            if ("to" != reader.readWord()) throw reader.unexpected("expected ';' or 'to'")
            val s = reader.readWord() // Range end.
            if (s == "max") {
                end = Util.MAX_TAG_VALUE
            } else {
                end = Integer.parseInt(s)
            }
        }
        reader.require(';')
        return ExtensionsElement.create(location, start, end, documentation)
    }

    /** Reads an enum constant like "ROCK = 0;". The label is the constant name.  */
    private fun readEnumConstant(
            documentation: String, location: Location, label: String): EnumConstantElement {
        var documentation = documentation
        reader.require('=')

        val tag = reader.readInt()

        val options = OptionReader(reader).readOptions()
        reader.require(';')
        documentation = reader.tryAppendTrailingDocumentation(documentation)

        return EnumConstantElement.builder(location)
                .name(label)
                .tag(tag)
                .documentation(documentation)
                .options(options)
                .build()
    }

    /** Reads an rpc and returns it.  */
    private fun readRpc(location: Location, documentation: String): RpcElement {
        val builder = RpcElement.builder(location)
                .name(reader.readName())
                .documentation(documentation)

        reader.require('(')
        var type: String
        var word = reader.readWord()
        if (word == "stream") {
            builder.requestStreaming(true)
            type = reader.readDataType()
        } else {
            type = reader.readDataType(word)
        }
        builder.requestType(type)
        reader.require(')')

        if (reader.readWord() != "returns") throw reader.unexpected("expected 'returns'")

        reader.require('(')
        word = reader.readWord()
        if (word == "stream") {
            builder.responseStreaming(true)
            type = reader.readDataType()
        } else {
            type = reader.readDataType(word)
        }
        builder.responseType(type)
        reader.require(')')

        if (reader.peekChar('{')) {
            val options = List.builder<OptionElement>()
            while (true) {
                val rpcDocumentation = reader.readDocumentation()
                if (reader.peekChar('}')) {
                    break
                }
                val declared = readDeclaration(rpcDocumentation, Context.RPC)
                if (declared is OptionElement) {
                    options.add((declared as OptionElement?)!!)
                }
            }
            builder.options(options.build())
        } else {
            reader.require(';')
        }

        return builder.build()
    }

    internal enum class Context {
        FILE,
        MESSAGE,
        ENUM,
        RPC,
        EXTEND,
        SERVICE;

        fun permitsPackage(): Boolean {
            return this == FILE
        }

        fun permitsSyntax(): Boolean {
            return this == FILE
        }

        fun permitsImport(): Boolean {
            return this == FILE
        }

        fun permitsExtensions(): Boolean {
            return this != FILE
        }

        fun permitsRpc(): Boolean {
            return this == SERVICE
        }

        fun permitsOneOf(): Boolean {
            return this == MESSAGE
        }
    }

    companion object {

        /** Parse a named `.proto` schema.  */
        fun parse(location: Location, data: String): ProtoFileElement {
            return ProtoParser(location, CharArray(data.length) { data[it] }).readProtoFile()
        }
    }
}
