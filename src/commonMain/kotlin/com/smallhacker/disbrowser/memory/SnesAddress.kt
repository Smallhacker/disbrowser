package com.smallhacker.disbrowser.memory

import com.smallhacker.util.UInt24
import com.smallhacker.util.toHex
import com.smallhacker.util.toIntOrNull
import com.smallhacker.util.toUInt24
import kotlin.math.abs

data class SnesAddress(val value: UInt24) : Comparable<SnesAddress> {
    operator fun plus(offset: Int) = SnesAddress(value + offset)
    operator fun minus(offset: Int) = SnesAddress(value - offset)
    operator fun inc() = plus(1)
    operator fun dec() = minus(1)

    override fun toString(): String = toFormattedString()
    fun toFormattedString(): String = "$${(value shr 16).toUByte().toHex()}:${value.toUShort().toHex()}"
    fun toSimpleString(): String = value.toHex()

    fun withinBank(value: UShort): SnesAddress = SnesAddress((this.value and 0xFF_0000u) or value.toUInt24())

    override fun compareTo(other: SnesAddress) = value.toUInt().compareTo(other.value.toUInt())

    infix fun distanceTo(other: SnesAddress) = abs(value.toInt() - other.value.toInt()).toUInt()

    companion object {
        fun parse(address: String): SnesAddress? = toIntOrNull(address, 16)
                ?.let { SnesAddress(it.toUInt24()) }
    }
}

fun address(snesAddress: Int) = SnesAddress(snesAddress.toUInt24())