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
package com.squareup.wire.schema.internal.parser

import com.squareup.wire.schema.internal.Util
import com.squareup.wire.schema.internal.Util.appendIndented

data class OptionElement(
        val name: String,
        val kind: Kind,
        val value: Any,
        val isParenthesized: Boolean = false
) {

    enum class Kind {
        STRING,
        BOOLEAN,
        NUMBER,
        ENUM,
        MAP,
        LIST,
        OPTION
    }

    @Suppress("UNCHECKED_CAST")
    fun toSchema(): String {
        val value = value
        when (kind) {
            OptionElement.Kind.STRING -> return formatName() + " = \"" + value + '"'.toString()
            OptionElement.Kind.BOOLEAN, OptionElement.Kind.NUMBER, OptionElement.Kind.ENUM -> return formatName() + " = " + value
            OptionElement.Kind.OPTION -> {
                val builder = StringBuilder()
                var optionValue = value as OptionElement
                // Treat nested options as non-parenthesized always, prevents double parentheses.
                optionValue = OptionElement(optionValue.name, optionValue.kind, optionValue.value)
                builder.append(formatName()).append('.').append(optionValue.toSchema())
                return builder.toString()
            }
            OptionElement.Kind.MAP -> {
                val builder = StringBuilder()
                builder.append(formatName()).append(" = {\n")

                val valueMap = value as Map<String, *>
                formatOptionMap(builder, valueMap)
                builder.append('}')
                return builder.toString()
            }
            OptionElement.Kind.LIST -> {
                val builder = StringBuilder()
                builder.append(formatName()).append(" = ")

                val optionList = value as List<OptionElement>
                Util.appendOptions(builder, optionList)
                return builder.toString()
            }
            else -> throw AssertionError()
        }
    }

    fun toSchemaDeclaration(): String {
        return "option " + toSchema() + ";\n"
    }

    private fun formatName(): String {
        return if (isParenthesized) '('.toString() + name + ')'.toString() else name
    }

    companion object {

        internal fun formatOptionMap(builder: StringBuilder, valueMap: Map<String, *>) {
            val entries = valueMap.entries.toList()
            var i = 0
            val count = entries.size
            while (i < count) {
                val entry = entries[i]
                val endl = if (i < count - 1) "," else ""
                appendIndented(builder,
                        entry.key + ": " + formatOptionMapValue(entry.value!!) + endl)
                i++
            }
        }

        @Suppress("UNCHECKED_CAST")
        internal fun formatOptionMapValue(value: Any): String {
            if (value is String) {
                return "\"" + value + '"'.toString()
            }
            if (value is Map<*, *>) {
                val builder = StringBuilder().append("{\n")

                val map = value as Map<String, *>
                formatOptionMap(builder, map)
                return builder.append('}').toString()
            }
            if (value is List<*>) {
                val builder = StringBuilder().append("[\n")
                var i = 0
                val count = value.size
                while (i < count) {
                    val endl = if (i < count - 1) "," else ""
                    appendIndented(builder, formatOptionMapValue(value[i]!!) + endl)
                    i++
                }
                return builder.append("]").toString()
            }
            return value.toString()
        }
    }
}
