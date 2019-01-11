package com.smallhacker.disbrowser.asm

import com.smallhacker.disbrowser.util.*
import java.nio.file.Files
import java.nio.file.Path

interface RomData {
    val size: UInt

    operator fun get(address: UInt): UByte

    fun range(start: UInt, length: UInt): RomData = RomRange(this, start, length)

    fun asSequence(): Sequence<UByte> = (0u until size).asSequence().map { this[it] }

    companion object {
        fun load(path: Path): RomData = ArrayRomData(Files.readAllBytes(path).toUByteArray())
    }
}

fun romData(vararg bytes: UByte): RomData = ArrayRomData(bytes)

fun RomData.getWord(address: UInt) = joinBytes(this[address], this[address + 1u]).toUShort()
fun RomData.getLong(address: UInt) = joinBytes(this[address], this[address + 1u], this[address + 2u]).toUInt24()

private class ArrayRomData(private val bytes: UByteArray) : RomData {
    override val size = bytes.size.toUInt()

    override fun get(address: UInt) = rangeChecked(address) {
        bytes[address.toInt()]
    }

}

private class RomRange(private val parent: RomData, private val start: UInt, override val size: UInt) : RomData {
    override fun get(address: UInt) = rangeChecked(address) {
        parent[start + address]
    }
}

private fun <T> RomData.rangeChecked(address: UInt, action: () -> T): T {
    if (address >= size) {
        throw IndexOutOfBoundsException("Index $address out of range: [0, $size)")
    }
    return action()
}

private class ReindexedRomData(
        private val parent: RomData,
        override val size: UInt,
        private val mapper: (UInt) -> UInt
) : RomData {
    override fun get(address: UInt) = rangeChecked(address) {
        val mapped = mapper(address)
        parent[mapped]
    }
}

fun RomData.deinterleave(entries: UInt, vararg startOffsets: UInt): RomData {
    val fieldCount = startOffsets.size.toUInt()
    return ReindexedRomData(this, entries * fieldCount) {
        val entry = it / fieldCount
        val field = it.rem(fieldCount)
        startOffsets[field.toInt()] + entry
    }
}