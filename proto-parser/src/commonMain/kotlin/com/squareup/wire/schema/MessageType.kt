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

import com.squareup.wire.schema.internal.parser.MessageElement

class MessageType private constructor(
        override val type: ProtoType,
        override val location: Location,
        override val documentation: String,
        val name: String,
        val declaredFields: List<Field>,
        val extensionFields: MutableList<Field>,
        val oneOfs: List<OneOf>,
        override val nestedTypes: List<Type>,
        private val extensionsList: List<Extensions>,
        private val reserveds: List<Reserved>,
        override val options: Options) : Type() {


    val requiredFields: List<Field>
        get() = fieldsAndOneOfFields().filter { it.isRequired }

    val fields: List<Field>
        get() = declaredFields + extensionFields

    fun fieldsAndOneOfFields(): List<Field> {
        val result = mutableListOf<Field>()
        result.addAll(declaredFields)
        result.addAll(extensionFields)
        for (oneOf in oneOfs) {
            result.addAll(oneOf.fields)
        }
        return result
    }

    /** Returns the field named `name`, or null if this type has no such field.  */
    fun field(name: String): Field? {
        for (field in declaredFields) {
            if (field.name == name) {
                return field
            }
        }
        for (oneOf in oneOfs) {
            for (field in oneOf.fields) {
                if (field.name == name) {
                    return field
                }
            }
        }
        return null
    }

    /**
     * Returns the field with the qualified name `qualifiedName`, or null if this type has no
     * such field.
     */
    fun extensionField(qualifiedName: String): Field? {
        for (field in extensionFields) {
            if (field.qualifiedName == qualifiedName) {
                return field
            }
        }
        return null
    }

    /** Returns the field tagged `tag`, or null if this type has no such field.  */
    fun field(tag: Int): Field? {
        for (field in declaredFields) {
            if (field.tag == tag) {
                return field
            }
        }
        for (field in extensionFields) {
            if (field.tag == tag) {
                return field
            }
        }
        return null
    }

    fun oneOfs(): List<OneOf> {
        return oneOfs
    }

    internal fun extensionFieldsMap(): Map<String, Field> {
        // TODO(jwilson): simplify this to just resolve field values directly.
        val extensionsForType = LinkedHashMap<String, Field>()
        for (field in extensionFields) {
            extensionsForType[field.qualifiedName] = field
        }
        return extensionsForType
    }

    internal fun addExtensionFields(fields: List<Field>) {
        extensionFields.addAll(fields)
    }

    override fun link(linker: Linker) {
        var linker = linker
        linker = linker.withContext(this)
        for (field in declaredFields) {
            field.link(linker)
        }
        for (field in extensionFields) {
            field.link(linker)
        }
        for (oneOf in oneOfs) {
            oneOf.link(linker)
        }
        for (type in nestedTypes) {
            type.link(linker)
        }
    }

    override fun linkOptions(linker: Linker) {
        var linker = linker
        linker = linker.withContext(this)
        for (type in nestedTypes) {
            type.linkOptions(linker)
        }
        for (field in declaredFields) {
            field.linkOptions(linker)
        }
        for (field in extensionFields) {
            field.linkOptions(linker)
        }
        for (oneOf in oneOfs) {
            oneOf.linkOptions(linker)
        }
        options.link(linker)
    }

    override fun validate(linker: Linker) {
        var linker = linker
        linker = linker.withContext(this)
        linker.validateFields(fieldsAndOneOfFields(), reserveds)
        linker.validateEnumConstantNameUniqueness(nestedTypes)
        for (field in fieldsAndOneOfFields()) {
            field.validate(linker)
        }
        for (type in nestedTypes) {
            type.validate(linker)
        }
        for (extensions in extensionsList) {
            extensions.validate(linker)
        }
    }

    override fun retainAll(schema: Schema, markSet: MarkSet): Type? {
        val retainedNestedTypes = mutableListOf<Type>()
        for (nestedType in nestedTypes) {
            val retainedNestedType = nestedType.retainAll(schema, markSet)
            if (retainedNestedType != null) {
                retainedNestedTypes.add(retainedNestedType)
            }
        }

        if (!markSet.contains(type)) {
            // If this type is not retained, and none of its nested types are retained, prune it.
            return if (retainedNestedTypes.isEmpty()) {
                null
            } else EnclosingType(location, type, documentation, retainedNestedTypes)
            // If this type is not retained but retained nested types, replace it with an enclosing type.
        }

        val retainedOneOfs = mutableListOf<OneOf>()
        for (oneOf in oneOfs) {
            val retainedOneOf = oneOf.retainAll(schema, markSet, type)
            if (retainedOneOf != null) {
                retainedOneOfs.add(retainedOneOf)
            }
        }

        return MessageType(
                type,
                location,
                documentation,
                name,
                Field.retainAll(schema, markSet, type, declaredFields),
                Field.retainAll(schema, markSet, type, extensionFields).toMutableList(),
                retainedOneOfs,
                retainedNestedTypes,
                extensionsList,
                reserveds,
                options.retainAll(schema, markSet))
    }

    internal fun toElement(): MessageElement {
        return MessageElement(
                location = location,
                documentation = documentation,
                name = name,
                options = options.toElements(),
                fields = Field.toElements(declaredFields),
                nestedTypes = Type.toElements(nestedTypes),
                oneOfs = OneOf.toElements(oneOfs),
                extensions = Extensions.toElements(extensionsList),
                reserveds = Reserved.toElements(reserveds))
    }

    companion object {

        internal fun fromElement(packageName: String?, protoType: ProtoType,
                                 messageElement: MessageElement): MessageType {
            if (!messageElement.groups.isEmpty()) {
                val group = messageElement.groups[0]
                throw IllegalStateException(group.location.toString() + ": 'group' is not supported")
            }

            val declaredFields = Field.fromElements(packageName, messageElement.fields, false)

            // Extension fields be populated during linking.
            val extensionFields = ArrayList<Field>()

            val oneOfs = OneOf.fromElements(packageName, messageElement.oneOfs, false)

            val nestedTypes = mutableListOf<Type>()
            for (nestedType in messageElement.nestedTypes) {
                nestedTypes.add(Type[packageName, protoType.nestedType(nestedType.name), nestedType])
            }

            val extensionsList = Extensions.fromElements(messageElement.extensions)

            val reserveds = Reserved.fromElements(messageElement.reserveds)

            val options = Options(Options.MESSAGE_OPTIONS, messageElement.options)

            return MessageType(protoType, messageElement.location, messageElement.documentation,
                    messageElement.name, declaredFields, extensionFields, oneOfs, nestedTypes,
                    extensionsList, reserveds, options)
        }
    }
}
