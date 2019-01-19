package com.smallhacker.disbrowser.asm

import com.smallhacker.disbrowser.game.GameData
import com.smallhacker.disbrowser.util.*

interface CodeUnit {
    val address: SnesAddress?
    val relativeAddress: SnesAddress
    val indicativeAddress: SnesAddress
    val sortedAddress: SnesAddress
    val nextSortedAddress: SnesAddress

    val linkedState: State?
    val preState: State?
    val postState: State?

    val bytes: ValidMemorySpace
    val opcode: Opcode
    val lengthSuffix: String?

    val memory: SnesMemory
    val certainty: Certainty

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
        override val address: SnesAddress?,
        override val indicativeAddress: SnesAddress,
        override val sortedAddress: SnesAddress,
        override val relativeAddress: SnesAddress,
        override val linkedState: State?,
        override val memory: SnesMemory,
        override val certainty: Certainty
) : CodeUnit {
    constructor(
            opcode: Opcode,
            bytes: ValidMemorySpace,
            indicativeAddress: SnesAddress,
            sortedAddress: SnesAddress,
            relativeAddress: SnesAddress,
            linkedState: State?,
            memory: SnesMemory,
            certainty: Certainty
    ) : this(opcode, bytes, null, indicativeAddress, sortedAddress, relativeAddress, linkedState, memory, certainty)

    constructor(
            opcode: Opcode,
            bytes: ValidMemorySpace,
            address: SnesAddress,
            relativeAddress: SnesAddress,
            linkedState: State?,
            memory: SnesMemory,
            certainty: Certainty
    ) : this(opcode, bytes, address, address, address, relativeAddress, linkedState, memory, certainty)

    override val nextSortedAddress: SnesAddress
        get() = sortedAddress + operandLength.toInt()
    override val operandLength get() = bytes.size

    override val preState: State? = null
    override val postState: State? = null
    override val lengthSuffix: String? = null
}

interface Instruction : CodeUnit {
    override val preState: State
    override val address: SnesAddress
    override val postState: State

    val continuation: Continuation
    val showLengthSuffix: Boolean
    fun link(): SnesAddress?
    fun referencedAddress(): SnesAddress?

    override fun toString(): String
}

class MutableInstruction(
        override val bytes: ValidMemorySpace,
        override val opcode: Opcode,
        override val preState: State,
        override var continuation: Continuation,
        override var certainty: Certainty
) : Instruction {
    override val memory = preState.memory
    override val address: SnesAddress get() = preState.address
    override val indicativeAddress get() = address
    override val relativeAddress get() = address
    override val sortedAddress get() = address
    override val nextSortedAddress get() = postState.address

    override val postState = opcode.mutate(this)
            .mutateAddress { it + bytes.size.toInt() }
            .withOrigin(this)
    override val linkedState = link()?.let { link ->
        opcode.mutate(this)
                .mutateAddress { link }
                .withOrigin(this)
    }

    override val showLengthSuffix get() = opcode.mode.showLengthSuffix and opcode.mnemonic.showLengthSuffix

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

    override fun link(): SnesAddress? {
        if (!opcode.link) {
            return null
        }

        return referencedAddress()
    }

    override fun referencedAddress() = opcode.mode.referencedAddress(this)

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
    val label = gameData?.get(indicativeAddress)?.label
    val comment = gameData?.get(indicativeAddress)?.comment
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