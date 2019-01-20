package com.smallhacker.util

// The Kotlin standard library does not contain a counterpart to Java's ArrayDeque<E>, so let's implement a simplistic
// one ourselves for portability.

interface ItemQueue<E> : MutableCollection<E> {
    fun removeNext(): E
}

class LifoQueue<E> private constructor(private val list: MutableList<E>) : ItemQueue<E>, MutableCollection<E> by list {
    constructor() : this(ArrayList())

    override fun removeNext(): E = list.removeAt(0)
}