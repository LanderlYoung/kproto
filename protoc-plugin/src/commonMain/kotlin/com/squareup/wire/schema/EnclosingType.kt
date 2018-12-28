/*
 * Copyright (C) 2016 Square, Inc.
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
import com.squareup.wire.schema.internal.parser.MessageElement

/** An empty type which only holds nested types.  */
class EnclosingType internal constructor(private val location: Location, private val type: ProtoType, private val documentation: String,
                                         private val nestedTypes: List<Type>) : Type() {

    override fun location(): Location {
        return location
    }

    override fun type(): ProtoType {
        return type
    }

    override fun documentation(): String {
        return documentation
    }

    override fun options(): Options {
        throw UnsupportedOperationException()
    }

    override fun nestedTypes(): List<Type> {
        return nestedTypes
    }

    internal override fun link(linker: Linker) {
        for (nestedType in nestedTypes) {
            nestedType.link(linker)
        }
    }

    internal override fun linkOptions(linker: Linker) {
        for (nestedType in nestedTypes) {
            nestedType.linkOptions(linker)
        }
    }

    internal override fun validate(linker: Linker) {
        for (nestedType in nestedTypes) {
            nestedType.validate(linker)
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
        return if (retainedNestedTypes.isEmpty()) {
            null
        } else EnclosingType(location, type, documentation, retainedNestedTypes)
    }

    internal fun toElement(): MessageElement {
        return MessageElement.builder(location)
                .name(type.simpleName())
                .nestedTypes(Type.toElements(nestedTypes))
                .build()
    }
}
