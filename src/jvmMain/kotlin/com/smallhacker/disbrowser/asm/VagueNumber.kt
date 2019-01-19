package com.smallhacker.disbrowser.asm

inline class VagueNumber(private val valueAndCertainty: ULong) {
    private constructor(value: UInt, certainty: UInt) : this(value.toULong() or (certainty.toULong() shl 32))
    constructor(value: UInt) : this(value, 0xFFFF_FFFFu)
    constructor() : this(0u, 0u)

    val value get() = valueAndCertainty.toUInt()
    val certainty get() = (valueAndCertainty shr 32).toUInt()

    fun withBits(value: UInt) = VagueNumber(this.value or value, this.certainty or value)
    fun withoutBits(value: UInt) = VagueNumber(this.value and value.inv(), this.certainty or value)

    val certain: Boolean
        get() = certainty == 0xFFFF_FFFFu

    fun get(mask: UInt): UInt? {
        if ((certainty and mask) != mask) {
            return null
        }
        return value and mask
    }

    fun getBoolean(mask: UInt): Boolean? {
        val value = get(mask) ?: return null
        return value == mask
    }

    override fun toString(): String {
        var i = 1u shl 31
        val out = StringBuilder()
        while (i != 0u) {
            val b = getBoolean(i)
            when (b) {
                true -> out.append('1')
                false -> out.append('0')
                null -> out.append('?')
            }
            i = i shr 1
        }
        return out.toString()
    }
}