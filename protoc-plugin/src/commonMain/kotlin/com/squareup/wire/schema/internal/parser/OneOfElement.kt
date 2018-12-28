/*
 * Copyright (C) 2014 Square, Inc.
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
package com.squareup.wire.schema.internal.parser

import com.squareup.wire.schema.internal.Util.appendDocumentation
import com.squareup.wire.schema.internal.Util.appendIndented

data class OneOfElement(
        val name: String,
        val documentation: String = "",
        val fields: List<FieldElement> = listOf(),
        val groups: List<GroupElement> = listOf()
) {
    fun toSchema(): String {
        val builder = StringBuilder()
        appendDocumentation(builder, documentation)
        builder.append("oneof ").append(name).append(" {")
        if (!fields.isEmpty()) {
            builder.append('\n')
            for (field in fields) {
                appendIndented(builder, field.toSchema())
            }
        }
        if (!groups.isEmpty()) {
            builder.append('\n')
            for (group in groups) {
                appendIndented(builder, group.toSchema())
            }
        }
        return builder.append("}\n").toString()
    }
}
