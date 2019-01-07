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
