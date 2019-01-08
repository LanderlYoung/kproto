// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: proto.proto
package io.landerlyoung.github.kproto.performance.serialtest.wire.kotlin

import com.squareup.wire.EnumAdapter
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.WireEnum

enum class ItemType(private val value: Int) : WireEnum {
    TEXT(0),

    PICTURE(1),

    BINARY(100);

    override fun getValue(): Int = value

    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<ItemType> = object : EnumAdapter<ItemType>(
            ItemType::class.java
        ) {
            override fun fromValue(value: Int): ItemType = ItemType.fromValue(value)
        }

        @JvmStatic
        fun fromValue(value: Int): ItemType = when (value) {
            0 -> TEXT
            1 -> PICTURE
            100 -> BINARY
            else -> throw IllegalArgumentException("Unexpected value: $value")
        }
    }
}
