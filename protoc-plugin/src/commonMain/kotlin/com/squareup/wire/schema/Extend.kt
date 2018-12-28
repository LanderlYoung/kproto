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
import com.squareup.wire.schema.internal.parser.ExtendElement

internal class Extend private constructor(private val location: Location, private val documentation: String, private val name: String,
                                          private val fields: List<Field>) {
    private var protoType: ProtoType? = null

    fun location(): Location {
        return location
    }

    fun type(): ProtoType? {
        return protoType
    }

    fun documentation(): String {
        return documentation
    }

    fun fields(): List<Field> {
        return fields
    }

    fun link(linker: Linker) {
        var linker = linker
        linker = linker.withContext(this)
        protoType = linker.resolveMessageType(name)
        val type = linker.get(protoType!!)
        if (type != null) {
            (type as MessageType).addExtensionFields(fields)
        }
    }

    fun validate(linker: Linker) {
        var linker = linker
        linker = linker.withContext(this)
        linker.validateImport(location(), type()!!)
    }

    companion object {

        fun fromElements(packageName: String,
                         extendElements: List<ExtendElement>): List<Extend> {
            val extendBuilder = List.Builder<Extend>()
            for (extendElement in extendElements) {
                extendBuilder.add(Extend(extendElement.location(), extendElement.documentation(),
                        extendElement.name(), Field.fromElements(packageName, extendElement.fields(), true)))
            }
            return extendBuilder.build()
        }

        fun toElements(extendList: List<Extend>): List<ExtendElement> {
            val elements = List.Builder<ExtendElement>()
            for (extend in extendList) {
                elements.add(ExtendElement.builder(extend.location)
                        .documentation(extend.documentation)
                        .name(extend.name)
                        .fields(Field.toElements(extend.fields))
                        .build())
            }
            return elements.build()
        }
    }
}
