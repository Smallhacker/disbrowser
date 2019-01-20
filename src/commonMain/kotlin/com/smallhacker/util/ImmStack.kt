package com.smallhacker.util

interface ImmStack<E>: Iterable<E> {
    fun isEmpty(): Boolean

    val top: E?

    fun pop(): ImmStack<E>

    fun push(value: E): ImmStack<E> = ImmStackImpl(this, value)
}

fun <T> immStack(): ImmStack<T> {
    @Suppress("UNCHECKED_CAST")
    return EmptyImmStack as ImmStack<T>
}

private class ImmStackImpl<E>(private val parent: ImmStack<E>, override val top: E):
    ImmStack<E> {
    override fun isEmpty() = false

    override fun pop(): ImmStack<E> = parent

    override fun iterator(): Iterator<E> {
        return sequenceOf(top).plus(parent).iterator()
    }

}

private object EmptyImmStack: ImmStack<Any?> {
    override fun isEmpty() = true

    override val top: Any? = null

    override fun pop() = this

    override fun iterator() = emptySequence<Any?>().iterator()
}