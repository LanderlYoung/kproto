// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: proto.proto
package io.landerlyoung.github.kproto.performance.serialtest.wire.kotlin

import com.squareup.wire.FieldEncoding
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.TagHandler
import com.squareup.wire.WireField
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.collections.List
import kotlin.jvm.JvmField
import okio.ByteString

data class Message(
    @field:WireField(tag = 1, adapter = "ItemType.ADAPTER") val itemType: ItemType? = null,
    @field:WireField(tag = 2, adapter = "CompoundItem.ADAPTER") val items: List<CompoundItem> =
            emptyList(),
    val unknownFields: ByteString = ByteString.EMPTY
) : com.squareup.wire.Message<Message, Message.Builder>(ADAPTER, unknownFields) {
    @Deprecated(
            message = "Shouldn't be used in Kotlin",
            level = DeprecationLevel.HIDDEN
    )
    override fun newBuilder(): Builder = Builder(this.copy())

    class Builder(private val message: Message) : com.squareup.wire.Message.Builder<Message,
            Builder>() {
        override fun build(): Message = message
    }

    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<Message> = object : ProtoAdapter<Message>(
            FieldEncoding.LENGTH_DELIMITED, 
            Message::class.java
        ) {
            override fun encodedSize(value: Message): Int = 
                ItemType.ADAPTER.encodedSizeWithTag(1, value.itemType) +
                CompoundItem.ADAPTER.asRepeated().encodedSizeWithTag(2, value.items) +
                value.unknownFields.size

            override fun encode(writer: ProtoWriter, value: Message) {
                ItemType.ADAPTER.encodeWithTag(writer, 1, value.itemType)
                CompoundItem.ADAPTER.asRepeated().encodeWithTag(writer, 2, value.items)
                writer.writeBytes(value.unknownFields)
            }

            override fun decode(reader: ProtoReader): Message {
                var itemType: ItemType? = null
                val items = mutableListOf<CompoundItem>()
                val unknownFields = reader.forEachTag { tag ->
                    when (tag) {
                        1 -> itemType = ItemType.ADAPTER.decode(reader)
                        2 -> items.add(CompoundItem.ADAPTER.decode(reader))
                        else -> TagHandler.UNKNOWN_TAG
                    }
                }
                return Message(
                    itemType = itemType,
                    items = items,
                    unknownFields = unknownFields
                )
            }
        }
    }
}
