package io.landerlyoung.github.kproto.performance

import io.landerlyoung.github.kproto.performance.serialtest.eq
import io.landerlyoung.github.kproto.performance.serialtest.javalite.Proto
import io.landerlyoung.github.kproto.performance.serialtest.wire.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * <pre>
 * Author: landerlyoung@gmail.com
 * Date:   2018-12-31
 * Time:   21:49
 * Life with Passion, Code with Creativity.
 * </pre>
 */

class PerformanceTest {
    @Before
    fun testTest() {
        // warm up
        for (i in 0..1000) {
            buildWireMessage()
            buildProtoMessage()
        }
    }

    @Test
    fun testWire() {
        val wireMessage = buildWireMessage()
        val protoMessage = buildProtoMessage()

        val wireOut = wireMessage.encode()
        val wireMessageCopy = Message.ADAPTER.decode(wireOut)

        assertEquals(wireMessage, wireMessageCopy)
    }

    @Test
    fun testProto() {
        val protoMessage = buildProtoMessage()
        val protoOut = protoMessage.toByteArray()

        val protoMessageCopy = Proto.Message.parseFrom(protoOut)

        assertEquals(protoMessage, protoMessageCopy)
    }

    // wire == proto == kotlin
    @Test
    fun testWireAndProto() {
        val wireMessage = buildWireMessage()
        val protoMessage = buildProtoMessage()

        assertTrue(wireMessage.eq(protoMessage))

        val wireOut = wireMessage.encode()
        val protoOut = protoMessage.toByteArray()

        val protoFromWire = Proto.Message.parseFrom(wireOut)
        val wireFromProto = Message.ADAPTER.decode(protoOut)

        assertEquals(wireMessage, wireFromProto)
        assertEquals(protoMessage, protoFromWire)

        assertTrue(wireFromProto.eq(protoFromWire))
    }

    private fun buildWireMessage(): Message {
        return Message.Builder()
                .itemType(ItemType.PICTURE)
                .items(listOf(
                        CompoundItem.Builder()
                                .text(TextItem("text_item_message"))
                                .build(),
                        CompoundItem.Builder()
                                .picture(PictureItem("picture_item_url"))
                                .build()
                ))
                .build()
    }

    private fun buildProtoMessage(): Proto.Message {
        return Proto.Message.newBuilder()
                .setItemType(Proto.ItemType.PICTURE)
                .addItems(Proto.CompoundItem.newBuilder()
                        .setText(Proto.TextItem.newBuilder()
                                .setText("text_item_message")
                                .build())
                        .build())
                .addItems(Proto.CompoundItem.newBuilder()
                        .setPicture(Proto.PictureItem.newBuilder()
                                .setPictureUrl("picture_item_url")
                                .build())
                        .build())
                .build()
    }
}
