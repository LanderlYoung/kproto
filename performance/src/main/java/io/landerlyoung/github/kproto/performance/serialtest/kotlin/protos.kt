package io.landerlyoung.github.kproto.performance.serialtest.kotlin

import kotlinx.serialization.*
import kotlinx.serialization.internal.IntDescriptor
import kotlinx.serialization.internal.SerialClassDescImpl

/**
 * <pre>
 * Author: landerlyoung@gmail.com
 * Date:   2019-01-02
 * Time:   18:45
 * Life with Passion, Code with Creativity.
 * </pre>
 */

object Kotlin {

    @Serializable(with = ItemType.Serializer::class)
    enum class ItemType {
        TEXT,
        PICTURE,
        BINARY;

        companion object Serializer : KSerializer<ItemType> {
            override val descriptor: SerialDescriptor
                get() = IntDescriptor

            override fun deserialize(input: Decoder): ItemType {
                val index = input.decodeInt()
                return when (index) {
                    0 -> TEXT
                    1 -> PICTURE
                    100 -> BINARY
                    else -> throw IllegalArgumentException("unknown enum type $index")
                }
            }

            override fun serialize(output: Encoder, obj: ItemType) {
                val index = when (obj) {
                    TEXT -> 0
                    PICTURE -> 1
                    BINARY -> 100
                }
                output.encodeInt(index)
            }
        }
    }

    @Serializable
    data class TextItem(
            @Optional
            @SerialId(1)
            val text: String = ""
    )


    @Serializable
    data class PictureItem(
            @Optional
            @SerialId(1)
            val pictureUrl: String = ""
    )

    @Serializable(with = CompoundItem.Serializer::class)
    data class CompoundItem(
            @Optional
            val item: Any? = null
    ) {
        companion object Serializer : KSerializer<CompoundItem> {

            inline fun serializer(): Serializer = this

            override val descriptor: SerialDescriptor


            init {
                val serialDescriptor =
                        SerialClassDescImpl("proto.package.CompoundItem")
                serialDescriptor.addElement("oneOfType")
                serialDescriptor.addElement("text")
                serialDescriptor.addElement("picture")

                descriptor = serialDescriptor
            }

            override fun deserialize(input: Decoder): CompoundItem {
                val decoder = input.beginStructure(descriptor)
                try {
                    val type = decoder.decodeElementIndex(descriptor)
                    return when (type) {
                        1 -> CompoundItem(decoder.decodeSerializableElement(descriptor, 1, TextItem.serializer()))
                        2 -> CompoundItem(decoder.decodeSerializableElement(descriptor, 2, PictureItem.serializer()))
                        else -> throw IllegalArgumentException("unknown oneOf type $type")
                    }
                } finally {
                    decoder.endStructure(descriptor)
                }
            }

            override fun serialize(output: Encoder, obj: CompoundItem) {
                val encoder = output.beginStructure(descriptor)
                try {
                    when (obj.item) {
                        null -> {
                            encoder.encodeNullableSerializableElement(descriptor, 1, TextItem.serializer(), null)
                        }
                        is TextItem -> {
                            encoder.encodeSerializableElement(descriptor, 1, TextItem.serializer(), obj.item)
                        }
                        is PictureItem -> {
                            encoder.encodeSerializableElement(descriptor, 2, PictureItem.serializer(), obj.item)
                        }
                        else -> throw IllegalArgumentException("oneOfType must be one of TextItem, PictureItem")
                    }
                } finally {
                    encoder.endStructure(descriptor)
                }
            }
        }
    }

    @Serializable
    data class Message(
            @Optional
            @SerialId(1)
            val itemType: ItemType = ItemType.TEXT,
            @Optional
            @SerialId(2)
            val items: List<CompoundItem> = listOf()
    )
}