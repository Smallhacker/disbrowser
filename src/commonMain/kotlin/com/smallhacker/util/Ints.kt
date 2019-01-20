package com.smallhacker.util

fun toIntOrNull(s: String, radix: Int = 10) = try {
    s.toInt(radix)
} catch (e: NumberFormatException) {
    null
}