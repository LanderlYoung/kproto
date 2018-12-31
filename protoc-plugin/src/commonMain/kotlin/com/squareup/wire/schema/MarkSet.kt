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

import com.squareup.wire.schema.internal.Multimap

/**
 * A mark set is used in three phases:
 *
 *
 *  1. Marking root types and root members. These are the identifiers specifically identified by
 * the user in the includes set. In this phase it is an error to mark a type that is excluded,
 * or to both a type and one of its members.
 *  1. Marking members transitively reachable by those roots. In this phase if a member is
 * visited, the member's enclosing type is marked instead, unless it is of a type that has a
 * specific member already marked.
 *  1. Retaining which members and types have been marked.
 *
 */
internal class MarkSet(val identifierSet: IdentifierSet) {
    val types: MutableSet<ProtoType> = LinkedHashSet()
    val members = Multimap<ProtoType, ProtoMember>()

    /**
     * Marks `protoMember`, throwing if it is explicitly excluded, or if its enclosing type is
     * also specifically included. This implicitly excludes other members of the same type.
     */
    fun root(protoMember: ProtoMember?) {
        if (protoMember == null) throw NullPointerException("protoMember == null")
        check(!identifierSet.excludes(protoMember))
        check(!types.contains(protoMember.type))
        members.put(protoMember.type, protoMember)
    }

    /**
     * Marks `type`, throwing if it is explicitly excluded, or if any of its members are also
     * specifically included.
     */
    fun root(type: ProtoType?) {
        if (type == null) throw NullPointerException("type == null")
        check(!identifierSet.excludes(type))
        check(!members.containsKey(type))
        types.add(type)
    }

    /**
     * Marks a type as transitively reachable by the includes set. Returns true if the mark is new,
     * the type will be retained, and its own dependencies should be traversed.
     */
    fun mark(type: ProtoType?): Boolean {
        if (type == null) throw NullPointerException("type == null")
        return if (identifierSet.excludes(type)) false else types.add(type)
    }

    /**
     * Marks a member as transitively reachable by the includes set. Returns true if the mark is new,
     * the member will be retained, and its own dependencies should be traversed.
     */
    fun mark(protoMember: ProtoMember?): Boolean {
        if (protoMember == null) throw NullPointerException("type == null")
        if (identifierSet.excludes(protoMember)) return false
        return if (members.containsKey(protoMember.type))
            members.put(protoMember.type, protoMember)
        else
            types.add(protoMember.type)
    }

    /** Returns true if all members of `type` are marked and should be retained.  */
    fun containsAllMembers(type: ProtoType?): Boolean {
        if (type == null) throw NullPointerException("type == null")
        return types.contains(type) && !members.containsKey(type)
    }

    /** Returns true if `type` is marked and should be retained.  */
    operator fun contains(type: ProtoType?): Boolean {
        if (type == null) throw NullPointerException("type == null")
        return types.contains(type)
    }

    /** Returns true if `member` is marked and should be retained.  */
    operator fun contains(protoMember: ProtoMember?): Boolean {
        if (protoMember == null) throw NullPointerException("protoMember == null")
        if (identifierSet.excludes(protoMember)) return false
        return members[protoMember.type].contains(protoMember)
    }
}
