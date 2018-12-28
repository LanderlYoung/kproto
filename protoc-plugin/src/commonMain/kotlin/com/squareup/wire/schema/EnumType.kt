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
import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import com.squareup.wire.schema.internal.parser.EnumElement

import com.squareup.wire.schema.Options.ENUM_OPTIONS

class EnumType private constructor(private val protoType: ProtoType, private val location: Location, private val documentation: String, private val name: String,
                                   private val constants: List<EnumConstant>, private val options: Options) : Type() {
    private var allowAlias: Any? = null

    override fun location(): Location {
        return location
    }

    override fun type(): ProtoType {
        return protoType
    }

    override fun documentation(): String {
        return documentation
    }

    override fun options(): Options {
        return options
    }

    override fun nestedTypes(): List<Type> {
        return listOf() // Enums do not allow nested type declarations.
    }

    fun allowAlias(): Boolean {
        return "true" == allowAlias
    }

    /** Returns the constant named `name`, or null if this enum has no such constant.  */
    fun constant(name: String): EnumConstant? {
        for (constant in constants()) {
            if (constant.name() == name) {
                return constant
            }
        }
        return null
    }

    /** Returns the constant tagged `tag`, or null if this enum has no such constant.  */
    fun constant(tag: Int): EnumConstant? {
        for (constant in constants()) {
            if (constant.tag() == tag) {
                return constant
            }
        }
        return null
    }

    fun constants(): List<EnumConstant> {
        return constants
    }

    internal override fun link(linker: Linker) {}

    internal override fun linkOptions(linker: Linker) {
        options.link(linker)
        for (constant in constants) {
            constant.linkOptions(linker)
        }
        allowAlias = options.get(ALLOW_ALIAS)
    }

    internal override fun validate(linker: Linker) {
        var linker = linker
        linker = linker.withContext(this)

        if ("true" != allowAlias) {
            validateTagUniqueness(linker)
        }
    }

    private fun validateTagUniqueness(linker: Linker) {
        val tagToConstant = LinkedHashMultimap.create<Int, EnumConstant>()
        for (constant in constants) {
            tagToConstant.put(constant.tag(), constant)
        }

        for ((key, value) in tagToConstant.asMap()) {
            if (value.size > 1) {
                val error = StringBuilder()
                error.append(String.format("multiple enum constants share tag %s:", key))
                var index = 1
                for (constant in value) {
                    error.append(String.format("\n  %s. %s (%s)",
                            index++, constant.name(), constant.location()))
                }
                linker.addError("%s", error)
            }
        }
    }

    internal override fun retainAll(schema: Schema, markSet: MarkSet): Type? {
        // If this type is not retained, prune it.
        if (!markSet.contains(protoType)) return null

        val retainedConstants = List.builder<EnumConstant>()
        for (constant in constants) {
            if (markSet.contains(ProtoMember.get(protoType, constant.name()))) {
                retainedConstants.add(constant.retainAll(schema, markSet))
            }
        }

        val result = EnumType(protoType, location, documentation, name,
                retainedConstants.build(), options.retainAll(schema, markSet))
        result.allowAlias = allowAlias
        return result
    }

    internal fun toElement(): EnumElement {
        return EnumElement.builder(location)
                .name(name)
                .documentation(documentation)
                .constants(EnumConstant.toElements(constants))
                .options(options.toElements())
                .build()
    }

    companion object {
        internal val ALLOW_ALIAS = ProtoMember.get(ENUM_OPTIONS, "allow_alias")

        internal fun fromElement(protoType: ProtoType, enumElement: EnumElement): EnumType {
            val constants = EnumConstant.fromElements(enumElement.constants())
            val options = Options(Options.ENUM_OPTIONS, enumElement.options())

            return EnumType(protoType, enumElement.location(), enumElement.documentation(),
                    enumElement.name(), constants, options)
        }
    }
}
