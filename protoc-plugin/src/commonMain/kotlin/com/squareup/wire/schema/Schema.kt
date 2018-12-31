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
 * A collection of .proto files that describe a set of messages. A schema is *linked*: each
 * field's type name is resolved to the corresponding type definition.
 *
 *
 * Use [SchemaLoader] to load a schema from source files.
 */
class Schema internal constructor(protoFiles: Iterable<ProtoFile>) {

    private val protoFiles: List<ProtoFile>
    private val typesIndex: Map<String, Type>
    private val servicesIndex: Map<String, Service>

    init {
        this.protoFiles = protoFiles.sortedWith(Comparator { left, right ->
            left.location.path.compareTo(right.location.path)
        })
        this.typesIndex = buildTypesIndex(protoFiles)
        this.servicesIndex = buildServicesIndex(protoFiles)
    }

    fun protoFiles(): List<ProtoFile> {
        return protoFiles
    }

    /** Returns the proto file at `path`, or null if this schema has no such file.  */
    fun protoFile(path: String): ProtoFile? {
        for (protoFile in protoFiles) {
            if (protoFile.location.path == path) {
                return protoFile
            }
        }
        return null
    }

    /**
     * Returns a copy of this schema that retains only the types and services selected by `identifierSet`, plus their transitive dependencies.
     */
    fun prune(identifierSet: IdentifierSet): Schema {
        return Pruner(this, identifierSet).prune()
    }

    /**
     * Returns the service with the fully qualified name `name`, or null if this schema defines
     * no such service.
     */
    fun getService(name: String): Service? {
        return servicesIndex[name]
    }

    /**
     * Returns the service with the fully qualified name `name`, or null if this schema defines
     * no such service.
     */
    fun getService(protoType: ProtoType): Service? {
        return getService(protoType.toString())
    }

    /**
     * Returns the type with the fully qualified name `name`, or null if this schema defines no
     * such type.
     */
    fun getType(name: String): Type? {
        return typesIndex[name]
    }

    /**
     * Returns the type with the fully qualified name `name`, or null if this schema defines no
     * such type.
     */
    fun getType(protoType: ProtoType): Type? {
        return getType(protoType.toString())
    }

    fun getField(protoMember: ProtoMember): Field? {
        val type = getType(protoMember.type) as? MessageType ?: return null
        var field = type.field(protoMember.member)
        if (field == null) {
            field = type.extensionField(protoMember.member)
        }
        return field
    }

    /**
     * Returns a wire adapter for the message or enum type named `typeName`. The returned type
     * adapter doesn't have model classes to encode and decode from, so instead it uses scalar types
     * ([String], [ByteString][okio.ByteString], [Integer], etc.),
     * [maps][Map], and [lists][java.util.List]. It can both encode and decode
     * these objects. Map keys are field names.
     *
     * @param includeUnknown true to include values for unknown tags in the returned model. Map keys
     * for such values is the unknown value's tag name as a string. Unknown values are decoded to
     * [Long], [Long], [Integer], or [     ByteString][okio.ByteString] for [VARINT][com.squareup.wire.FieldEncoding.VARINT], [     ][com.squareup.wire.FieldEncoding.FIXED64], [     ][com.squareup.wire.FieldEncoding.FIXED32], or [     ][com.squareup.wire.FieldEncoding.LENGTH_DELIMITED] respectively.
     */
//    fun protoAdapter(typeName: String, includeUnknown: Boolean): ProtoAdapter<Any> {
//        val type = getType(typeName) ?: throw IllegalArgumentException("unexpected type $typeName")
//        return SchemaProtoAdapterFactory(this, includeUnknown)[type.type]
//    }

    companion object {
        private fun buildTypesIndex(protoFiles: Iterable<ProtoFile>): Map<String, Type> {
            val result = mutableMapOf<String, Type>()
            for (protoFile in protoFiles) {
                for (type in protoFile.types()) {
                    index(result, type)
                }
            }
            return result
        }

        private fun index(typesByName: MutableMap<String, Type>, type: Type) {
            typesByName[type.type.toString()] = type
            for (nested in type.nestedTypes) {
                index(typesByName, nested)
            }
        }

        private fun buildServicesIndex(protoFiles: Iterable<ProtoFile>): Map<String, Service> {
            val result = mutableMapOf<String, Service>()
            for (protoFile in protoFiles) {
                for (service in protoFile.services()) {
                    result[service.type.toString()] = service
                }
            }
            return result
        }
    }
}
