// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: proto.proto
package io.landerlyoung.github.kproto.performance.serialtest.wire.kotlin

import com.squareup.wire.*
import com.squareup.wire.Message
import okio.ByteString

data class CompoundItem(val item: Item? = null, val unknownFields: ByteString = ByteString.EMPTY) :
        Message<CompoundItem, CompoundItem.Builder>(ADAPTER, unknownFields) {
    @Deprecated(
            message = "Shouldn't be used in Kotlin",
            level = DeprecationLevel.HIDDEN
    )
    override fun newBuilder(): Builder = Builder(this.copy())

    class Builder(private val message: CompoundItem) : Message.Builder<CompoundItem, Builder>() {
        override fun build(): CompoundItem = message
    }

    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<CompoundItem> = object : ProtoAdapter<CompoundItem>(
            FieldEncoding.LENGTH_DELIMITED, 
            CompoundItem::class.java
        ) {
            override fun encodedSize(value: CompoundItem): Int = 
                when (value.item) {
                    is Item.Text -> TextItem.ADAPTER.encodedSizeWithTag(1, value.item.text)
                    is Item.Picture -> PictureItem.ADAPTER.encodedSizeWithTag(2, value.item.picture)
                    else -> 0
                } +
                value.unknownFields.size

            override fun encode(writer: ProtoWriter, value: CompoundItem) {
                when (value.item) {
                    is Item.Text -> TextItem.ADAPTER.encodeWithTag(writer, 1, value.item.text)
                    is Item.Picture -> PictureItem.ADAPTER.encodeWithTag(writer, 2,
                            value.item.picture)
                }
                writer.writeBytes(value.unknownFields)
            }

            override fun decode(reader: ProtoReader): CompoundItem {
                var item: Item? = null
                val unknownFields = reader.forEachTag { tag ->
                    when (tag) {
                        1 -> item = Item.Text(TextItem.ADAPTER.decode(reader))
                        2 -> item = Item.Picture(PictureItem.ADAPTER.decode(reader))
                        else -> TagHandler.UNKNOWN_TAG
                    }
                }
                return CompoundItem(
                    item = item,
                    unknownFields = unknownFields
                )
            }
        }
    }

    sealed class Item {
        data class Text(val text: TextItem) : Item()

        data class Picture(val picture: PictureItem) : Item()
    }
}
