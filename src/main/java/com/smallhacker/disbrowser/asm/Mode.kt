package com.smallhacker.disbrowser.asm

import com.smallhacker.disbrowser.game.GameData
import com.smallhacker.disbrowser.util.*

interface Mode {
    val dataMode get() = false
    val showLengthSuffix get() = true
    val canHaveLabel: Boolean
    fun operandLength(state: State): UInt?
    fun printWithLabel(ins: CodeUnit, gameData: GameData): String? = referencedAddress(ins)?.let { gameData[it]?.label }
    fun printRaw(ins: CodeUnit): String
    fun referencedAddress(ins: CodeUnit): SnesAddress?
}

fun Mode.instructionLength(state: State): UInt? {
    val operatorLength = if (this.dataMode) 0u else 1u
    return operandLength(state)
            ?.plus(operatorLength)
}

val Mode.x: Mode get() = IndexXMode(this)
val Mode.y: Mode get() = IndexYMode(this)
val Mode.s: Mode get() = IndexSMode(this)
val Mode.indirect: Mode get() = IndirectMode(this)
val Mode.indirectLong: Mode get() = IndirectLongMode(this)

private abstract class RawWrappedMode(
        private val parent: Mode,
        private val prefix: String = "",
        private val suffix: String = ""
) : Mode by parent {
    override val canHaveLabel = false
    override fun printWithLabel(ins: CodeUnit, gameData: GameData): String? = printRaw(ins)
    override fun printRaw(ins: CodeUnit) = prefix + parent.printRaw(ins) + suffix
}

private abstract class WrappedMode(
        private val parent: Mode,
        private val prefix: String = "",
        private val suffix: String = ""
) : Mode by parent {
    override fun printWithLabel(ins: CodeUnit, gameData: GameData): String? = parent.printWithLabel(ins, gameData)?.let { prefix + it + suffix }
    override fun printRaw(ins: CodeUnit) = prefix + parent.printRaw(ins) + suffix
}

abstract class MultiMode(private val fallback: Mode, private vararg val options: Mode) : Mode {
    override val canHaveLabel = get{ canHaveLabel }
    override val dataMode = get { dataMode }
    override val showLengthSuffix = get { showLengthSuffix }

    override fun operandLength(state: State) = get(state) { operandLength(state) }

    override fun printWithLabel(ins: CodeUnit, gameData: GameData): String? = get(ins.preState) { printWithLabel(ins, gameData) }

    override fun printRaw(ins: CodeUnit) = get(ins.preState) { printRaw(ins) }

    override fun referencedAddress(ins: CodeUnit): SnesAddress? = get(ins.preState) { referencedAddress(ins) }

    protected abstract fun pickMode(state: State): UInt

    private fun <T> get(state: State?, getter: Mode.() -> T): T {
        if (state != null) {
            val mode = pickMode(state)
            if (mode != 0u) {
                return getter(options[mode.toInt() - 1])
            }
        }

        return getter(fallback)
    }

    private fun <T : Any> get(getter: Mode.() -> T) = get(null, getter)
}

private interface DataValueType {
    fun resolve(byte: UByte, state: State?, memory: SnesMemory): SnesAddress?
    fun resolve(word: UShort, state: State?, memory: SnesMemory): SnesAddress?
    fun resolve(long: UInt24, state: State?, memory: SnesMemory): SnesAddress?
    val canHaveLabel: Boolean

    val byte: Mode get() = DataByteMode(this)
    val word: Mode get() = DataWordMode(this)
    val long: Mode get() = DataLongMode(this)
}

object DirectData : DataValueType {
    override val canHaveLabel = false
    override fun resolve(byte: UByte, state: State?, memory: SnesMemory): SnesAddress? = null
    override fun resolve(word: UShort, state: State?, memory: SnesMemory): SnesAddress? = null
    override fun resolve(long: UInt24, state: State?, memory: SnesMemory): SnesAddress? = null
}

object DataPointer : DataValueType {
    override val canHaveLabel = true
    override fun resolve(byte: UByte, state: State?, memory: SnesMemory) = state?.resolveDirectPage(byte)
    override fun resolve(word: UShort, state: State?, memory: SnesMemory) = state?.resolveAbsoluteData(word)
    override fun resolve(long: UInt24, state: State?, memory: SnesMemory) = memory.toCanonical(SnesAddress(long))
}

object CodePointer : DataValueType {
    override val canHaveLabel = true
    override fun resolve(byte: UByte, state: State?, memory: SnesMemory) = state?.resolveDirectPage(byte)
    override fun resolve(word: UShort, state: State?, memory: SnesMemory) = state?.resolveAbsoluteCode(word)
    override fun resolve(long: UInt24, state: State?, memory: SnesMemory) = memory.toCanonical(SnesAddress(long))
}

private abstract class DataValueMode(private val length: UInt, protected val type: DataValueType) : Mode {
    override val canHaveLabel = type.canHaveLabel
    override val dataMode = true
    override val showLengthSuffix = false
    override fun operandLength(state: State) = length
}

private class DataByteMode(type: DataValueType) : DataValueMode(1u, type) {
    override fun printRaw(ins: CodeUnit) = "$" + toHex(ins.byte)
    override fun referencedAddress(ins: CodeUnit): SnesAddress? = type.resolve(ins.byte, ins.preState, ins.memory)
}

