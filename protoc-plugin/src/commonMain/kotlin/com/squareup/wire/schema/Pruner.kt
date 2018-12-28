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
import java.util.ArrayDeque
import java.util.Deque

/**
 * Creates a new schema that contains only the types selected by an identifier set, including their
 * transitive dependencies.
 */
internal class Pruner(val schema: Schema, val identifierSet: IdentifierSet) {
    val marks: MarkSet

    /**
     * [types][ProtoType] and [members][ProtoMember] whose immediate dependencies have not
     * yet been visited.
     */
    val queue: Deque<Any>

    init {
        this.marks = MarkSet(identifierSet)
        this.queue = ArrayDeque()
    }

    fun prune(): Schema {
        markRoots()
        markReachable()

        val retained = mutableListOf<ProtoFile>()
        for (protoFile in schema.protoFiles()) {
            retained.add(protoFile.retainAll(schema, marks))
        }

        return Schema(retained)
    }

    private fun markRoots() {
        for (protoFile in schema.protoFiles()) {
            markRoots(protoFile)
        }
    }

    private fun markRoots(protoFile: ProtoFile) {
        for (type in protoFile.types()) {
            markRoots(type)
        }
        for (service in protoFile.services()) {
            markRoots(service)
        }
    }

    private fun markRoots(type: Type) {
        val protoType = type.type()
        if (identifierSet.includes(protoType)) {
            marks.root(protoType)
            queue.add(protoType)
        } else {
            if (type is MessageType) {
                for (field in type.fieldsAndOneOfFields()) {
                    markRoots(ProtoMember.get(protoType, field.name()))
                }
            } else if (type is EnumType) {
                for (enumConstant in type.constants()) {
                    markRoots(ProtoMember.get(protoType, enumConstant.name()))
                }
            } else {
                throw AssertionError()
            }
        }

        for (nested in type.nestedTypes()) {
            markRoots(nested)
        }
    }

    private fun markRoots(service: Service) {
        val protoType = service.type()
        if (identifierSet.includes(protoType)) {
            marks.root(protoType)
            queue.add(protoType)
        } else {
            for (rpc in service.rpcs()) {
                markRoots(ProtoMember.get(protoType, rpc.name()))
            }
        }
    }

    private fun markRoots(protoMember: ProtoMember) {
        if (identifierSet.includes(protoMember)) {
            marks.root(protoMember)
            queue.add(protoMember)
        }
    }

    private fun markReachable() {
        // Mark everything reachable by what's enqueued, queueing new things as we go.
        var root: Any
        while ((root = queue.poll()) != null) {
            if (root is ProtoMember) {
                val protoMember = root
                mark(protoMember.type())
                val member = root.member()
                val type = schema.getType(protoMember.type())
                if (type is MessageType) {
                    var field = type.field(member)
                    if (field == null) {
                        field = type.extensionField(member)
                    }
                    if (field != null) {
                        markField(type.type(), field)
                        continue
                    }
                } else if (type is EnumType) {
                    val constant = type.constant(member)
                    if (constant != null) {
                        markOptions(constant.options())
                        continue
                    }
                }

                val service = schema.getService(protoMember.type())
                if (service != null) {
                    val rpc = service.rpc(member)
                    if (rpc != null) {
                        markRpc(service.type(), rpc)
                        continue
                    }
                }

                throw IllegalArgumentException("Unexpected member: $root")

            } else if (root is ProtoType) {
                val protoType = root
                if (protoType.isScalar) {
                    continue // Skip scalar types.
                }

                val type = schema.getType(protoType)
                if (type != null) {
                    markType(type)
                    continue
                }

                val service = schema.getService(protoType)
                if (service != null) {
                    markService(service)
                    continue
                }

                throw IllegalArgumentException("Unexpected type: $root")

            } else {
                throw AssertionError()
            }
        }
    }

    private fun mark(type: ProtoType) {
        var type = type
        // Mark the map type as it's non-scalar and transitively reachable.
        if (type.isMap) {
            marks.mark(type)
            // Map key type is always scalar. No need to mark it.
            type = type.valueType()
        }

        if (marks.mark(type)) {
            queue.add(type) // The transitive dependencies of this type must be visited.
        }
    }

    private fun mark(protoMember: ProtoMember) {
        if (marks.mark(protoMember)) {
            queue.add(protoMember) // The transitive dependencies of this member must be visited.
        }
    }

    private fun markType(type: Type) {
        markOptions(type.options())

        if (marks.containsAllMembers(type.type())) {
            if (type is MessageType) {
                markMessage(type)
            } else if (type is EnumType) {
                markEnum(type)
            }
        }
    }

    private fun markMessage(message: MessageType) {
        markFields(message.type(), message.fields())
        for (oneOf in message.oneOfs()) {
            markFields(message.type(), oneOf.fields())
        }
    }

    private fun markEnum(wireEnum: EnumType) {
        markOptions(wireEnum.options())
        if (marks.containsAllMembers(wireEnum.type())) {
            for (constant in wireEnum.constants()) {
                if (marks.contains(ProtoMember.get(wireEnum.type(), constant.name()))) {
                    markOptions(constant.options())
                }
            }
        }
    }

    private fun markFields(declaringType: ProtoType, fields: List<Field>) {
        for (field in fields) {
            markField(declaringType, field)
        }
    }

    private fun markField(declaringType: ProtoType, field: Field) {
        if (marks.contains(ProtoMember.get(declaringType, field.name()))) {
            markOptions(field.options())
            mark(field.type()!!)
        }
    }

    private fun markOptions(options: Options) {
        for ((_, value) in options.fields().entries()) {
            mark(value)
        }
    }

    private fun markService(service: Service) {
        markOptions(service.options())
        if (marks.containsAllMembers(service.type())) {
            for (rpc in service.rpcs()) {
                markRpc(service.type(), rpc)
            }
        }
    }

    private fun markRpc(declaringType: ProtoType, rpc: Rpc) {
        if (marks.contains(ProtoMember.get(declaringType, rpc.name()))) {
            markOptions(rpc.options())
            mark(rpc.requestType()!!)
            mark(rpc.responseType()!!)
        }
    }
}
