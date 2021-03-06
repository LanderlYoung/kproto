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
 * A heterogeneous set of rules to include and exclude types and members. If a member is included in
 * the set, its type is implicitly also included. A type that is included without a specific member
 * implicitly includes all of that type's members, but not its nested types.
 *
 *
 * Rules in this set may be in the following forms:
 *
 *  * Package names, followed by `.*`, like `squareup.protos.person.*`. This matches
 * types and services defined in the package and its descendant packages.
 *  * Fully qualified type and service names, like `squareup.protos.person.Person`.
 *  * Fully qualified member names, which are type names followed by a '#', followed by a member
 * name, like `squareup.protos.person.Person#address`. Members may be fields, enum
 * constants or RPCs.
 *
 *
 *
 * An identifier set populated with `Movie` and `Actor#name` contains all members of
 * `Movie` (such as `Movie#name` and `Movie#release_date`). It contains the type
 * `Actor` and one member `Actor#name`, but not `Actor#birth_date` or `Actor#oscar_count`.
 *
 *
 * This set has *included identifiers* and *excluded identifiers*, with excludes taking
 * precedence over includes. That is, if a type `Movie` is in both the includes and the
 * excludes, it is not contained in the set.
 *
 *
 * If the includes set is empty, that implies that all elements should be included. Use this to
 * exclude unwanted types and members without also including everything else.
 *
 *
 * Despite the builder, instances of this class are not safe for concurrent use.
 */
class IdentifierSet private constructor(builder: Builder) {
    private val includes: Set<String> = builder.includes
    private val excludes: Set<String> = builder.excludes
    private val usedIncludes = LinkedHashSet<String>()
    private val usedExcludes = LinkedHashSet<String>()

    val isEmpty: Boolean
        get() = includes.isEmpty() && excludes.isEmpty()

    /** Returns true if `type` is a root.  */
    fun includes(type: ProtoType): Boolean {
        return includes(type.toString())
    }

    /** Returns true if `protoMember` is a root.  */
    fun includes(protoMember: ProtoMember): Boolean {
        return includes(protoMember.toString())
    }

    /**
     * Returns true if `identifier` or any of its enclosing identifiers is included. If any
     * enclosing identifier is excluded, that takes precedence and this returns false.
     */
    private fun includes(identifier: String): Boolean {
        if (includes.isEmpty()) return !exclude(identifier)

        var includeMatch: String? = null
        var excludeMatch: String? = null
        var rule: String? = identifier
        while (rule != null) {
            if (excludes.contains(rule)) {
                excludeMatch = rule
            }
            if (includes.contains(rule)) {
                includeMatch = rule
            }
            rule = enclosing(rule)
        }
        if (excludeMatch != null) {
            usedExcludes.add(excludeMatch)
            return false
        }
        if (includeMatch != null) {
            usedIncludes.add(includeMatch)
            return true
        }
        return false
    }

    /**
     * Returns true if `type` should be excluded, even if it is a transitive dependency of a
     * root. In that case, the referring member is also excluded.
     */
    fun excludes(type: ProtoType): Boolean {
        return exclude(type.toString())
    }

    /** Returns true if `protoMember` should be excluded.  */
    fun excludes(protoMember: ProtoMember): Boolean {
        return exclude(protoMember.toString())
    }

    /** Returns true if `identifier` or any of its enclosing identifiers is excluded.  */
    private fun exclude(identifier: String): Boolean {
        var excludeMatch: String? = null
        var rule: String? = identifier
        while (rule != null) {
            if (excludes.contains(rule)) {
                excludeMatch = rule
            }
            rule = enclosing(rule)
        }
        if (excludeMatch != null) {
            usedExcludes.add(excludeMatch)
            return true
        }
        return false
    }

    fun unusedIncludes(): Set<String> {
        return includes + usedExcludes - includes.intersect(usedExcludes)
    }

    fun unusedExcludes(): Set<String> {
        return excludes + usedExcludes - excludes.intersect(usedExcludes)
    }

    class Builder {
        internal val includes: MutableSet<String> = mutableSetOf()
        internal val excludes: MutableSet<String> = mutableSetOf()

        fun include(identifier: String): Builder {
            includes.add(identifier)
            return this
        }

        fun include(identifiers: Iterable<String>): Builder {
            includes.addAll(identifiers)
            return this
        }

        fun exclude(identifier: String): Builder {
            excludes.add(identifier)
            return this
        }

        fun exclude(identifiers: Iterable<String>): Builder {
            excludes.addAll(identifiers)
            return this
        }

        fun build(): IdentifierSet {
            return IdentifierSet(this)
        }
    }

    companion object {

        /**
         * Returns the identifier or wildcard that encloses `identifier`, or null if it is not
         * enclosed.
         *
         *
         *  * If `identifier` is a member this returns the enclosing type.
         *  * If it is a type it returns the enclosing package with a wildcard, like `squareup.dinosaurs.*`.
         *  * If it is a package with a wildcard, it returns the parent package with a wildcard, like
         * `squareup.*`. The root wildcard is a lone asterisk, `*`.
         *
         */
        internal fun enclosing(identifier: String): String? {
            val hash = identifier.lastIndexOf('#')
            if (hash != -1) return identifier.substring(0, hash)

            val from = if (identifier.endsWith(".*")) identifier.length - 3 else identifier.length - 1
            val dot = identifier.lastIndexOf('.', from)
            if (dot != -1) return identifier.substring(0, dot) + ".*"

            return if (identifier != "*") "*" else null
        }
    }
}
