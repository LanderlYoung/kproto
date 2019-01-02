package com.squareup.wire.schema.internal

/**
 * <pre>
 * Author: landerlyoung@gmail.com
 * Date:   2018-12-31
 * Time:   19:16
 * Life with Passion, Code with Creativity.
 * </pre>
 */
internal class Multimap<Key, Value>
private constructor(private val _map: LinkedHashMap<Key, HashSet<Value>>)
    : Map<Key, HashSet<Value>> by _map {

    constructor() : this(_map = LinkedHashMap())

    override fun get(key: Key): HashSet<Value> =
            _map.getOrPut(key) { HashSet() }

    fun put(key: Key, value: Value) =
            get(key).add(value)

    fun putAll(key: Key, values: Iterable<Value>) {
        get(key).addAll(values)
    }

    fun containsEntry(key: Key, value: Value?): Boolean =
            _map[key]?.contains(value) == true
}