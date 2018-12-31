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

/*
package com.squareup.wire.schema

import com.squareup.wire.FieldEncoding
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import java.io.IOException
import java.util.ArrayList
import java.util.LinkedHashMap

/**
 * Creates type adapters to read and write protocol buffer data from a schema model. This doesn't
 * require an intermediate code gen step.
 */
internal class SchemaProtoAdapterFactory(val schema: Schema, val includeUnknown: Boolean) {
    val adapterMap: MutableMap<ProtoType, ProtoAdapter<*>> = LinkedHashMap()

    init {

        adapterMap[ProtoType.BOOL] = ProtoAdapter.BOOL
        adapterMap[ProtoType.BYTES] = ProtoAdapter.BYTES
        adapterMap[ProtoType.DOUBLE] = ProtoAdapter.DOUBLE
        adapterMap[ProtoType.FLOAT] = ProtoAdapter.FLOAT
        adapterMap[ProtoType.FIXED32] = ProtoAdapter.FIXED32
        adapterMap[ProtoType.FIXED64] = ProtoAdapter.FIXED64
        adapterMap[ProtoType.INT32] = ProtoAdapter.INT32
        adapterMap[ProtoType.INT64] = ProtoAdapter.INT64
        adapterMap[ProtoType.SFIXED32] = ProtoAdapter.SFIXED32
        adapterMap[ProtoType.SFIXED64] = ProtoAdapter.SFIXED64
        adapterMap[ProtoType.SINT32] = ProtoAdapter.SINT32
        adapterMap[ProtoType.SINT64] = ProtoAdapter.SINT64
        adapterMap[ProtoType.STRING] = ProtoAdapter.STRING
        adapterMap[ProtoType.UINT32] = ProtoAdapter.UINT32
        adapterMap[ProtoType.UINT64] = ProtoAdapter.UINT64
    }

    operator fun get(protoType: ProtoType): ProtoAdapter<Any> {
        if (protoType.isMap) throw UnsupportedOperationException("map types not supported")

        val result = adapterMap[protoType]
        if (result != null) {
            return result as ProtoAdapter<Any>?
        }

        val type = schema.getType(protoType)
                ?: throw IllegalArgumentException("unknown type: $protoType")

        if (type is EnumType) {
            val enumAdapter = EnumAdapter(type)
            adapterMap[protoType] = enumAdapter
            return enumAdapter
        }

        if (type is MessageType) {
            val messageAdapter = MessageAdapter(includeUnknown)
            // Put the adapter in the map early to mitigate the recursive calls to get() made below.
            adapterMap[protoType] = messageAdapter

            for (field in type.fields()) {
                val fieldAdapter = Field(
                        field.name(), field.tag(), field.isRepeated, get(field.type()!!))
                messageAdapter.fieldsByName[field.name()] = fieldAdapter
                messageAdapter.fieldsByTag[field.tag()] = fieldAdapter
            }
            return messageAdapter
        }

        throw IllegalArgumentException("unexpected type: $protoType")
    }

    internal class EnumAdapter(val enumType: EnumType) : ProtoAdapter<Any>(FieldEncoding.VARINT, Any::class.java) {

        override fun encodedSize(value: Any): Int {
            throw UnsupportedOperationException()
        }

        @Throws(IOException::class)
        override fun encode(writer: ProtoWriter, value: Any) {
            if (value is String) {
                val constant = enumType.constant(value)
                writer.writeVarint32(constant!!.tag)
            } else if (value is Int) {
                writer.writeVarint32(value)
            } else {
                throw IllegalArgumentException("unexpected " + enumType.type + ": " + value)
            }
        }

        @Throws(IOException::class)
        override fun decode(reader: ProtoReader): Any {
            val value = ProtoAdapter.UINT32.decode(reader)
            val constant = enumType.constant(value!!)
            return if (constant != null) constant.name() else value
        }
    }

    internal class MessageAdapter(val includeUnknown: Boolean) : ProtoAdapter<Map<String, Any>>(FieldEncoding.LENGTH_DELIMITED, Map<*, *>::class.java) {
        val fieldsByTag: MutableMap<Int, Field> = LinkedHashMap()
        val fieldsByName: MutableMap<String, Field> = LinkedHashMap()

        override fun redact(message: Map<String, Any>): Map<String, Any>? {
            throw UnsupportedOperationException()
        }

        override fun encodedSize(value: Map<String, Any>): Int {
            var size = 0
            for ((key, value1) in value) {
                val field = fieldsByName[key] ?: continue
// Ignore unknown values!

                val protoAdapter = field.protoAdapter as ProtoAdapter<Any>
                if (field.repeated) {
                    for (o in value1) {
                        size += protoAdapter.encodedSizeWithTag(field.tag, o)
                    }
                } else {
                    size += protoAdapter.encodedSizeWithTag(field.tag, value1)
                }
            }
            return size
        }

        @Throws(IOException::class)
        override fun encode(writer: ProtoWriter, value: Map<String, Any>) {
            for ((key, value1) in value) {
                val field = fieldsByName[key] ?: continue
// Ignore unknown values!

                val protoAdapter = field.protoAdapter as ProtoAdapter<Any>
                if (field.repeated) {
                    for (o in value1) {
                        protoAdapter.encodeWithTag(writer, field.tag, o)
                    }
                } else {
                    protoAdapter.encodeWithTag(writer, field.tag, value1)
                }
            }
        }

        @Throws(IOException::class)
        override fun decode(reader: ProtoReader): Map<String, Any> {
            val result = LinkedHashMap<String, Any>()

            val token = reader.beginMessage()
            var tag: Int
            while ((tag = reader.nextTag()) != -1) {
                var field: Field? = fieldsByTag[tag]
                if (field == null) {
                    if (includeUnknown) {
                        val name = Integer.toString(tag)
                        field = Field(name, tag, true, reader.peekFieldEncoding().rawProtoAdapter())
                    } else {
                        reader.skip()
                        continue
                    }
                }

                val value = field.protoAdapter.decode(reader)
                if (field.repeated) {
                    var values: MutableList<Any>? = result[field.name] as List<Any>
                    if (values == null) {
                        values = ArrayList()
                        result[field.name] = values
                    }
                    values.add(value)
                } else {
                    result[field.name] = value
                }
            }
            reader.endMessage(token)
            return result
        }

        override fun toString(value: Map<String, Any>): String {
            throw UnsupportedOperationException()
        }
    }

    internal class Field(val name: String, val tag: Int, val repeated: Boolean, val protoAdapter: ProtoAdapter<*>)
}
*/
