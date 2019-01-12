package com.smallhacker.disbrowser.asm

import com.smallhacker.disbrowser.util.*

interface CodeUnit {
    val address: SnesAddress?
    val relativeAddress: SnesAddress
    val presentedAddress: SnesAddress
    val nextPresentedAddress: SnesAddress

    val linkedState: State?
    val preState: State?
    val postState: State?

    val bytes: ValidMemorySpace
    val opcode: Opcode
    val lengthSuffix: String?

    fun operandByte(index: UInt): UByte = bytes[opcode.operandIndex + index]
    fun printOpcodeAndSuffix(): String {
        val mnemonic = opcode.mnemonic.displayName
        val suffix = lengthSuffix ?: ""
        return "$mnemonic$suffix"
    }
    fun printAlternativeOpcodeAndSuffix(): String? {
        val mnemonic = opcode.mnemonic.alternativeName ?: return null
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
        override val bytes: ValidMemorySpace,
        override val presentedAddress: SnesAddress,
        override val relativeAddress: SnesAddress,
        override val linkedState: State?
) : CodeUnit {
    override val nextPresentedAddress: SnesAddress
        get() = presentedAddress + operandLength.toInt()
    override val operandLength get() = bytes.size

    override val address: SnesAddress? = null
    override val preState: State? = null
    override val postState: State? = null
    override val lengthSuffix: String? = null
}

class Instruction(override val bytes: ValidMemorySpace, override val opcode: Opcode, override val preState: State) : CodeUnit {
    override val address: SnesAddress get() = preState.address
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

    private fun link(): SnesAddress? {
        if (!opcode.link) {
            return null
        }

        return referencedAddress()

//        return when (opcode.mode) {
//            Mode.ABSOLUTE_CODE -> preState.resolveAbsoluteCode(word)
//            Mode.ABSOLUTE_LONG -> SnesAddress(long)
//            Mode.RELATIVE -> relativeAddress + 2 + signedByte.toInt()
//            Mode.RELATIVE_LONG -> relativeAddress + 3 + signedWord.toInt()
//            Mode.CODE_WORD -> preState.resolveAbsoluteCode(word)
//            Mode.CODE_LONG -> SnesAddress(long)
//            Mode.DATA_WORD -> preState.resolveAbsoluteData(word)
//            Mode.DATA_LONG -> SnesAddress(long)
//            else -> null
//        }
    }

    private fun referencedAddress(): SnesAddress? {
        return when (opcode.mode) {
            Mode.ABSOLUTE -> preState.resolveAbsoluteData(word)
            Mode.ABSOLUTE_CODE -> preState.resolveAbsoluteCode(word)
            Mode.ABSOLUTE_INDIRECT -> preState.resolveAbsoluteData(word)
            Mode.ABSOLUTE_INDIRECT_LONG -> preState.resolveAbsoluteData(word)
            Mode.ABSOLUTE_LONG -> SnesAddress(long)
            Mode.ABSOLUTE_LONG_X -> SnesAddress(long)
            Mode.ABSOLUTE_X -> preState.resolveAbsoluteData(word)
            Mode.ABSOLUTE_X_INDIRECT -> preState.resolveAbsoluteData(word)
            Mode.ABSOLUTE_Y -> preState.resolveAbsoluteData(word)
            Mode.BLOCK_MOVE -> null
            Mode.CODE_WORD -> preState.resolveAbsoluteCode(word)
            Mode.CODE_LONG -> SnesAddress(long)
            Mode.DATA_BYTE -> null
            Mode.DATA_WORD -> preState.resolveAbsoluteData(word)
            Mode.DATA_LONG -> SnesAddress(long)
            Mode.DIRECT -> preState.resolveDirectPage(byte)
            Mode.DIRECT_X -> preState.resolveDirectPage(byte)
            Mode.DIRECT_Y -> preState.resolveDirectPage(byte)
            Mode.DIRECT_S -> null
            Mode.DIRECT_INDIRECT -> preState.resolveDirectPage(byte)
            Mode.DIRECT_INDIRECT_Y -> preState.resolveDirectPage(byte)
            Mode.DIRECT_X_INDIRECT -> preState.resolveDirectPage(byte)
            Mode.DIRECT_S_INDIRECT_Y -> null
            Mode.DIRECT_INDIRECT_LONG -> preState.resolveDirectPage(byte)
            Mode.DIRECT_INDIRECT_LONG_Y -> preState.resolveDirectPage(byte)
            Mode.IMMEDIATE_8 -> null
            Mode.IMMEDIATE_16 -> null
            Mode.IMMEDIATE_M -> null
            Mode.IMMEDIATE_X -> null
            Mode.IMPLIED -> null
            Mode.RELATIVE -> relativeAddress + 2 + signedByte.toInt()
            Mode.RELATIVE_LONG -> relativeAddress + 3 + signedWord.toInt()
        }
    }

    override fun toString(): String {
        return "$address ${bytesToString()} ${opcode.mnemonic.displayName} ${opcode.mode.print(this).padEnd(100, ' ')} ($preState -> $postState)"
    }
}