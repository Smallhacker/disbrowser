package com.smallhacker.disbrowser.asm

import com.smallhacker.disbrowser.util.*

interface CodeUnit {
    val address: Address?
    val relativeAddress: Address
    val presentedAddress: Address
    val nextPresentedAddress: Address

    val linkedState: State?
    val preState: State?
    val postState: State?

    val bytes: RomData
    val opcode: Opcode
    val lengthSuffix: String?

    fun operandByte(index: UInt): UByte = bytes[opcode.operandIndex + index]
    fun printOpcodeAndSuffix(): String {
        val mnemonic = opcode.mnemonic.displayName
        val suffix = lengthSuffix ?: ""
        return "$mnemonic$suffix"
    }
    fun printOperands() = opcode.mode.print(this)

    fun bytesToString(): String {
        return bytes.asSequence()
                .map { toHex(it.toUInt(), 1u) }
                .joinToString(" ")
                .padEnd(11, ' ')
    }

    val operandLength: UInt?

    val signedByte get() = byte.toByte()

    val signedWord get() = word.toShort()

    val byte get() = operandByte(0u)

    val byte2 get() = operandByte(1u)

    val word get() = joinBytes(operandByte(0u), operandByte(1u)).toUShort()

    val long get() = joinBytes(operandByte(0u), operandByte(1u), operandByte(2u)).toUInt24()

    val value
        get() = when (operandLength?.toInt()) {
            0 -> 0u
            1 -> byte.toUInt()
            2 -> word.toUInt()
            3 -> long.toUInt()
            else -> null
        }
}

class DataBlock(
        override val opcode: Opcode,
        override val bytes: RomData,
        override val presentedAddress: Address,
        override val relativeAddress: Address,
        override val linkedState: State?
) : CodeUnit {
    override val nextPresentedAddress: Address
        get() = presentedAddress + operandLength.toInt()
    override val operandLength get() = bytes.size

    override val address: Address? = null
    override val preState: State? = null
    override val postState: State? = null
    override val lengthSuffix: String? = null
}

class Instruction(override val bytes: RomData, override val opcode: Opcode, override val preState: State) : CodeUnit {
    override val address: Address get() = preState.address
    override val relativeAddress get() = address
    override val presentedAddress get() = address
    override val nextPresentedAddress get() = postState.address

    override val postState = opcode.mutate(this)
            .mutateAddress { it + bytes.size.toInt() }
            .withOrigin(this)
    override val linkedState = link()?.let { link ->
        opcode.mutate(this)
                .mutateAddress { link }
                .withOrigin(this)
    }

    override val lengthSuffix: String?
        get() {
            if (!opcode.mode.showLengthSuffix) {
                return null
            }

            return when (operandLength?.toInt()) {
                null -> ".?"
                1 -> ".b"
                2 -> ".w"
                3 -> ".l"
                else -> null
            }
        }

    override val operandLength
        get() = opcode.mode.operandLength(preState)

    private fun link(): Address? {
        if (!opcode.link) {
            return null
        }

        return when (opcode.mode) {
            Mode.ABSOLUTE -> relativeAddress.withinBank(word)
            Mode.ABSOLUTE_LONG -> Address(long)
            Mode.RELATIVE -> relativeAddress + 2 + signedByte.toInt()
            Mode.RELATIVE_LONG -> relativeAddress + 3 + signedWord.toInt()
            Mode.DATA_WORD -> relativeAddress.withinBank(word)
            Mode.DATA_LONG -> Address(long)
            else -> null
        }
    }

    override fun toString(): String {
        return "$address ${bytesToString()} ${opcode.mnemonic.displayName} ${opcode.mode.print(this).padEnd(100, ' ')} ($preState -> $postState)"
    }
}
