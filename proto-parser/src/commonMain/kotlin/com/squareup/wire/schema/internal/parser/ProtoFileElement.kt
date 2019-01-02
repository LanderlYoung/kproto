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
import com.squareup.wire.schema.ProtoFile

/** A single `.proto` file.  */
data class ProtoFileElement(
        val location: Location,
        val packageName: String? = null,
        val syntax: ProtoFile.Syntax? = null,
        val imports: List<String> = listOf(),
        val publicImports: List<String> = listOf(),
        val types: List<TypeElement> = listOf(),
        val services: List<ServiceElement> = listOf(),
        val extendDeclarations: List<ExtendElement> = listOf(),
        val options: List<OptionElement> = listOf()

) {

   fun toSchema(): String {
        val builder = StringBuilder()
        builder.append("// ").append(location).append('\n')
        if (syntax != null) {
            builder.append("syntax = \"").append(syntax).append("\";\n")
        }
        if (packageName != null) {
            builder.append("package ").append(packageName).append(";\n")
        }
        if (!imports.isEmpty() || !publicImports.isEmpty()) {
            builder.append('\n')
            for (file in imports) {
                builder.append("import \"").append(file).append("\";\n")
            }
            for (file in publicImports) {
                builder.append("import public \"").append(file).append("\";\n")
            }
        }
        if (!options.isEmpty()) {
            builder.append('\n')
            for (option in options) {
                builder.append(option.toSchemaDeclaration())
            }
        }
        if (!types.isEmpty()) {
            builder.append('\n')
            for (typeElement in types) {
                builder.append(typeElement.toSchema())
            }
        }
        if (!extendDeclarations.isEmpty()) {
            builder.append('\n')
            for (extendDeclaration in extendDeclarations) {
                builder.append(extendDeclaration.toSchema())
            }
        }
        if (!services.isEmpty()) {
            builder.append('\n')
            for (service in services) {
                builder.append(service.toSchema())
            }
        }
        return builder.toString()
    }
}
