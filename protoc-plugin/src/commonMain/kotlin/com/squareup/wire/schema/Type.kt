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
import com.squareup.wire.schema.internal.parser.EnumElement
import com.squareup.wire.schema.internal.parser.MessageElement
import com.squareup.wire.schema.internal.parser.TypeElement

abstract class Type {
    abstract fun location(): Location
    abstract fun type(): ProtoType
    abstract fun documentation(): String
    abstract fun options(): Options
    abstract fun nestedTypes(): List<Type>
    internal abstract fun link(linker: Linker)
    internal abstract fun linkOptions(linker: Linker)
    internal abstract fun validate(linker: Linker)
    internal abstract fun retainAll(schema: Schema, markSet: MarkSet): Type

    companion object {

        operator fun get(packageName: String, protoType: ProtoType, type: TypeElement): Type {
            return if (type is EnumElement) {
                EnumType.fromElement(protoType, type)

            } else if (type is MessageElement) {
                MessageType.fromElement(packageName, protoType, type)

            } else {
                throw IllegalArgumentException("unexpected type: " + type.javaClass)
            }
        }

        internal fun fromElements(packageName: String, elements: List<TypeElement>): List<Type> {
            val types = List.Builder<Type>()
            for (element in elements) {
                val protoType = ProtoType.get(packageName, element.name())
                types.add(Type[packageName, protoType, element])
            }
            return types.build()
        }

        internal fun toElement(type: Type): TypeElement {
            return if (type is EnumType) {
                type.toElement()

            } else if (type is MessageType) {
                type.toElement()

            } else if (type is EnclosingType) {
                type.toElement()

            } else {
                throw IllegalArgumentException("unexpected type: " + type.javaClass)
            }
        }

        internal fun toElements(types: List<Type>): List<TypeElement> {
            val elements = List.Builder<TypeElement>()
            for (type in types) {
                elements.add(Type.toElement(type))
            }
            return elements.build()
        }
    }
}
