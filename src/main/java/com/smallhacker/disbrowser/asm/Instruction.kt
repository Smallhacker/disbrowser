package com.smallhacker.disbrowser.asm

import com.smallhacker.disbrowser.game.GameData
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

    val memory: SnesMemory

    fun operandByte(index: UInt): UByte = bytes[opcode.operandIndex + index]

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
        override val linkedState: State?,
        override val memory: SnesMemory
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
    override val memory = preState.memory
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

    private val showLengthSuffix get() = opcode.mode.showLengthSuffix and opcode.mnemonic.showLengthSuffix

    override val lengthSuffix: String?
        get() {
            if (!showLengthSuffix) {
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
    }

    private fun referencedAddress() = opcode.mode.referencedAddress(this)

    override fun toString(): String {
        val (address, bytes, _, primaryMnemonic, _, suffix, operands, _, _) = print()
        return "${address ?: "\$xx:xxxx"} $bytes $primaryMnemonic${suffix ?: ""} ${operands?.padEnd(100, ' ')
                ?: ""} ($preState -> $postState)"
    }
}

fun CodeUnit.print(gameData: GameData? = null): PrintedCodeUnit {
    val mnemonic = opcode.mnemonic
    val primaryMnemonic = mnemonic.displayName
    val secondaryMnemonic = mnemonic.alternativeName

    var suffix = lengthSuffix
    var operands = gameData?.let { opcode.mode.printWithLabel(this, it) }
    if (operands == null) {
        operands = opcode.mode.printRaw(this)
        suffix = null
    }

    val state = postState?.toString()
    val label = address?.let { gameData?.get(it)?.label }
    val comment = address?.let { gameData?.get(it)?.comment }
    val formattedAddress = address?.toFormattedString()
    val bytes = bytesToString()

    val labelAddress = if (opcode.mode.canHaveLabel) {
        opcode.mode.referencedAddress(this)?.let {
            memory.toCanonical(it)
        }
    } else {
        null
    }

    return PrintedCodeUnit(formattedAddress, bytes, label, primaryMnemonic, secondaryMnemonic, suffix, operands, state, comment, labelAddress)
}

data class PrintedCodeUnit(
        val address: String?,
        val bytes: String,
        val label: String?,
        val primaryMnemonic: String,
        val secondaryMnemonic: String?,
        val suffix: String?,
        val operands: String?,
        val state: String?,
        val comment: String?,
        val labelAddress: SnesAddress?
)