private class DataWordMode(type: DataValueType) : DataValueMode(2u, type) {
    override fun printRaw(ins: CodeUnit) = "$" + toHex(ins.word)
    override fun referencedAddress(ins: CodeUnit): SnesAddress? = type.resolve(ins.word, ins.preState, ins.memory)
}

private class DataLongMode(type: DataValueType) : DataValueMode(3u, type) {
    override fun printRaw(ins: CodeUnit) = "$" + toHex(ins.long)
    override fun referencedAddress(ins: CodeUnit): SnesAddress? = type.resolve(ins.long, ins.preState, ins.memory)
}

object Implied : Mode {
    override val canHaveLabel = false
    override val showLengthSuffix = false
    override fun operandLength(state: State) = 0u
    override fun printRaw(ins: CodeUnit) = ""
    override fun referencedAddress(ins: CodeUnit): SnesAddress? = null
}

object Immediate8 : Mode {
    override val canHaveLabel = false
    override val showLengthSuffix = false
    override fun operandLength(state: State) = 1u
    override fun printRaw(ins: CodeUnit) = "#$" + toHex(ins.byte)
    override fun referencedAddress(ins: CodeUnit): SnesAddress? = null
}

object Immediate16 : Mode {
    override val canHaveLabel = false
    override val showLengthSuffix = false
    override fun operandLength(state: State) = 2u
    override fun printRaw(ins: CodeUnit) = "#$" + toHex(ins.word)
    override fun referencedAddress(ins: CodeUnit): SnesAddress? = null
}

object Direct : Mode {
    override val canHaveLabel = true
    override fun operandLength(state: State) = 1u
    override fun printRaw(ins: CodeUnit) = "$" + toHex(ins.byte)
    override fun referencedAddress(ins: CodeUnit) = ins.preState?.resolveDirectPage(ins.byte)
}

object Absolute : Mode {
    override val canHaveLabel = true
    override fun operandLength(state: State) = 2u
    override fun printRaw(ins: CodeUnit) = "$" + toHex(ins.word)
    override fun referencedAddress(ins: CodeUnit) = ins.preState?.resolveAbsoluteData(ins.word)
}

object AbsoluteCode : Mode {
    override val canHaveLabel = true
    override fun operandLength(state: State) = 2u
    override fun printRaw(ins: CodeUnit) = "$" + toHex(ins.word)
    override fun referencedAddress(ins: CodeUnit) = ins.preState?.resolveAbsoluteCode(ins.word)
}

object AbsoluteLong : Mode {
    override val canHaveLabel = true
    override fun operandLength(state: State) = 3u
    override fun printRaw(ins: CodeUnit) = "$" + toHex(ins.long)
    override fun referencedAddress(ins: CodeUnit) = ins.memory.toCanonical(SnesAddress(ins.long))
}

private object ImmediateUnknownMode : Mode {
    override val canHaveLabel = false
    override fun operandLength(state: State): UInt? = null
    override fun printRaw(ins: CodeUnit) = "???"
    override fun referencedAddress(ins: CodeUnit): SnesAddress? = null
}

object ImmediateM : MultiMode(ImmediateUnknownMode, Immediate8, Immediate16) {
    override fun pickMode(state: State) = state.mWidth ?: 0u
}

object ImmediateX : MultiMode(ImmediateUnknownMode, Immediate8, Immediate16) {
    override fun pickMode(state: State) = state.xWidth ?: 0u
}

abstract class BaseRelativeMode(private val length: UInt) : Mode {
    override val showLengthSuffix = false
    override fun operandLength(state: State) = length
    override fun printRaw(ins: CodeUnit) = "$" + referencedAddress(ins).toSimpleString()
    override fun referencedAddress(ins: CodeUnit) = ins.memory.toCanonical(ins.relativeAddress + (relativeOffset(ins) + 1 + length.toInt()))
    protected abstract fun relativeOffset(ins: CodeUnit): Int
}

object Relative : BaseRelativeMode(1u) {
    override val canHaveLabel = true
    override fun relativeOffset(ins: CodeUnit) = ins.signedByte.toInt()
}

object RelativeLong : BaseRelativeMode(2u) {
    override val canHaveLabel = true
    override fun relativeOffset(ins: CodeUnit) = ins.signedWord.toInt()
}

object BlockMove : Mode {
    override val canHaveLabel = false
    override val showLengthSuffix = false

    override fun operandLength(state: State) = 2u

    override fun printRaw(ins: CodeUnit) = String.format("#$%02x,#$%02x", ins.byte.toInt(), ins.byte2.toInt())

    override fun referencedAddress(ins: CodeUnit): SnesAddress? = null
}

private class IndexXMode(parent: Mode) : WrappedMode(parent, suffix = ",x")
private class IndexYMode(parent: Mode) : WrappedMode(parent, suffix = ",y")
private class IndexSMode(parent: Mode) : RawWrappedMode(parent, suffix = ",s")
private class IndirectMode(parent: Mode) : WrappedMode(parent, "(", ")")
private class IndirectLongMode(parent: Mode) : WrappedMode(parent, "[", "]")
