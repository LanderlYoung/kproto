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

import com.squareup.wire.schema.internal.parser.OneOfElement

class OneOf private constructor(
        val name: String,
        val documentation: String,
        val fields: List<Field>) {

    internal fun link(linker: Linker) {
        fields.forEach { it.link(linker) }
    }

    internal fun linkOptions(linker: Linker) {
        fields.forEach { it.linkOptions(linker) }
    }

    internal fun retainAll(schema: Schema, markSet: MarkSet, enclosingType: ProtoType): OneOf? {
        val retainedFields = Field.retainAll(schema, markSet, enclosingType, fields)
        return if (retainedFields.isEmpty()) null else OneOf(name, documentation, retainedFields)
    }

    companion object {

        internal fun fromElements(packageName: String?,
                                  elements: List<OneOfElement>, extension: Boolean): List<OneOf> {
            val oneOfs = mutableListOf<OneOf>()
            for (oneOf in elements) {
                if (!oneOf.groups.isEmpty()) {
                    val group = oneOf.groups[0]
                    throw IllegalStateException(group.location.toString() + ": 'group' is not supported")
                }
                oneOfs.add(OneOf(oneOf.name, oneOf.documentation,
                        Field.fromElements(packageName, oneOf.fields, extension)))
            }
            return oneOfs
        }

        internal fun toElements(oneOfs: List<OneOf>): List<OneOfElement> {
            val elements = mutableListOf<OneOfElement>()
            for (oneOf in oneOfs) {
                elements.add(OneOfElement(
                        documentation=oneOf.documentation,
                        name=oneOf.name,
                        fields=Field.toElements(oneOf.fields)))
            }
            return elements
        }
    }
}
