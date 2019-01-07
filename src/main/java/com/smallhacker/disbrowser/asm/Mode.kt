package com.smallhacker.disbrowser.asm

import com.smallhacker.disbrowser.util.*

fun format(format: String, value: UVal<*>): String {
    return format.replace(Regex("[0]+"), value.toHex())
}

enum class Mode {
    DATA_BYTE(1, "$00", { dataByte }, dataMode = true),
    DATA_WORD(2, "$0000", { dataWord }, dataMode = true),
    DATA_LONG(3, "$000000", { dataLong }, dataMode = true),

    IMPLIED(1, { "" }),
    IMMEDIATE_8(2, "#$00", { byte }),
    IMMEDIATE_16(3, "#$0000", { word }),
    IMMEDIATE_M(-1, {
        when (preState.m) {
            null -> "????"
            true -> format("#$00", byte)
            false -> format("#$0000", word)
        }
    }),
    IMMEDIATE_X(-2, {
        when (preState.x) {
            null -> "???"
            true -> format("#$00", byte)
            false -> format("#$0000", word)
        }
    }),
    ABSOLUTE(3, "$0000", { word }),
    ABSOLUTE_X(3, "$0000,x", { word }),
    ABSOLUTE_Y(3, "$0000,y", { word }),
    ABSOLUTE_LONG(4, "$000000", { long }),
    ABSOLUTE_LONG_X(4, "$000000,x", { long }),
    ABSOLUTE_INDIRECT(3, "($0000)", { word }),
    ABSOLUTE_INDIRECT_LONG(3, "[$0000]", { word }),
    ABSOLUTE_X_INDIRECT(3, "($0000,x)", { word }),
    DIRECT(2, "$00", { byte }),
    DIRECT_X(2, "$00,x", { byte }),
    DIRECT_Y(2, "$00,y", { byte }),
    DIRECT_S(2, "$00,s", { byte }),
    DIRECT_INDIRECT(2, "($00)", { byte }),
    DIRECT_INDIRECT_Y(2, "($00),y", { byte }),
    DIRECT_X_INDIRECT(2, "($00,x)", { byte }),
    DIRECT_S_INDIRECT_Y(2, "($00,s),y", { byte }),
    DIRECT_INDIRECT_LONG(2, "[$00]", { byte }),
    DIRECT_INDIRECT_LONG_Y(2, "[$00],y", { byte }),
    RELATIVE(2, {
        val rel = signedByte.toInt() + 2
        format("$000000", uLong((address + rel).value))
    }),
    //RELATIVE_LONG(3, "$0000", { word }),
    RELATIVE_LONG(3, {
        val rel = signedWord.toInt() + 3
        format("$000000", uLong((address + rel).value))
    }),
    BLOCK_MOVE(3, { String.format("#$%02x,#$%02x", byte.value, byte2.value) })
    ;

    private val length: Int
    val print: Instruction.() -> String
    val dataMode: Boolean

    constructor(length: Int, print: Instruction.() -> String) {
        this.length = length
        this.print = print
        this.dataMode = false
    }

    constructor(length: Int, format: String, valueGetter: Instruction.() -> UVal<*>, dataMode: Boolean = false) {
        this.length = length
        this.print = { format(format, valueGetter(this)) }
        this.dataMode = dataMode
    }

    /**
     * Returns the total length, in bytes, of an instruction of this mode and its operands.
     *
     * This is usually one greater than [operandLength], except in the cases when the instruction is just pure data
     * without an opcode (in which case the two are equal).
     *
     * If the length cannot be determined based on the current [State], `null` is returned.
     */
    fun instructionLength(state: State): Int? {
        return when (length) {
            -1 -> state.mWidth?.plus(1)
            -2 -> state.xWidth?.plus(1)
            else -> length
        }
    }

    /**
     * Returns the length, in bytes, of the operands of an instruction of this mode.
     *
     * This is usually one less than [operandLength], except in the cases when the instruction is just pure data
     * without an opcode (in which case the two are equal).
     *
     * If the length cannot be determined based on the current [State], `null` is returned.
     */
    fun operandLength(state: State): Int? {
        val len = instructionLength(state) ?: return null

        return when(this) {
            DATA_BYTE,
            DATA_WORD,
            DATA_LONG -> len
            else -> len - 1
        }
    }
}
