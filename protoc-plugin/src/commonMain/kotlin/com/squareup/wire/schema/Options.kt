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
import com.google.common.collect.ImmutableMap
import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import com.squareup.wire.schema.internal.parser.OptionElement
import java.util.LinkedHashMap
import java.util.regex.Matcher
import java.util.regex.Pattern

import com.google.common.base.Preconditions.checkNotNull
import com.google.common.collect.Iterables.getOnlyElement

/**
 * A set of options declared on a message declaration, field declaration, enum declaration, enum
 * constant declaration, service declaration, RPC method declaration, or proto file declaration.
 * Options values may be arbitrary protocol buffer messages, but must be valid protocol buffer
 * messages.
 */
class Options(private val optionType: ProtoType, elements: List<OptionElement>) {
    private val optionElements: List<OptionElement>
    private var map: Map<ProtoMember, Any>? = null

    init {
        this.optionElements = elements
    }

    /**
     * Returns a map with the values for these options. Map values may be either a single entry, like
     * `{deprecated: "true"}`, or more sophisticated, with nested maps and lists.
     *
     *
     * The map keys are always [ProtoMember] instances, even for nested maps. The values are
     * always either lists, maps, or strings.
     */
    fun map(): Map<ProtoMember, Any>? {
        return map
    }

    operator fun get(protoMember: ProtoMember): Any {
        checkNotNull(protoMember, "protoMember")
        return map!![protoMember]
    }

    /**
     * Returns true if any of the options in `options` matches both of the regular expressions
     * provided: its name matches the option's name and its value matches the option's value.
     */
    fun optionMatches(namePattern: String, valuePattern: String): Boolean {
        val nameMatcher = Regex.fromLiteral(namePattern).matchEntire("")
        val valueMatcher = Pattern.compile(valuePattern).matcher("")
        for ((key, value) in map!!) {
            if (nameMatcher.reset(key.member()).matches() && valueMatcher.reset(value.toString()).matches()) {
                return true
            }
        }
        return false
    }

    internal fun toElements(): List<OptionElement> {
        return optionElements
    }

    internal fun link(linker: Linker) {
        var map = ImmutableMap.of<ProtoMember, Any>()
        for (option in optionElements) {
            val canonicalOption = canonicalizeOption(linker, optionType, option)
            if (canonicalOption != null) {
                map = union(linker, map, canonicalOption)
            }
        }

        this.map = map
    }

    internal fun canonicalizeOption(
            linker: Linker, extensionType: ProtoType, option: OptionElement): Map<ProtoMember, Any>? {
        val type = linker.get(extensionType) as? MessageType
                ?: return null // No known extensions for the given extension type.

        var path: Array<String>?
        var field = type.field(option.name())
        if (field != null) {
            // This is an option declared by descriptor.proto.
            path = arrayOf(option.name())
        } else {
            // This is an option declared by an extension.
            val extensionsForType = type.extensionFieldsMap()

            path = resolveFieldPath(option.name(), extensionsForType.keys)
            val packageName = linker.packageName()
            if (path == null && packageName != null) {
                // If the path couldn't be resolved, attempt again by prefixing it with the package name.
                path = resolveFieldPath(packageName + "." + option.name(), extensionsForType.keys)
            }
            if (path == null) {
                return null // Unable to find the root of this field path.
            }

            field = extensionsForType[path[0]]
        }

        val result = LinkedHashMap<ProtoMember, Any>()
        var last: MutableMap<ProtoMember, Any> = result
        var lastProtoType: ProtoType? = type.type()
        for (i in 1 until path.size) {
            val nested = LinkedHashMap<ProtoMember, Any>()
            last[ProtoMember.get(lastProtoType, field!!)] = nested
            lastProtoType = field.type()
            last = nested
            field = linker.dereference(field, path[i])
            if (field == null) {
                return null // Unable to dereference this path segment.
            }
        }

        last[ProtoMember.get(lastProtoType, field!!)] = canonicalizeValue(linker, field, option.value())
        return result
    }

    private fun canonicalizeValue(linker: Linker, context: Field, value: Any): Any {
        if (value is OptionElement) {
            val result = ImmutableMap.builder<ProtoMember, Any>()
            val field = linker.dereference(context, value.name())
            if (field == null) {
                linker.addError("unable to resolve option %s on %s", value.name(), context.type())
            } else {
                val protoMember = ProtoMember.get(context.type(), field)
                result.put(protoMember, canonicalizeValue(linker, field, value.value()))
            }
            return coerceValueForField(context, result.build())
        }

        if (value is Map<*, *>) {
            val result = ImmutableMap.builder<ProtoMember, Any>()
            for ((key, value1) in value) {
                val name = key as String
                val field = linker.dereference(context, name)
                if (field == null) {
                    linker.addError("unable to resolve option %s on %s", name, context.type())
                } else {
                    val protoMember = ProtoMember.get(context.type(), field)
                    result.put(protoMember, canonicalizeValue(linker, field, value1))
                }
            }
            return coerceValueForField(context, result.build())
        }

        if (value is List<*>) {
            val result = List.builder<Any>()
            for (element in value) {
                result.addAll(canonicalizeValue(linker, context, element) as List<*>)
            }
            return coerceValueForField(context, result.build())
        }

        if (value is String) {
            return coerceValueForField(context, value)
        }

        throw IllegalArgumentException("Unexpected option value: $value")
    }

