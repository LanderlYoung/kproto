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

import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.Util.appendDocumentation
import com.squareup.wire.schema.internal.Util.appendIndented

data class EnumElement(
        override val location: Location,
        override val name: String,
        override val documentation: String,
        override val options: List<OptionElement> = listOf(),
        val constants: List<EnumConstantElement> = listOf()) : TypeElement {

    override val nestedTypes: List<TypeElement> = listOf()

    override fun toSchema(): String {
        val builder = StringBuilder()
        appendDocumentation(builder, documentation)
        builder.append("enum ")
                .append(name)
                .append(" {")
        if (!options.isEmpty()) {
            builder.append('\n')
            for (option in options) {
                appendIndented(builder, option.toSchemaDeclaration())
            }
        }
        if (!constants.isEmpty()) {
            builder.append('\n')
            for (constant in constants) {
                appendIndented(builder, constant.toSchema())
            }
        }
        return builder.append("}\n").toString()
    }
}
