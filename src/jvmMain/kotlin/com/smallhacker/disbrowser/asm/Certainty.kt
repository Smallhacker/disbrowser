package com.smallhacker.disbrowser.asm

inline class Certainty(val value: UInt) {
    operator fun minus(value: Int): Certainty {
        val signed = this.value.toInt() - value
        return if (signed < 0) {
            PROBABLY_WRONG
        } else Certainty(signed.toUInt())
    }

    companion object {
        val PROBABLY_CORRECT = Certainty(100u)
        val UNCERTAIN = Certainty(50u)
        val PROBABLY_WRONG = Certainty(0u)

    }
}