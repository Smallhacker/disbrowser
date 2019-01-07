package com.smallhacker.disbrowser.asm

import com.smallhacker.disbrowser.util.*
import java.nio.file.Files
import java.nio.file.Path

interface RomData {
    val size: Int

    operator fun get(address: Int): UByte

    fun range(start: Int, length: Int): RomData = RomRange(this, start, length)

    fun asSequence(): Sequence<UByte> = (0 until size).asSequence().map { get(it) }

    companion object {
        fun load(path: Path): RomData = Rom(Files.readAllBytes(path))
    }
}

fun RomData.getWord(address: Int): UWord = get(address).toWord() or (get(address + 1).toWord() left 8)

private class Rom(private val bytes: ByteArray): RomData {
    override val size = bytes.size

    override fun get(address: Int): UByte {
        checkRange(address)
        return uByte(bytes[address].toInt())
    }

}

private class RomRange(private val parent: RomData, private val start: Int, override val size: Int): RomData {
    override fun get(address: Int): UByte {
        checkRange(address)
        return parent[start + address]
    }
}

private fun RomData.checkRange(address: Int) {
    if (address < 0 || address >= size) {
        throw IndexOutOfBoundsException("Index $address out of range: [0, $size)")
    }
}
