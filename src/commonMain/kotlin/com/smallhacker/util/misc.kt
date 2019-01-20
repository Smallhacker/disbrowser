package com.smallhacker.util

fun toHex(value: UInt, digits: UInt): String {
    if (digits == 0u) {
        return ""
    }

    val hex = value.toLong().toString(16)
    return hex.padStart(digits.toInt(), '0')
}

fun UInt.toHex() = toHex(this, 8u)
fun UInt24.toHex() = toHex(toUInt(), 6u)
fun UShort.toHex() = toHex(toUInt(), 4u)
fun UByte.toHex() = toHex(toUInt(), 2u)

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

fun <T> List<T>.asReverseSequence(): Sequence<T> =
        ((size - 1) downTo 0).asSequence().map { this[it] }

fun <K, V> MutableMap<K, V>.removeIf(condition: (K, V) -> Boolean) {
    this.asSequence()
            .filter { condition(it.key, it.value) }
            .map { it.key }
            .toList()
            .forEach { remove(it) }
}

fun hashOf(vararg values: Any?): Int {
    return if (values.isEmpty()) {
        0
    } else {
        values.asSequence()
                .map { it.hashCode() }
                .reduce { acc, v -> (acc * 31) + v }
    }
}