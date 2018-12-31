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

import com.squareup.wire.schema.internal.parser.EnumConstantElement

class EnumConstant private constructor(
        val location: Location,
        val name: String,
        val tag: Int,
        val documentation: String,
        val options: Options) {

    internal fun toElement(): EnumConstantElement {
        return EnumConstantElement(
                location = location,
                documentation = documentation,
                name = name,
                tag = tag,
                options = options.toElements())
    }

    internal fun linkOptions(linker: Linker) {
        options.link(linker)
    }

    internal fun retainAll(schema: Schema, markSet: MarkSet): EnumConstant {
        return EnumConstant(location, name, tag, documentation, options.retainAll(schema, markSet))
    }

    companion object {

        internal fun fromElement(element: EnumConstantElement): EnumConstant {
            return EnumConstant(element.location, element.name, element.tag,
                    element.documentation, Options(Options.ENUM_VALUE_OPTIONS, element.options))
        }

        internal fun fromElements(elements: List<EnumConstantElement>): List<EnumConstant> {
            val constants = mutableListOf<EnumConstant>()
            for (element in elements) {
                constants.add(fromElement(element))
            }
            return constants
        }

        internal fun toElements(constants: List<EnumConstant>): List<EnumConstantElement> {
            val elements = mutableListOf<EnumConstantElement>()
            for (constant in constants) {
                elements.add(constant.toElement())
            }
            return elements
        }
    }
}
