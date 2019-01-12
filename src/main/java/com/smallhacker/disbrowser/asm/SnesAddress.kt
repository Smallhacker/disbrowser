package com.smallhacker.disbrowser.asm

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.smallhacker.disbrowser.util.UInt24
import com.smallhacker.disbrowser.util.toUInt24
import com.smallhacker.disbrowser.util.tryParseInt

data class SnesAddress(val value: UInt24) : Comparable<SnesAddress> {
    operator fun plus(offset: Int) = SnesAddress(value + offset)
    operator fun minus(offset: Int) = SnesAddress(value - offset)
    operator fun inc() = plus(1)
    operator fun dec() = minus(1)

    override fun toString(): String = toFormattedString()
    fun toFormattedString(): String = String.format("$%02x:%04x", (value shr 16).toInt(), (value and 0xFFFFu).toInt())
    @JsonValue
    fun toSimpleString(): String = String.format("%06x", value.toInt())

    fun withinBank(value: UShort): SnesAddress = SnesAddress((this.value and 0xFF_0000u) or value.toUInt24())

    override fun compareTo(other: SnesAddress) = value.toUInt().compareTo(other.value.toUInt())

    infix fun distanceTo(other: SnesAddress) = Math.abs(value.toInt() - other.value.toInt()).toUInt()

    companion object {
        @JvmStatic
        @JsonCreator
        fun parse(address: String): SnesAddress? = tryParseInt(address, 16)
                ?.let { SnesAddress(it.toUInt24()) }
    }
}

fun address(snesAddress: Int) = SnesAddress(snesAddress.toUInt24())