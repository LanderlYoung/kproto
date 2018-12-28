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

data class MessageElement(
        override val location: Location,
        override val name: String,
        override val documentation: String = "",
        override val options: List<OptionElement> = listOf(),
        override val nestedTypes: List<TypeElement> = listOf(),
        val reserveds: List<ReservedElement>,
        val fields: List<FieldElement> = listOf(),
        val oneOfs: List<OneOfElement> = listOf(),
        val extensions: List<ExtensionsElement> = listOf(),
        val groups: List<GroupElement> = listOf()
) : TypeElement {

    override fun toSchema(): String {
        val builder = StringBuilder()
        appendDocumentation(builder, documentation)
        builder.append("message ")
                .append(name)
                .append(" {")
        if (!reserveds.isEmpty()) {
            builder.append('\n')
            for (reserved in reserveds) {
                appendIndented(builder, reserved.toSchema())
            }
        }
        if (!options.isEmpty()) {
            builder.append('\n')
            for (option in options) {
                appendIndented(builder, option.toSchemaDeclaration())
            }
        }
        if (!fields.isEmpty()) {
            builder.append('\n')
            for (field in fields) {
                appendIndented(builder, field.toSchema())
            }
        }
        if (!oneOfs.isEmpty()) {
            builder.append('\n')
            for (oneOf in oneOfs) {
                appendIndented(builder, oneOf.toSchema())
            }
        }
        if (!groups.isEmpty()) {
            builder.append('\n')
            for (group in groups) {
                appendIndented(builder, group.toSchema())
            }
        }
        if (!extensions.isEmpty()) {
            builder.append('\n')
            for (extension in extensions) {
                appendIndented(builder, extension.toSchema())
            }
        }
        if (!nestedTypes.isEmpty()) {
            builder.append('\n')
            for (type in nestedTypes) {
                appendIndented(builder, type.toSchema())
            }
        }
        return builder.append("}\n").toString()
    }

}
