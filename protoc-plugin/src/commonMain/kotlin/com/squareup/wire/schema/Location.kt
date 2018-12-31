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

/**
 * Locates a .proto file, or a position within a .proto file, on the file system. This includes a
 * base directory or a .jar file, and a path relative to that base.
 */
data class Location(
        /** Returns the base of this location; typically a directory or .jar file.  */
        val base: String,

        /** Returns the path to this location relative to [.base].  */
        val path: String,

        /** Returns the line number of this location, or -1 for no specific line number.  */
        val line: Int,

        /** Returns the column on the line of this location, or -1 for no specific column.  */
        val column: Int
) {

    fun at(line: Int, column: Int): Location {
        return copy(line = line, column = column)
    }

    /** Returns a copy of this location with an empty base.  */
    fun withoutBase(): Location {
        return copy(base = "")
    }

    /** Returns a copy of this location including only its path.  */
    fun withPathOnly(): Location {
        return copy(base = "", line = -1, column = -1)
    }


    override fun toString(): String {
        val result = StringBuilder()
        if (!base.isEmpty()) {
            result.append(base).append("/")
        }
        result.append(path)
        if (line != -1) {
            result.append(" at ").append(line)
            if (column != -1) {
                result.append(':').append(column)
            }
        }
        return result.toString()
    }

    companion object {
        operator fun get(path: String): Location {
            return get("", path)
        }

        operator fun get(base: String, path: String) =
                Location(base.trimEnd('/'),
                        path.trimStart('/'),
                        -1,
                        -1)
    }
}
