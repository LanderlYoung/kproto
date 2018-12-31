/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.wire.schema.internal

import com.squareup.wire.schema.internal.parser.OptionElement

object Util {
    const val MIN_TAG_VALUE = 1
    const val MAX_TAG_VALUE = (1 shl 29) - 1 // 536,870,911
    private const val RESERVED_TAG_VALUE_START = 19000
    private const val RESERVED_TAG_VALUE_END = 19999

    fun appendDocumentation(builder: StringBuilder, documentation: String) {
        if (documentation.isEmpty()) {
            return
        }
        for (line in documentation.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
            builder.append("// ").append(line).append('\n')
        }
    }

    fun appendOptions(builder: StringBuilder, options: List<OptionElement>) {
        builder.append("[\n")
        var i = 0
        val count = options.size
        while (i < count) {
            val endl = if (i < count - 1) "," else ""
            appendIndented(builder, options[i].toSchema() + endl)
            i++
        }
        builder.append(']')
    }

    fun appendIndented(builder: StringBuilder, value: String) {
        for (line in value.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            builder.append("  ").append(line).append('\n')
        }
    }

    /** True if the supplied value is in the valid tag range and not reserved.  */
    fun isValidTag(value: Int): Boolean {
        return value in MIN_TAG_VALUE..(RESERVED_TAG_VALUE_START - 1) ||
                value in (RESERVED_TAG_VALUE_END + 1)..MAX_TAG_VALUE
    }

    fun <T> concatenate(a: List<T>, b: T): List<T> {
        return mutableListOf<T>().apply {
            addAll(a)
            add(b)
        }
    }
}

