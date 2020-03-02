package io.landerlyoung.github.kproto.performance.serialtest.kotlin

import kotlinx.serialization.Serializable

/**
 * <pre>
 * Author: landerlyoung@gmail.com
 * Date:   2019-01-15
 * Time:   19:56
 * Life with Passion, Code with Creativity.
 * </pre>
 */

@Serializable
class G<T>(
        val t: T? = null
)

fun test() {
    G<Kotlin.ItemType>()

    G.serializer(Kotlin.TextItem.serializer())
}