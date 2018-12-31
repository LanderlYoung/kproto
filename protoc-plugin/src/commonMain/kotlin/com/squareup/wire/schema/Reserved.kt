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
package com.squareup.wire.schema

import com.squareup.wire.schema.internal.parser.ReservedElement

internal class Reserved private constructor(
        private val location: Location,
        private val documentation: String,
        private val values: List<Any>) {

    fun location(): Location {
        return location
    }

    fun documentation(): String {
        return documentation
    }

    fun values(): List<Any> {
        return values
    }

    fun matchesTag(tag: Int): Boolean {
        for (value in values) {
            if (value is Int && tag == value) {
                return true
            }
            if (value is IntRange && value.contains(tag)) {
                return true
            }
        }
        return false
    }

    fun matchesName(name: String): Boolean {
        for (value in values) {
            if (value is String && name == value) {
                return true
            }
        }
        return false
    }

    companion object {

        fun fromElements(reserveds: List<ReservedElement>): List<Reserved> {
            val builder = mutableListOf<Reserved>()
            for (reserved in reserveds) {
                builder.add(Reserved(reserved.location, reserved.documentation, reserved.values))
            }
            return builder
        }

        fun toElements(reserveds: List<Reserved>): List<ReservedElement> {
            val builder = mutableListOf<ReservedElement>()
            for (reserved in reserveds) {
                builder.add(ReservedElement(reserved.location(), reserved.documentation(), reserved.values()))
            }
            return builder
        }
    }
}
