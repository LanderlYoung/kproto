package io.landerlyoung.github.kproto.performance.serialtest

import io.landerlyoung.github.kproto.performance.serialtest.javalite.Proto
import io.landerlyoung.github.kproto.performance.serialtest.wire.*

/**
 * <pre>
 * Author: landerlyoung@gmail.com
 * Date:   2019-01-02
 * Time:   18:21
 * Life with Passion, Code with Creativity.
 * </pre>
 */

fun ItemType.eq(p: Proto.ItemType?): Boolean {
    return when (this) {
        ItemType.TEXT -> p == Proto.ItemType.TEXT
        ItemType.PICTURE -> p == Proto.ItemType.PICTURE
        ItemType.BINARY -> p == Proto.ItemType.BINARY
    }
}

fun TextItem.eq(p: Proto.TextItem?): Boolean =
        p != null && this.text == p.text

fun PictureItem.eq(p: Proto.PictureItem?): Boolean =
        p != null && this.pictureUrl == p.pictureUrl

fun CompoundItem.eq(p: Proto.CompoundItem?): Boolean =
        p != null && if (this.text != null) {
            this.text.eq(p.text)
        } else {
            this.picture.eq(p.picture)
        }

fun Message.eq(p: Proto.Message?): Boolean =
        p != null && this.itemType.eq(p.itemType) &&
                this.items.zip(p.itemsList).all { (first, second) ->
                    first.eq(second)
                }


