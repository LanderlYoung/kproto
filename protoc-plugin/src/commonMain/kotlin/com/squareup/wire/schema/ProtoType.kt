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
 * Names a protocol buffer message, enumerated type, service, map, or a scalar. This class models a
 * fully-qualified name using the protocol buffer package.
 */
class ProtoType {

    val isScalar: Boolean
    private val string: String
    val isMap: Boolean
    private val keyType: ProtoType?
    private val valueType: ProtoType?

    /** Creates a scalar or message type.  */
    private constructor(isScalar: Boolean, string: String) {
        this.isScalar = isScalar
        this.string = string
        this.isMap = false
        this.keyType = null
        this.valueType = null
    }

    /** Creates a map type.  */
    internal constructor(keyType: ProtoType, valueType: ProtoType, string: String) {
        check(keyType.isScalar && keyType != BYTES && keyType != DOUBLE && keyType != FLOAT) {
            "map key must be non-byte, non-floating point scalar: $keyType"
        }
        this.isScalar = false
        this.string = string
        this.isMap = true
        this.keyType = keyType // TODO restrict what's allowed here
        this.valueType = valueType
    }

    fun simpleName(): String {
        val dot = string.lastIndexOf('.')
        return string.substring(dot + 1)
    }

    /** Returns the enclosing type, or null if this type is not nested in another type.  */
    fun enclosingTypeOrPackage(): String? {
        val dot = string.lastIndexOf('.')
        return if (dot == -1) null else string.substring(0, dot)
    }

    /** The type of the map's keys. Only present when [.isMap] is true.  */
    fun keyType(): ProtoType? {
        return keyType
    }

    /** The type of the map's values. Only present when [.isMap] is true.  */
    fun valueType(): ProtoType? {
        return valueType
    }

    fun nestedType(name: String?): ProtoType {
        if (isScalar) {
            throw UnsupportedOperationException("scalar cannot have a nested type")
        }
        if (isMap) {
            throw UnsupportedOperationException("map cannot have a nested type")
        }
        if (name == null || name.contains(".") || name.isEmpty()) {
            throw IllegalArgumentException("unexpected name: " + name!!)
        }
        return ProtoType(false, string + '.'.toString() + name)
    }

    override fun equals(o: Any?): Boolean {
        return o is ProtoType && string == o.string
    }

    override fun hashCode(): Int {
        return string.hashCode()
    }

    override fun toString(): String {
        return string
    }

    companion object {
        val BOOL = ProtoType(true, "bool")
        val BYTES = ProtoType(true, "bytes")
        val DOUBLE = ProtoType(true, "double")
        val FLOAT = ProtoType(true, "float")
        val FIXED32 = ProtoType(true, "fixed32")
        val FIXED64 = ProtoType(true, "fixed64")
        val INT32 = ProtoType(true, "int32")
        val INT64 = ProtoType(true, "int64")
        val SFIXED32 = ProtoType(true, "sfixed32")
        val SFIXED64 = ProtoType(true, "sfixed64")
        val SINT32 = ProtoType(true, "sint32")
        val SINT64 = ProtoType(true, "sint64")
        val STRING = ProtoType(true, "string")
        val UINT32 = ProtoType(true, "uint32")
        val UINT64 = ProtoType(true, "uint64")

        private val SCALAR_TYPES: Map<String, ProtoType>

        init {
            SCALAR_TYPES = mapOf(
                    BOOL.string to BOOL,
                    BYTES.string to BYTES,
                    DOUBLE.string to DOUBLE,
                    FLOAT.string to FLOAT,
                    FIXED32.string to FIXED32,
                    FIXED64.string to FIXED64,
                    INT32.string to INT32,
                    INT64.string to INT64,
                    SFIXED32.string to SFIXED32,
                    SFIXED64.string to SFIXED64,
                    SINT32.string to SINT32,
                    SINT64.string to SINT64,
                    STRING.string to STRING,
                    UINT32.string to UINT32,
                    UINT64.string to UINT64
            )
        }

        operator fun get(enclosingTypeOrPackage: String?, typeName: String): ProtoType {
            return if (enclosingTypeOrPackage != null)
                get(enclosingTypeOrPackage + '.'.toString() + typeName)
            else
                get(typeName)
        }

        operator fun get(name: String?): ProtoType {
            val scalar = SCALAR_TYPES.get(name)
            if (scalar != null) return scalar

            if (name == null || name.isEmpty() || name.contains("#")) {
                throw IllegalArgumentException("unexpected name: " + name!!)
            }

            if (name.startsWith("map<") && name.endsWith(">")) {
                val comma = name.indexOf(',')
                if (comma == -1) throw IllegalArgumentException("expected ',' in map type: $name")
                val key = get(name.substring(4, comma).trim { it <= ' ' })
                val value = get(name.substring(comma + 1, name.length - 1).trim { it <= ' ' })
                return ProtoType(key, value, name)
            }

            return ProtoType(false, name)
        }
    }
}
