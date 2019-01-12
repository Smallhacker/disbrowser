package com.smallhacker.disbrowser.asm

import com.smallhacker.disbrowser.util.*

private val ZEROES = Regex("[0]+")
private fun countBytes(format: String) = (ZEROES.find(format)?.groupValues?.firstOrNull()?.length?.toUInt() ?: 0u) / 2u

private const val ACCUMULATOR_SIZE = -1
private const val INDEX_SIZE = -2

fun format(format: String, value: UInt, operandBytes: UInt = countBytes(format)) =
        format.replace(ZEROES, toHex(value, operandBytes))

enum class Mode {
    DATA_BYTE("$00", dataMode = true, showLengthSuffix = false),
    DATA_WORD("$0000", dataMode = true, showLengthSuffix = false),
    DATA_LONG("$000000", dataMode = true, showLengthSuffix = false),
    CODE_WORD("$0000", dataMode = true, showLengthSuffix = false),
    CODE_LONG("$000000", dataMode = true, showLengthSuffix = false),

    IMPLIED("", showLengthSuffix = false),
    IMMEDIATE_8("#$00", showLengthSuffix = false),
    IMMEDIATE_16("#$0000", showLengthSuffix = false),
    ABSOLUTE("$0000"),
    ABSOLUTE_CODE("$0000"),
    ABSOLUTE_X("$0000,x"),
    ABSOLUTE_Y("$0000,y"),
    ABSOLUTE_LONG("$000000"),
    ABSOLUTE_LONG_X("$000000,x"),
    ABSOLUTE_INDIRECT("($0000)"),
    ABSOLUTE_INDIRECT_LONG("[$0000]"),
    ABSOLUTE_X_INDIRECT("($0000,x)"),
    DIRECT("$00"),
    DIRECT_X("$00,x"),
    DIRECT_Y("$00,y"),
    DIRECT_S("$00,s"),
    DIRECT_INDIRECT("($00)"),
    DIRECT_INDIRECT_Y("($00),y"),
    DIRECT_X_INDIRECT("($00,x)"),
    DIRECT_S_INDIRECT_Y("($00,s),y"),
    DIRECT_INDIRECT_LONG("[$00]"),
    DIRECT_INDIRECT_LONG_Y("[$00],y"),

    IMMEDIATE_M(
            operandLength = ACCUMULATOR_SIZE,
            print = {
                when (preState?.m) {
                    null -> "????"
                    true -> format("#$00", byte.toUInt())
                    false -> format("#$0000", word.toUInt())
                }
            }
    ),
    IMMEDIATE_X(
            operandLength = INDEX_SIZE,
            print = {
                when (preState?.x) {
                    null -> "???"
                    true -> format("#$00", byte.toUInt())
                    false -> format("#$0000", word.toUInt())
                }
            }
    ),

    RELATIVE(
            format = "$000000",
            operandLength = 1u,
            valueGetter = {
                val rel = signedByte.toInt() + 2
                (relativeAddress + rel).value.toUInt()
            },
            showLengthSuffix = false
    ),
    RELATIVE_LONG(
            format = "$000000",
            operandLength = 2u,
            valueGetter = {
                val rel = signedWord.toInt() + 3
                (relativeAddress + rel).value.toUInt()
            },
            showLengthSuffix = false
    ),
    BLOCK_MOVE(
            operandLength = 2,
            print = { String.format("#$%02x,#$%02x", byte.toInt(), byte2.toInt()) },
            showLengthSuffix = false
    )
    ;

    private val operandLength: Int
    val print: CodeUnit.() -> String
    val dataMode: Boolean
    val showLengthSuffix: Boolean

    constructor(operandLength: Int, print: CodeUnit.() -> String, showLengthSuffix: Boolean = true) {
        this.operandLength = operandLength
        this.print = print
        this.dataMode = false
        this.showLengthSuffix = showLengthSuffix
    }

    constructor(
            format: String,
            printedLength: UInt = countBytes(format),
            operandLength: UInt = printedLength,
            valueGetter: CodeUnit.() -> UInt = { value!! },
            dataMode: Boolean = false,
            showLengthSuffix: Boolean = true
    ) {
        this.operandLength = operandLength.toInt()
        this.print = { format(format, valueGetter(this), printedLength) }
        this.dataMode = dataMode
        this.showLengthSuffix = showLengthSuffix
    }

    /**
     * Returns the total length, in bytes, of an instruction of this mode and its operands.
     *
     * This is usually one greater than [operandLength], except in the cases when the instruction is just pure data
     * without an opcode (in which case the two are equal).
     *
     * If the length cannot be determined based on the current [State], `null` is returned.
     */
    fun instructionLength(state: State): UInt? {
        val operatorLength = if (this.dataMode) 0u else 1u
        return operandLength(state)
                ?.plus(operatorLength)
    }

    /**
     * Returns the length, in bytes, of the operands of an instruction of this mode.
     *
     * This is usually one less than [operandLength], except in the cases when the instruction is just pure data
     * without an opcode (in which case the two are equal).
     *
     * If the length cannot be determined based on the current [State], `null` is returned.
     */
    fun operandLength(state: State): UInt? {
        return when (operandLength) {
            ACCUMULATOR_SIZE -> state.mWidth
            INDEX_SIZE -> state.xWidth
            else -> operandLength.toUInt()
        }
    }
}