    private fun coerceValueForField(context: Field, value: Any): Any {
        return if (context.isRepeated) {
            value as? List<*> ?: listOf(value)
        } else {
            if (value is List<*>) getOnlyElement<Any>(value) else value
        }
    }

    /** Combine values for the same key, resolving conflicts based on their type.  */
    private fun union(linker: Linker, a: Any, b: Any): Any {
        if (a is List<*>) {
            return union(a, b as List<*>)
        } else if (a is Map<*, *>) {
            return union(linker, a as Map<ProtoMember, Any>, b as Map<ProtoMember, Any>)
        } else {
            linker.addError("conflicting options: %s, %s", a, b)
            return a // Just return any placeholder.
        }
    }

    private fun union(
            linker: Linker, a: Map<ProtoMember, Any>, b: Map<ProtoMember, Any>): ImmutableMap<ProtoMember, Any> {
        val result = LinkedHashMap(a)
        for ((key, bValue) in b) {
            val aValue = result[key]
            val union = if (aValue != null) union(linker, aValue, bValue) else bValue
            result[key] = union
        }
        return ImmutableMap.copyOf(result)
    }

    private fun union(a: List<*>, b: List<*>): List<Any> {
        return List.builder<Any>().addAll(a).addAll(b).build()
    }

    internal fun fields(): Multimap<ProtoType, ProtoMember> {
        val result = LinkedHashMultimap.create<ProtoType, ProtoMember>()
        gatherFields(result, optionType, map)
        return result
    }

    private fun gatherFields(sink: Multimap<ProtoType, ProtoMember>, type: ProtoType, o: Any?) {
        if (o is Map<*, *>) {
            for ((key, value) in o) {
                val protoMember = key as ProtoMember
                sink.put(type, protoMember)
                gatherFields(sink, protoMember.type(), value)
            }
        } else if (o is List<*>) {
            for (e in (o as List<*>?)!!) {
                gatherFields(sink, type, e)
            }
        }
    }

    internal fun retainAll(schema: Schema, markSet: MarkSet): Options {
        if (map!!.isEmpty()) return this // Nothing to prune.
        val result = Options(optionType, optionElements)
        val mapOrNull = retainAll(schema, markSet, optionType, map)
        result.map = if (mapOrNull != null)
            mapOrNull as ImmutableMap<ProtoMember, Any>?
        else
            ImmutableMap.of()
        return result
    }

    /** Returns an object of the same type as `o`, or null if it is not retained.  */
    private fun retainAll(schema: Schema, markSet: MarkSet, type: ProtoType?, o: Any): Any? {
        if (!markSet.contains(type)) {
            return null // Prune this type.

        } else if (o is Map<*, *>) {
            val builder = ImmutableMap.builder<ProtoMember, Any>()
            for ((key, value) in o) {
                val protoMember = key as ProtoMember
                if (!markSet.contains(protoMember)) continue // Prune this field.
                val field = schema.getField(protoMember)
                val retainedValue = retainAll(schema, markSet, field!!.type(), value)
                if (retainedValue != null) {
                    builder.put(protoMember, retainedValue) // This retained field is non-empty.
                }
            }
            val map = builder.build()
            return if (!map.isEmpty()) map else null

        } else if (o is List<*>) {
            val builder = List.builder<Any>()
            for (value in o) {
                val retainedValue = retainAll(schema, markSet, type, value)
                if (retainedValue != null) {
                    builder.add(retainedValue) // This retained value is non-empty.
                }
            }
            val list = builder.build()
            return if (!list.isEmpty()) list else null

        } else {
            return o
        }
    }

    companion object {
        val FILE_OPTIONS = ProtoType.get("google.protobuf.FileOptions")
        val MESSAGE_OPTIONS = ProtoType.get("google.protobuf.MessageOptions")
        val FIELD_OPTIONS = ProtoType.get("google.protobuf.FieldOptions")
        val ENUM_OPTIONS = ProtoType.get("google.protobuf.EnumOptions")
        val ENUM_VALUE_OPTIONS = ProtoType.get("google.protobuf.EnumValueOptions")
        val SERVICE_OPTIONS = ProtoType.get("google.protobuf.ServiceOptions")
        val METHOD_OPTIONS = ProtoType.get("google.protobuf.MethodOptions")

        /**
         * Given a path like `a.b.c.d` and a set of paths like `{a.b.c, a.f.g, h.j}`, this
         * returns the original path split on dots such that the first element is in the set. For the
         * above example it would return the array `[a.b.c, d]`.
         *
         *
         * Typically the input path is a package name like `a.b`, followed by a dot and a
         * sequence of field names. The first field name is an extension field; subsequent field names
         * make a path within that extension.
         *
         *
         * Note that a single input may yield multiple possible answers, such as when package names
         * and field names collide. This method prefers shorter package names though that is an
         * implementation detail.
         */
        internal fun resolveFieldPath(name: String, fullyQualifiedNames: Set<String>): Array<String>? {
            // Try to resolve a local name.
            var i = 0
            while (i < name.length) {
                i = name.indexOf('.', i)
                if (i == -1) i = name.length

                val candidate = name.substring(0, i)
                if (fullyQualifiedNames.contains(candidate)) {
                    val path = name.substring(i).split("\\.".toRegex()).toTypedArray()
                    path[0] = name.substring(0, i)
                    return path
                }
                i++
            }

            return null
        }
    }
}
