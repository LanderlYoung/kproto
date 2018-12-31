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
package com.squareup.wire.schema.internal.parser

import com.squareup.wire.schema.Field
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.Util.appendDocumentation
import com.squareup.wire.schema.internal.Util.appendIndented

data class GroupElement(
        val location: Location,
        val label: Field.Label?,
        val name: String,
        val tag: Int,
        val documentation: String = "",
        val fields: List<FieldElement> = listOf()
) {
    fun toSchema(): String {
        val builder = StringBuilder()
        appendDocumentation(builder, documentation)
        if (label != null) {
            builder.append(label.name.toLowerCase()).append(' ')
        }
        builder.append("group ")
                .append(name)
                .append(" = ")
                .append(tag)
                .append(" {")
        if (!fields.isEmpty()) {
            builder.append('\n')
            for (field in fields) {
                appendIndented(builder, field.toSchema())
            }
        }
        return builder.append("}\n").toString()
    }
}
