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
import com.squareup.wire.schema.internal.parser.GroupElement
import com.squareup.wire.schema.internal.parser.MessageElement
import com.squareup.wire.schema.internal.parser.TypeElement
import java.util.ArrayList
import java.util.LinkedHashMap

import com.google.common.base.Preconditions.checkNotNull

class MessageType private constructor(private val protoType: ProtoType, private val location: Location, private val documentation: String, private val name: String,
                                      private val declaredFields: List<Field>, private val extensionFields: MutableList<Field>, private val oneOfs: List<OneOf>,
                                      private val nestedTypes: List<Type>, private val extensionsList: List<Extensions>,
                                      private val reserveds: List<Reserved>, options: Options) : Type() {
    private val options: Options

    val requiredFields: List<Field>
        get() {
            val required = List.builder<Field>()
            for (field in fieldsAndOneOfFields()) {
                if (field.isRequired) {
                    required.add(field)
                }
            }
            return required.build()
        }

    init {
        this.options = checkNotNull(options)
    }

    override fun location(): Location {
        return location
    }

    override fun type(): ProtoType {
        return protoType
    }

    override fun documentation(): String {
        return documentation
    }

    override fun nestedTypes(): List<Type> {
        return nestedTypes
    }

    override fun options(): Options {
        return options
    }

    fun fields(): List<Field> {
        return List.builder<Field>()
                .addAll(declaredFields)
                .addAll(extensionFields)
                .build()
    }

    fun extensionFields(): List<Field> {
        return List.copyOf(extensionFields)
    }

    fun fieldsAndOneOfFields(): List<Field> {
        val result = List.builder<Field>()
        result.addAll(declaredFields)
        result.addAll(extensionFields)
        for (oneOf in oneOfs) {
            result.addAll(oneOf.fields())
        }
        return result.build()
    }

    /** Returns the field named `name`, or null if this type has no such field.  */
    fun field(name: String): Field? {
        for (field in declaredFields) {
            if (field.name() == name) {
                return field
            }
        }
        for (oneOf in oneOfs) {
            for (field in oneOf.fields()) {
                if (field.name() == name) {
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
            if (field.qualifiedName() == qualifiedName) {
                return field
            }
        }
        return null
    }

    /** Returns the field tagged `tag`, or null if this type has no such field.  */
    fun field(tag: Int): Field? {
        for (field in declaredFields) {
            if (field.tag() == tag) {
                return field
            }
        }
        for (field in extensionFields) {
            if (field.tag() == tag) {
                return field
            }
        }
        return null
    }

    fun oneOfs(): List<OneOf> {
        return oneOfs
    }

    fun extensions(): List<Extensions> {
        return extensionsList
    }

    internal fun extensionFieldsMap(): Map<String, Field> {
        // TODO(jwilson): simplify this to just resolve field values directly.
        val extensionsForType = LinkedHashMap<String, Field>()
        for (field in extensionFields) {
            extensionsForType[field.qualifiedName()] = field
        }
        return extensionsForType
    }

    internal fun addExtensionFields(fields: List<Field>) {
        extensionFields.addAll(fields)
    }

    internal override fun link(linker: Linker) {
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

    internal override fun linkOptions(linker: Linker) {
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

    internal override fun validate(linker: Linker) {
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

    internal override fun retainAll(schema: Schema, markSet: MarkSet): Type? {
        val retainedNestedTypesBuilder = List.builder<Type>()
        for (nestedType in nestedTypes) {
            val retainedNestedType = nestedType.retainAll(schema, markSet)
            if (retainedNestedType != null) {
                retainedNestedTypesBuilder.add(retainedNestedType)
            }
        }

        val retainedNestedTypes = retainedNestedTypesBuilder.build()
        if (!markSet.contains(protoType)) {
            // If this type is not retained, and none of its nested types are retained, prune it.
            return if (retainedNestedTypes.isEmpty()) {
                null
            } else EnclosingType(location, protoType, documentation, retainedNestedTypes)
            // If this type is not retained but retained nested types, replace it with an enclosing type.
        }

        val retainedOneOfsBuilder = List.builder<OneOf>()
        for (oneOf in oneOfs) {
            val retainedOneOf = oneOf.retainAll(schema, markSet, protoType)
            if (retainedOneOf != null) {
                retainedOneOfsBuilder.add(retainedOneOf)
            }
        }
        val retainedOneOfs = retainedOneOfsBuilder.build()

        return MessageType(protoType, location, documentation, name,
                Field.retainAll(schema, markSet, protoType, declaredFields),
                Field.retainAll(schema, markSet, protoType, extensionFields), retainedOneOfs,
                retainedNestedTypes, extensionsList, reserveds, options.retainAll(schema, markSet))
    }

    internal fun toElement(): MessageElement {
        return MessageElement.builder(location)
                .documentation(documentation)
                .name(name)
                .options(options.toElements())
                .fields(Field.toElements(declaredFields))
                .nestedTypes(Type.toElements(nestedTypes))
                .oneOfs(OneOf.toElements(oneOfs))
                .extensions(Extensions.toElements(extensionsList))
                .reserveds(Reserved.toElements(reserveds))
                .build()
    }

    companion object {

        internal fun fromElement(packageName: String, protoType: ProtoType,
                                 messageElement: MessageElement): MessageType {
            if (!messageElement.groups().isEmpty()) {
                val group = messageElement.groups()[0]
                throw IllegalStateException(group.location().toString() + ": 'group' is not supported")
            }

            val declaredFields = Field.fromElements(packageName, messageElement.fields(), false)

            // Extension fields be populated during linking.
            val extensionFields = ArrayList<Field>()

            val oneOfs = OneOf.fromElements(packageName, messageElement.oneOfs(), false)

            val nestedTypes = List.builder<Type>()
            for (nestedType in messageElement.nestedTypes()) {
                nestedTypes.add(Type.get(packageName, protoType.nestedType(nestedType.name()), nestedType))
            }

            val extensionsList = Extensions.fromElements(messageElement.extensions())

            val reserveds = Reserved.fromElements(messageElement.reserveds())

            val options = Options(Options.MESSAGE_OPTIONS, messageElement.options())

            return MessageType(protoType, messageElement.location(), messageElement.documentation(),
                    messageElement.name(), declaredFields, extensionFields, oneOfs, nestedTypes.build(),
                    extensionsList, reserveds, options)
        }
    }
}
