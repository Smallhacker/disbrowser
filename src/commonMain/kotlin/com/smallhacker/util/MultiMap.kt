package com.smallhacker.util

typealias MultiMap<K, V> = Map<K, List<V>>
typealias MutableMultiMap<K, V> = MutableMap<K, MutableList<V>>

fun <K, V> mutableMultiMap(): MutableMultiMap<K, V> = HashMap()

fun <K, V> MutableMultiMap<K, V>.putSingle(key: K, value: V) {
    getOrPut(key) { ArrayList() }.add(value)
}