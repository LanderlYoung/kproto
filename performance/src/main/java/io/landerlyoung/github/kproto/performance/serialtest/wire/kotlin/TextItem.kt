// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: proto.proto
package io.landerlyoung.github.kproto.performance.serialtest.wire.kotlin

import com.squareup.wire.*
import com.squareup.wire.Message
import okio.ByteString

data class TextItem(@field:WireField(tag = 1, adapter = "com.squareup.wire.ProtoAdapter.STRING") val
        text: String? = null, val unknownFields: ByteString = ByteString.EMPTY) : Message<TextItem,
        TextItem.Builder>(ADAPTER, unknownFields) {
    @Deprecated(
            message = "Shouldn't be used in Kotlin",
            level = DeprecationLevel.HIDDEN
    )
    override fun newBuilder(): Builder = Builder(this.copy())

    class Builder(private val message: TextItem) : Message.Builder<TextItem, Builder>() {
        override fun build(): TextItem = message
    }

    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<TextItem> = object : ProtoAdapter<TextItem>(
            FieldEncoding.LENGTH_DELIMITED, 
            TextItem::class.java
        ) {
            override fun encodedSize(value: TextItem): Int = 
                ProtoAdapter.STRING.encodedSizeWithTag(1, value.text) +
                value.unknownFields.size

            override fun encode(writer: ProtoWriter, value: TextItem) {
                ProtoAdapter.STRING.encodeWithTag(writer, 1, value.text)
                writer.writeBytes(value.unknownFields)
            }

            override fun decode(reader: ProtoReader): TextItem {
                var text: String? = null
                val unknownFields = reader.forEachTag { tag ->
                    when (tag) {
                        1 -> text = ProtoAdapter.STRING.decode(reader)
                        else -> TagHandler.UNKNOWN_TAG
                    }
                }
                return TextItem(
                    text = text,
                    unknownFields = unknownFields
                )
            }
        }
    }
}
