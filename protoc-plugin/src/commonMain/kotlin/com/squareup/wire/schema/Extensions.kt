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
import com.squareup.wire.schema.internal.Util
import com.squareup.wire.schema.internal.parser.ExtensionsElement

internal class Extensions private constructor(private val location: Location, private val documentation: String, private val start: Int, private val end: Int) {

    fun location(): Location {
        return location
    }

    fun documentation(): String {
        return documentation
    }

    fun start(): Int {
        return start
    }

    fun end(): Int {
        return end
    }

    fun validate(linker: Linker) {
        if (!Util.isValidTag(start()) || !Util.isValidTag(end())) {
            linker.withContext(this).addError("tags are out of range: %s to %s", start(), end())
        }
    }

    companion object {

        fun fromElements(elements: List<ExtensionsElement>): List<Extensions> {
            val extensions = List.builder<Extensions>()
            for (element in elements) {
                extensions.add(Extensions(element.location(), element.documentation(),
                        element.start(), element.end()))
            }
            return extensions.build()
        }

        fun toElements(extensions: List<Extensions>): List<ExtensionsElement> {
            val elements = List.Builder<ExtensionsElement>()
            for (extension in extensions) {
                elements.add(ExtensionsElement.create(extension.location, extension.start, extension.end,
                        extension.documentation))
            }
            return elements.build()
        }
    }
}
