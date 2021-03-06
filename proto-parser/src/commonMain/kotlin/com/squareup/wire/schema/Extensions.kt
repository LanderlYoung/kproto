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

import com.squareup.wire.schema.internal.Util
import com.squareup.wire.schema.internal.parser.ExtensionsElement

internal class Extensions private constructor(
        val location: Location,
        val documentation: String,
        val start: Int,
        val end: Int) {

    fun validate(linker: Linker) {
        if (!Util.isValidTag(start) || !Util.isValidTag(end)) {
            linker.withContext(this).addError("tags are out of range: %s to %s", start, end)
        }
    }

    companion object {

        fun fromElements(elements: List<ExtensionsElement>): List<Extensions> =
                elements.map { element ->
                    Extensions(element.location, element.documentation,
                            element.start, element.end)
                }

        fun toElements(extensions: List<Extensions>): List<ExtensionsElement> =
                extensions.map { extension ->
                    ExtensionsElement(
                            location = extension.location,
                            start = extension.start,
                            end = extension.end,
                            documentation = extension.documentation)

                }
    }
}
