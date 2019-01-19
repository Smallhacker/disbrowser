package com.smallhacker.disbrowser.util

abstract class UType(val bytes: Int, val name: String) {
    val mask = (1 shl (bytes * 8)) - 1
}

class UVal<U : UType>(value: Int, val type: U) : Comparable<UVal<U>> {
    val value = value and type.mask

    override fun equals(other: Any?) = equalsBy(other, { value }, { type })

    override fun toString() = "${type.name}(${toHex(true)})"

    fun toHex(prefix: Boolean = false): String {
        val digits = 2 * type.bytes
        val start = if (prefix) "0x" else ""
        val pattern = "%0${digits}x"
        return start + String.format(pattern, value)
    }

    override fun compareTo(other: UVal<U>): Int = Integer.compare(value, other.value)

    override fun hashCode() = hashOf(value, type)

    //fun value(value: Int) = UVal(value, type)
    //fun mutate(mutator: (Int) -> Int) = UVal(mutator(value), type)

    object U1 : UType(1, "OldUByte")
    object U2 : UType(2, "OldUWord")
    object U3 : UType(3, "OldULong")
}

inline fun <reified U: UVal<*>> U.value(value: Int): U = UVal(value, type) as U
inline infix fun <reified U: UVal<*>> U.mutate(mutator: (Int) -> Int): U = value(mutator(value))

typealias OldUByte = UVal<UVal.U1>
typealias OldUWord = UVal<UVal.U2>
typealias OldULong = UVal<UVal.U3>

fun UVal<*>.toByte() = uByte(value)
fun UVal<*>.toWord() = uWord(value)
fun UVal<*>.toLong() = uLong(value)

private val UBYTE_CACHE = Array(256) { OldUByte(it, UVal.U1) }

fun uByte(value: Int): OldUByte = UBYTE_CACHE[value and 0xFF]
fun uWord(value: Int): OldUWord = UVal(value, UVal.U2)
fun uLong(value: Int): OldULong = UVal(value, UVal.U3)

inline infix fun <reified U: UVal<V>, V: UType> U.left(count: Int): U = mutate { it shl count }
inline infix fun <reified U: UVal<V>, V: UType> U.right(count: Int): U = mutate { it ushr count }
inline infix fun <reified U: UVal<V>, V: UType> U.and(other: U): U = mutate { it and other.value }
inline infix fun <reified U: UVal<V>, V: UType> U.or(other: U): U = mutate { it or other.value }
inline operator fun <reified U: UVal<*>> U.not(): U = mutate { it.inv() }

inline infix operator fun <reified U: UVal<V>, V: UType> U.plus(other: U): U = mutate { it + other.value }
inline infix operator fun <reified U: UVal<V>, V: UType> U.minus(other: U): U = mutate { it - other.value }
