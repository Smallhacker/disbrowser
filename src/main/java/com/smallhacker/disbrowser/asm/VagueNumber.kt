package com.smallhacker.disbrowser.asm

@Suppress("DataClassPrivateConstructor")
data class VagueNumber private constructor(val value: Int, val certainty: Int){
    constructor() : this(0, 0)
    constructor(value: Int) : this(value, -1)

    fun withBits(value: Int) = VagueNumber(this.value or value, this.certainty or value)
    fun withoutBits(value: Int) = VagueNumber(this.value and value.inv(), this.certainty or value)

    val certain: Boolean
        get() = certainty == -1

    fun get(mask: Int): Int? {
        if ((certainty and mask) != mask) {
            return null
        }
        return value and mask
    }

    fun getBoolean(mask: Int): Boolean? {
        val value = get(mask) ?: return null
        return value == mask
    }

    override fun toString(): String {
        var i = 1 shl 31
        val out = StringBuilder()
        while (i != 0) {
            val b = getBoolean(i)
            when (b) {
                true -> out.append('1')
                false -> out.append('0')
                null -> out.append('?')
            }
            i = i ushr 1
        }
        return out.toString()
    }
}