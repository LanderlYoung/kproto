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

import com.squareup.wire.schema.internal.parser.EnumElement
import com.squareup.wire.schema.internal.parser.MessageElement
import com.squareup.wire.schema.internal.parser.TypeElement

abstract class Type {
    abstract val location: Location
    abstract val type: ProtoType
    abstract val documentation: String
    abstract val options: Options
    abstract val nestedTypes: List<Type>
    internal abstract fun link(linker: Linker)
    internal abstract fun linkOptions(linker: Linker)
    internal abstract fun validate(linker: Linker)
    internal abstract fun retainAll(schema: Schema, markSet: MarkSet): Type?

    companion object {

        operator fun get(packageName: String?, protoType: ProtoType, type: TypeElement): Type {
            return when (type) {
                is EnumElement -> EnumType.fromElement(protoType, type)
                is MessageElement -> MessageType.fromElement(packageName, protoType, type)
                else -> throw IllegalArgumentException("unexpected type: $type")
            }
        }

        internal fun fromElements(packageName: String?, elements: List<TypeElement>): List<Type> {
            val types = mutableListOf<Type>()
            for (element in elements) {
                val protoType = ProtoType[packageName, element.name]
                types.add(Type[packageName, protoType, element])
            }
            return types
        }

        internal fun toElement(type: Type): TypeElement {
            return when (type) {
                is EnumType -> type.toElement()
                is MessageType -> type.toElement()
                is EnclosingType -> type.toElement()
                else -> throw IllegalArgumentException("unexpected type: $type")
            }
        }

        internal fun toElements(types: List<Type>): List<TypeElement> {
            val elements = mutableListOf<TypeElement>()
            for (type in types) {
                elements.add(Type.toElement(type))
            }
            return elements
        }
    }
}
