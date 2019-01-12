package com.smallhacker.disbrowser.datatype

import com.smallhacker.disbrowser.util.asReverseSequence

interface RangeMap<K : Comparable<K>, R : ClosedRange<K>, V : Any> {
    operator fun get(key: K): V?
}


interface MutableRangeMap<K : Comparable<K>, R : ClosedRange<K>, V: Any> : RangeMap<K, R, V> {
    operator fun set(keyRange: R, value: V): RangeMap<K, R, V>
}

class NaiveRangeMap<K : Comparable<K>, R : ClosedRange<K>, V: Any> : MutableRangeMap<K, R, V> {
    private val entries = ArrayList<Pair<R, V>>()

    override fun get(key: K) = entries
            .asReverseSequence()
            .filter { it.first.contains(key) }
            .map { it.second }
            .firstOrNull()

    override fun set(keyRange: R, value: V): RangeMap<K, R, V> {
        entries += keyRange to value
        return this
    }
}