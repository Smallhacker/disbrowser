package com.smallhacker.disbrowser.util

inline fun <reified T> T.equalsBy(other: Any?, vararg values: T.() -> Any?) = when (other) {
    !is T -> false
    else -> values.asSequence()
            .map { it(this) to it(other) }
            .all { it.first == it.second }
}

fun tryParseInt(s: String, radix: Int = 10): Int? {
    return try {
        Integer.parseInt(s, radix)
    } catch (e: NumberFormatException) {
        null
    }
}

fun toHex(value: UInt, bytes: UInt): String {
    if (bytes == 0u) {
        return ""
    }

    val digits = 2u * bytes
    val pattern = "%0${digits}x"
    return String.format(pattern, value.toInt())
}

fun joinBytes(vararg bytes: UByte) = bytes
        .asSequence()
        .mapIndexed { index, v -> v.toUInt() shl (index * 8) }
        .reduce { a, b -> a or b }

fun joinNullableBytes(vararg bytes: UByte?): UInt? {
    if (bytes.any { it == null }) {
        return null
    }

    return bytes
            .asSequence()
            .mapIndexed { index, v -> v!!.toUInt() shl (index * 8) }
            .reduce { a, b -> a or b }
}

inline class UInt24(private val data: UInt) {
    fun toUInt() = data and 0x00FF_FFFFu
    fun toUShort() = toUInt().toUShort()
    fun toUByte() = toUInt().toUByte()
    fun toInt() = toUInt().toInt()
    fun toShort() = toUShort().toShort()
    fun toByte() = toUByte().toByte()

    infix fun and(v: UInt24) = (data and v.data).toUInt24()
    infix fun and(v: UInt) = (data and v).toUInt24()
    infix fun or(v: UInt24) = (data or v.data).toUInt24()
    infix fun or(v: UInt) = (data or v).toUInt24()
    infix fun shl(v: Int) = (data shl v).toUInt24()
    infix fun shr(v: Int) = (toUInt() shr v).toUInt24()

    operator fun plus(v: UInt24) = (toUInt() + v.toUInt()).toUInt24()
    operator fun plus(v: UInt) = (toUInt() + v).toUInt24()
    operator fun plus(v: Int) = (toInt() + v).toUInt24()
    operator fun minus(v: UInt24) = (toUInt() - v.toUInt()).toUInt24()
    operator fun minus(v: UInt) = (toUInt() - v).toUInt24()
    operator fun minus(v: Int) = (toInt() - v).toUInt24()

    override fun toString() = data.toString()
}

fun UInt.toUInt24() = UInt24(this and 0x00FF_FFFFu)
fun UShort.toUInt24() = this.toUInt().toUInt24()
fun UByte.toUInt24() = this.toUInt().toUInt24()
fun Int.toUInt24() = this.toUInt().toUInt24()
fun Short.toUInt24() = this.toUInt().toUInt24()
fun Byte.toUInt24() = this.toUInt().toUInt24()

fun <T> List<T>.asReverseSequence(): Sequence<T> =
        ((size - 1) downTo 0).asSequence().map { this[it] }