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

import com.squareup.wire.schema.Options.Companion.FIELD_OPTIONS
import com.squareup.wire.schema.internal.parser.FieldElement

class Field private constructor(
        val packageName: String?,
        val location: Location,
        val label: Label?,
        val name: String,

        private val documentation: String,
        val tag: Int,
        val default: String?,
        private val elementType: String,
        val options: Options,
        val isExtension: Boolean) {

    lateinit var type: ProtoType
        private set
    var deprecated: Any? = null
        private set
    var packed: Any? = null
        private set
    var isRedacted: Boolean = false
        private set

    val isRepeated: Boolean
        get() = label == Label.REPEATED

    val isOptional: Boolean
        get() = label == Label.OPTIONAL

    val isRequired: Boolean
        get() = label == Label.REQUIRED

    val isDeprecated: Boolean
        get() = "true" == deprecated

    val isPacked: Boolean
        get() = "true" == packed

    /**
     * Returns this field's name, prefixed with its package name. Uniquely identifies extension
     * fields, such as in options.
     */
    val qualifiedName: String =
            if (packageName != null)
                packageName + '.'.toString() + name
            else
                name

    private fun isPackable(linker: Linker, type: ProtoType): Boolean {
        return (type != ProtoType.STRING
                && type != ProtoType.BYTES
                && linker.get(type) !is MessageType)
    }

    internal fun link(linker: Linker) {
        var linker = linker
        linker = linker.withContext(this)
        type = linker.resolveType(elementType)
    }

    internal fun linkOptions(linker: Linker) {
        var linker = linker
        linker = linker.withContext(this)
        options.link(linker)
        deprecated = options[DEPRECATED]
        packed = options[PACKED]
        // We allow any package name to be used as long as it ends with '.redacted'.
        isRedacted = options.optionMatches(".*\\.redacted", "true")
    }

    internal fun validate(linker: Linker) {
        var linker = linker
        linker = linker.withContext(this)
        if (isPacked && !isPackable(linker, type!!)) {
            linker.addError("packed=true not permitted on %s", type!!)
        }
        if (isExtension && isRequired) {
            linker.addError("extension fields cannot be required", type!!)
        }
        linker.validateImport(location, type!!)
    }

    internal fun retainAll(schema: Schema, markSet: MarkSet): Field? {
        // For map types only the value can participate in pruning as the key will always be scalar.
        if (type!!.isMap && !markSet.contains(type!!.valueType())) return null

        if (!markSet.contains(type)) return null

        val result = Field(packageName, location, label, name, documentation, tag, default,
                elementType, options.retainAll(schema, markSet), isExtension)
        result.type = type
        result.deprecated = deprecated
        result.packed = packed
        result.isRedacted = isRedacted
        return result
    }

    override fun toString(): String {
        return name
    }

    enum class Label {
        OPTIONAL, REQUIRED, REPEATED,
        /** Indicates the field is a member of a `oneof` block.  */
        ONE_OF
    }

    companion object {
        internal val DEPRECATED = ProtoMember.get(FIELD_OPTIONS, "deprecated")
        internal val PACKED = ProtoMember.get(FIELD_OPTIONS, "packed")

        internal fun fromElements(packageName: String?,
                                  fieldElements: List<FieldElement>, extension: Boolean): List<Field> {
            val fields = mutableListOf<Field>()
            for (field in fieldElements) {
                fields.add(Field(packageName, field.location, field.label, field.name,
                        field.documentation, field.tag, field.defaultValue, field.type,
                        Options(Options.FIELD_OPTIONS, field.options), extension))
            }
            return fields
        }

        internal fun toElements(fields: List<Field>): List<FieldElement> {
            val elements = mutableListOf<FieldElement>()
            for (field in fields) {
                elements.add(FieldElement(
                        location = field.location,
                        label = field.label,
                        name = field.name,
                        documentation = field.documentation,
                        tag = field.tag,
                        defaultValue = field.default,
                        options = field.options.toElements(),
                        type = field.elementType))
            }
            return elements
        }

        internal fun retainAll(
                schema: Schema, markSet: MarkSet, enclosingType: ProtoType, fields: Collection<Field>): List<Field> {
            val result = mutableListOf<Field>()
            for (field in fields) {
                val retainedField = field.retainAll(schema, markSet)
                if (retainedField != null && markSet.contains(ProtoMember.get(enclosingType, field.name))) {
                    result.add(retainedField)
                }
            }
            return result
        }
    }
}
