package com.smallhacker.disbrowser.asm

data class Address(val value: Int): Comparable<Address> {
    val rom = (value and 0x8000) == 0
    val pc = (value and 0x7FFF) or ((value and 0x7F_0000) shr 1)

    operator fun plus(offset: Int) = Address(value + offset)
    operator fun minus(offset: Int) = Address(value - offset)
    operator fun inc() = plus(1)
    operator fun dec() = plus(1)

    override fun toString(): String = toFormattedString()
    fun toFormattedString(): String = String.format("$%02x:%04x", value shr 16, value and 0xFFFF)
    fun toSimpleString(): String = String.format("%06x", value)

    fun withinBank(value: Int): Address = Address((this.value and 0xFF_0000) or value)

    override fun compareTo(other: Address) = value.compareTo(other.value)
}
