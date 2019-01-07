package com.smallhacker.disbrowser.asm

import com.smallhacker.disbrowser.util.*

class Instruction(val bytes: RomData, val opcode: Opcode, val preState: State) {
    val address: Address
        get() = preState.address

    val postState = opcode.mutate(this)
            .mutateAddress { it + bytes.size }
            .withOrigin(this)
    val linkedState = link()?.let { link ->
        opcode.mutate(this)
                .mutateAddress { link }
                .withOrigin(this)
    }

    operator fun get(index: Int): UByte {
        return bytes[index]
    }

    val signedByte: Int
        get() = byte.value.toByte().toInt()

    val signedWord: Int
        get() = word.value.toShort().toInt()

    val byte: UByte
        get() = get(1)

    val byte2: UByte
        get() = get(2)

    val dataByte: UByte
        get() = get(0)

    val word: UWord
        get() = (get(2).toWord() left 8) or get(1).toWord()

    val dataWord: UWord
        get() = (get(1).toWord() left 8) or get(0).toWord()

    val long: ULong
        get() = (get(3).toLong() left 16) or (get(2).toLong() left 8) or get(1).toLong()

    val dataLong: ULong
        get() = (get(2).toLong() left 16) or (get(1).toLong() left 8) or get(0).toLong()

    fun bytesToString(): String {
        return bytes.asSequence().map { it.toHex() }.joinToString(" ").padEnd(11, ' ')
    }

    //val value: ULong
    //    get() {
    //        val len = operandLength
    //        val start = opcode.operandIndex

    //}

    //fun getOperand(index: Int, length: Int) {
    //    val v = uLong(0)
    //    for (i in (index + length) downTo index)
    //}

    val lengthSuffix: String
        get() {
            return when (opcode.mode) {
                Mode.IMPLIED -> ""
                Mode.IMMEDIATE_8 -> ""
                Mode.IMMEDIATE_16 -> ""
                Mode.RELATIVE -> ""
                Mode.RELATIVE_LONG -> ""
                Mode.BLOCK_MOVE -> ""
                else -> when (operandLength) {
                    null -> ".?"
                    1 -> ".b"
                    2 -> ".w"
                    3 -> ".l"
                    else -> ""
                }
            }
        }

    private val operandLength
        get() = opcode.mode.operandLength(preState)

    private fun link(): Address? {
        if (!opcode.link) {
            return null
        }

        return when (opcode.mode) {
            Mode.ABSOLUTE -> address.withinBank(word.value)
            Mode.ABSOLUTE_LONG -> Address(long.value)
            Mode.RELATIVE -> address + 2 + signedByte
            Mode.RELATIVE_LONG -> address + 3 + signedWord
            Mode.DATA_WORD -> address.withinBank(dataWord.value)
            Mode.DATA_LONG -> Address(dataLong.value)
            else -> null
        }
    }

    override fun toString(): String {
        return "$address ${bytesToString()} ${opcode.mnemonic} ${opcode.mode.print(this).padEnd(100, ' ')} ($preState -> $postState)"
    }
}
