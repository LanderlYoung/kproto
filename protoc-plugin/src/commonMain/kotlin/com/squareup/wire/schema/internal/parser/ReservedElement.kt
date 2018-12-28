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

import com.squareup.wire.schema.Location

import com.squareup.wire.schema.internal.Util.appendDocumentation


data class ReservedElement(
        val location: Location,
        val documentation: String,
        /** A [String] name or [Integer] or [Range&amp;lt;Integer&gt;][Range] tag.  */
        val values: List<Any>
) {

    fun toSchema(): String {
        val builder = StringBuilder()
        appendDocumentation(builder, documentation)
        builder.append("reserved ")
        val value = values
        for (i in value.indices) {
            if (i > 0) builder.append(", ")

            val reservation = value[i]
            if (reservation is String) {
                builder.append('"').append(reservation).append('"')
            } else if (reservation is Int) {
                builder.append(reservation)
            } else if (reservation is Range<*>) {
                val range = reservation as Range<Int>
                builder.append(range.lowerEndpoint()).append(" to ").append(range.upperEndpoint())
            } else {
                throw AssertionError()
            }
        }
        return builder.append(";\n").toString()
    }
}
