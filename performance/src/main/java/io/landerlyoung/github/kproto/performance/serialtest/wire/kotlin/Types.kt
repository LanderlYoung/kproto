// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: proto.proto
package io.landerlyoung.github.kproto.performance.serialtest.wire.kotlin

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.TagHandler
import com.squareup.wire.WireField
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.jvm.JvmField
import okio.ByteString

data class Types(
    @field:WireField(tag = 1, adapter = "com.squareup.wire.ProtoAdapter.STRING") val title: String?
            = null,
    @field:WireField(tag = 2, adapter = "com.squareup.wire.ProtoAdapter.STRING") val name: String? =
            null,
    @field:WireField(tag = 3, adapter = "com.squareup.wire.ProtoAdapter.STRING") val gender: String?
            = null,
    @field:WireField(tag = 4, adapter = "com.squareup.wire.ProtoAdapter.INT32") val opt_int32: Int?
            = null,
    @field:WireField(tag = 5, adapter = "com.squareup.wire.ProtoAdapter.INT64") val opt_int64: Long?
            = null,
    @field:WireField(tag = 6, adapter = "com.squareup.wire.ProtoAdapter.SINT32") val opt_sint32:
            Int? = null,
    @field:WireField(tag = 7, adapter = "com.squareup.wire.ProtoAdapter.SINT64") val opt_sint64:
            Long? = null,
    @field:WireField(tag = 8, adapter = "com.squareup.wire.ProtoAdapter.BOOL") val opt_bool:
            Boolean? = null,
    @field:WireField(tag = 9, adapter = "com.squareup.wire.ProtoAdapter.FLOAT") val opt_float:
            Float? = null,
    @field:WireField(tag = 10, adapter = "com.squareup.wire.ProtoAdapter.DOUBLE") val opt_double:
            Double? = null,
    @field:WireField(tag = 11, adapter = "com.squareup.wire.ProtoAdapter.BYTES") val opt_bytes:
            ByteString? = null,
    @field:WireField(tag = 12, adapter = "com.squareup.wire.ProtoAdapter.INT32") val pack_int32:
            List<Int> = emptyList(),
    @field:WireField(tag = 13, adapter = "com.squareup.wire.ProtoAdapter.STRING") val pack_string:
            List<String> = emptyList(),
    @field:WireField(tag = 14, adapter = "TextItem.ADAPTER") val pack_msg: List<TextItem> =
            emptyList(),
    @field:WireField(tag = 15, adapter = "map_msgAdapter") val map_msg: Map<String, TextItem>,
    @field:WireField(tag = 16, adapter = "map_isAdapter") val map_is: Map<Int, String>,
    val unknownFields: ByteString = ByteString.EMPTY
) : Message<Types, Types.Builder>(ADAPTER, unknownFields) {
    @Deprecated(
            message = "Shouldn't be used in Kotlin",
            level = DeprecationLevel.HIDDEN
    )
    override fun newBuilder(): Builder = Builder(this.copy())

    class Builder(private val message: Types) : Message.Builder<Types, Builder>() {
        override fun build(): Types = message
    }

    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<Types> = object : ProtoAdapter<Types>(
            FieldEncoding.LENGTH_DELIMITED, 
            Types::class.java
        ) {
            private val map_msgAdapter: ProtoAdapter<Map<String, TextItem>> =
                    ProtoAdapter.newMapAdapter(ProtoAdapter.STRING, TextItem.ADAPTER)

            private val map_isAdapter: ProtoAdapter<Map<Int, String>> =
                    ProtoAdapter.newMapAdapter(ProtoAdapter.INT32, ProtoAdapter.STRING)

            override fun encodedSize(value: Types): Int = 
                ProtoAdapter.STRING.encodedSizeWithTag(1, value.title) +
                ProtoAdapter.STRING.encodedSizeWithTag(2, value.name) +
                ProtoAdapter.STRING.encodedSizeWithTag(3, value.gender) +
                ProtoAdapter.INT32.encodedSizeWithTag(4, value.opt_int32) +
                ProtoAdapter.INT64.encodedSizeWithTag(5, value.opt_int64) +
                ProtoAdapter.SINT32.encodedSizeWithTag(6, value.opt_sint32) +
                ProtoAdapter.SINT64.encodedSizeWithTag(7, value.opt_sint64) +
                ProtoAdapter.BOOL.encodedSizeWithTag(8, value.opt_bool) +
                ProtoAdapter.FLOAT.encodedSizeWithTag(9, value.opt_float) +
                ProtoAdapter.DOUBLE.encodedSizeWithTag(10, value.opt_double) +
                ProtoAdapter.BYTES.encodedSizeWithTag(11, value.opt_bytes) +
                ProtoAdapter.INT32.asRepeated().encodedSizeWithTag(12, value.pack_int32) +
                ProtoAdapter.STRING.asRepeated().encodedSizeWithTag(13, value.pack_string) +
                TextItem.ADAPTER.asRepeated().encodedSizeWithTag(14, value.pack_msg) +
                map_msgAdapter.encodedSizeWithTag(15, value.map_msg) +
                map_isAdapter.encodedSizeWithTag(16, value.map_is) +
                value.unknownFields.size

            override fun encode(writer: ProtoWriter, value: Types) {
                ProtoAdapter.STRING.encodeWithTag(writer, 1, value.title)
                ProtoAdapter.STRING.encodeWithTag(writer, 2, value.name)
                ProtoAdapter.STRING.encodeWithTag(writer, 3, value.gender)
                ProtoAdapter.INT32.encodeWithTag(writer, 4, value.opt_int32)
                ProtoAdapter.INT64.encodeWithTag(writer, 5, value.opt_int64)
                ProtoAdapter.SINT32.encodeWithTag(writer, 6, value.opt_sint32)
                ProtoAdapter.SINT64.encodeWithTag(writer, 7, value.opt_sint64)
                ProtoAdapter.BOOL.encodeWithTag(writer, 8, value.opt_bool)
                ProtoAdapter.FLOAT.encodeWithTag(writer, 9, value.opt_float)
                ProtoAdapter.DOUBLE.encodeWithTag(writer, 10, value.opt_double)
                ProtoAdapter.BYTES.encodeWithTag(writer, 11, value.opt_bytes)
                ProtoAdapter.INT32.asRepeated().encodeWithTag(writer, 12, value.pack_int32)
                ProtoAdapter.STRING.asRepeated().encodeWithTag(writer, 13, value.pack_string)
                TextItem.ADAPTER.asRepeated().encodeWithTag(writer, 14, value.pack_msg)
                map_msgAdapter.encodeWithTag(writer, 15, value.map_msg)
                map_isAdapter.encodeWithTag(writer, 16, value.map_is)
                writer.writeBytes(value.unknownFields)
            }

            override fun decode(reader: ProtoReader): Types {
                var title: String? = null
                var name: String? = null
                var gender: String? = null
                var opt_int32: Int? = null
                var opt_int64: Long? = null
                var opt_sint32: Int? = null
                var opt_sint64: Long? = null
                var opt_bool: Boolean? = null
                var opt_float: Float? = null
                var opt_double: Double? = null
                var opt_bytes: ByteString? = null
                val pack_int32 = mutableListOf<Int>()
                val pack_string = mutableListOf<String>()
                val pack_msg = mutableListOf<TextItem>()
                val map_msg = mutableMapOf<String, TextItem>()
                val map_is = mutableMapOf<Int, String>()
                val unknownFields = reader.forEachTag { tag ->
                    when (tag) {
                        1 -> title = ProtoAdapter.STRING.decode(reader)
                        2 -> name = ProtoAdapter.STRING.decode(reader)
                        3 -> gender = ProtoAdapter.STRING.decode(reader)
                        4 -> opt_int32 = ProtoAdapter.INT32.decode(reader)
                        5 -> opt_int64 = ProtoAdapter.INT64.decode(reader)
                        6 -> opt_sint32 = ProtoAdapter.SINT32.decode(reader)
                        7 -> opt_sint64 = ProtoAdapter.SINT64.decode(reader)
                        8 -> opt_bool = ProtoAdapter.BOOL.decode(reader)
                        9 -> opt_float = ProtoAdapter.FLOAT.decode(reader)
                        10 -> opt_double = ProtoAdapter.DOUBLE.decode(reader)
                        11 -> opt_bytes = ProtoAdapter.BYTES.decode(reader)
                        12 -> pack_int32.add(ProtoAdapter.INT32.decode(reader))
                        13 -> pack_string.add(ProtoAdapter.STRING.decode(reader))
                        14 -> pack_msg.add(TextItem.ADAPTER.decode(reader))
                        15 -> map_msg.putAll(map_msgAdapter.decode(reader))
                        16 -> map_is.putAll(map_isAdapter.decode(reader))
                        else -> TagHandler.UNKNOWN_TAG
                    }
                }
                return Types(
                    title = title,
                    name = name,
                    gender = gender,
                    opt_int32 = opt_int32,
                    opt_int64 = opt_int64,
                    opt_sint32 = opt_sint32,
                    opt_sint64 = opt_sint64,
                    opt_bool = opt_bool,
                    opt_float = opt_float,
                    opt_double = opt_double,
                    opt_bytes = opt_bytes,
                    pack_int32 = pack_int32,
                    pack_string = pack_string,
                    pack_msg = pack_msg,
                    map_msg = map_msg,
                    map_is = map_is,
                    unknownFields = unknownFields
                )
            }
        }
    }
}
