package com.smallhacker.disbrowser.asm

import com.smallhacker.disbrowser.ImmStack
import com.smallhacker.disbrowser.immStack
import com.smallhacker.disbrowser.util.toUInt24

data class State(val origin: Instruction? = null, val data: RomData, val address: SnesAddress, val flags: VagueNumber = VagueNumber(), val stack: ImmStack<VagueNumber> = immStack()) {
    val m: Boolean? get() = flags.getBoolean(0x20u)
    val x: Boolean? get() = flags.getBoolean(0x10u)
    val db: UByte? get() = pb // TODO
    val dp: UShort? get() = 0x7e00u // TODO
    val pb: UByte get() = (address.value shr 16).toUByte()

    val mWidth: UInt? get() = toWidth(m)
    val xWidth: UInt? get() = toWidth(x)

    fun sep(i: UByte) = withFlags(flags.withBits(i.toUInt()))
    fun rep(i: UByte) = withFlags(flags.withoutBits(i.toUInt()))
    fun uncertain() = withFlags(VagueNumber())

    private fun withFlags(flags: VagueNumber) = copy(flags = flags)

    fun mutateAddress(mutator: (SnesAddress) -> SnesAddress) = copy(address = mutator(address))
    fun withOrigin(instruction: Instruction?) = copy(origin = instruction)

    fun push(value: UInt) = push(VagueNumber(value))
    fun push(value: VagueNumber) = copy(stack = stack.push(value))
    fun pushUnknown(count: UInt? = 1u): State {
        if (count == null) {
            return copy(stack = immStack())
        }

        var stack = this.stack
        for (i in 1u..count) {
            stack = stack.push(VagueNumber())
        }
        return copy(stack = stack)
    }

    fun pull() = (stack.top ?: VagueNumber()) to copy(stack = stack.pop())
    fun pull(count: UInt?): State {
        if (count == null) {
            return copy(stack = immStack())
        }

        var stack = this.stack
        for (i in 1u..count) {
            stack = stack.pop()
        }
        return copy(stack = stack)
    }

    fun clearStack() = copy(stack = immStack())

    override fun toString(): String {
        return "A:${printSize(m)} XY:${printSize(x)} S:" + stackToString()
    }

    fun resolve(directPage: UByte) = dp?.let { dp ->
        val ptr = (dp.toUInt24() shl 8) or (directPage.toUInt24())
        SnesAddress(ptr)
    }

    fun resolve(absolute: UShort)= db?.let { db ->
        val ptr = (db.toUInt24() shl 16) or (absolute.toUInt24())
        SnesAddress(ptr)
    }

    private fun stackToString(): String {
        return stack.reversed().asSequence()
                .map { stackByteToString(it) }
                .joinToString(" ")
    }

    private fun stackByteToString(v: VagueNumber): String {
        if (v.certain) {
            return String.format("%02x", v.value.toInt())
        }

        if (v.certainty == 0u) {
            return "??"
        }

        val c = v.certainty
        val high = (c and 0xF0u) != 0u
        val low = (c and 0x0Fu) != 0u

        return StringBuilder()
                .append(if (high) String.format("%x", ((v.value shr 4) and 0xFu).toInt()) else "?")
                .append(if (low) String.format("%x", (v.value and 0xFu).toInt()) else "?")
                .toString()
    }

    private fun printSize(flag: Boolean?): String = when (flag) {
        null -> "??"
        true -> " 8"
        false -> "16"
    }

    val urlString: String
        get() {
            val out = StringBuilder()
            out.append(when (x) {
                null -> ""
                true -> "X"
                false -> "x"
            })
            out.append(when (m) {
                null -> ""
                true -> "M"
                false -> "m"
            })
            return out.toString()
        }

    private fun toWidth(flag: Boolean?): UInt? = when (flag) {
        null -> null
        true -> 1u
        false -> 2u
    }
}

fun State.pushByte(value: Byte) = this.push(VagueNumber(value.toUInt()))
fun State.pull(consumer: State.(VagueNumber) -> State): State {
    val (value, state) = this.pull()
    return consumer(state, value)
}