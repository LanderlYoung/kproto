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

import com.squareup.wire.schema.internal.parser.ExtendElement

class Extend private constructor(
        val location: Location,
        val documentation: String,
        val name: String,
        val fields: List<Field>) {
    var protoType: ProtoType? = null
        private set


    internal fun link(linker: Linker) {
        var linker = linker
        linker = linker.withContext(this)
        protoType = linker.resolveMessageType(name)
        val type = linker[protoType!!]
        (type as MessageType).addExtensionFields(fields)
    }

    internal fun validate(linker: Linker) {
        var linker = linker
        linker = linker.withContext(this)
        linker.validateImport(location, protoType!!)
    }

    companion object {

        fun fromElements(packageName: String?,
                         extendElements: List<ExtendElement>): List<Extend> {
            val extendBuilder = mutableListOf<Extend>()
            for (extendElement in extendElements) {
                extendBuilder.add(Extend(extendElement.location, extendElement.documentation,
                        extendElement.name, Field.fromElements(packageName, extendElement.fields, true)))
            }
            return extendBuilder
        }

        fun toElements(extendList: List<Extend>): List<ExtendElement> {
            val elements = mutableListOf<ExtendElement>()
            for (extend in extendList) {
                elements.add(ExtendElement(
                        location = extend.location,
                        documentation = extend.documentation,
                        name = extend.name,
                        fields = Field.toElements(extend.fields)
                ))
            }
            return elements
        }
    }
}
