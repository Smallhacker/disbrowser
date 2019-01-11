package com.smallhacker.disbrowser.asm

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import com.smallhacker.disbrowser.util.UInt24
import com.smallhacker.disbrowser.util.toUInt24

data class Address(@JsonValue val value: UInt24): Comparable<Address> {
    @JsonIgnore
    val rom = (value and 0x8000u).toUInt() == 0u
    @JsonIgnore
    val pc = snesToPc(value)

    operator fun plus(offset: Int) = Address(value + offset)
    operator fun minus(offset: Int) = Address(value - offset)
    operator fun inc() = plus(1)
    operator fun dec() = minus(1)

    override fun toString(): String = toFormattedString()
    fun toFormattedString(): String = String.format("$%02x:%04x", (value shr 16).toInt(), (value and 0xFFFFu).toInt())
    fun toSimpleString(): String = String.format("%06x", value.toInt())

    fun withinBank(value: UShort): Address = Address((this.value and 0xFF_0000u) or value.toUInt24())

    override fun compareTo(other: Address) = value.toUInt().compareTo(other.value.toUInt())

    infix fun distanceTo(other: Address)= Math.abs(value.toInt() - other.value.toInt()).toUInt()
}

fun address(snesAddress: Int) = Address(snesAddress.toUInt24())

private fun snesToPc(value: UInt24): UInt {
    // TODO: This is incredibly oversimplified. Anything that isn't a small LoROM will crash and burn
    val intVal = value.toUInt()
    return (intVal and 0x7FFFu) or ((intVal and 0x7F_0000u) shr 1)
}
