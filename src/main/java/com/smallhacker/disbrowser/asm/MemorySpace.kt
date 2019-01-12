package com.smallhacker.disbrowser.asm

import com.smallhacker.disbrowser.util.UInt24
import com.smallhacker.disbrowser.util.joinBytes
import com.smallhacker.disbrowser.util.joinNullableBytes
import com.smallhacker.disbrowser.util.toUInt24
import java.nio.file.Files
import java.nio.file.Path

interface MemorySpace {
    val size: UInt
    operator fun get(address: UInt): UByte?
}

interface ValidMemorySpace : MemorySpace {
    override operator fun get(address: UInt): UByte
}

object EmptyMemorySpace : ValidMemorySpace {
    override val size get() = 0u
    override fun get(address: UInt) = throw IllegalStateException()
}

fun MemorySpace.asSequence(): Sequence<UByte?> = (0u until size).asSequence().map { this[it] }
fun MemorySpace.getWord(address: UInt): UShort? = joinNullableBytes(this[address], this[address + 1u])?.toUShort()
fun MemorySpace.getLong(address: UInt): UInt24? = joinNullableBytes(this[address], this[address + 1u], this[address + 2u])?.toUInt24()
fun MemorySpace.range(start: UInt, length: UInt): MemorySpace = MemoryRange(this, start, length)
fun MemorySpace.range(start: SnesAddress, length: UInt): MemorySpace = range(start.value.toUInt(), length)
fun MemorySpace.validate(): ValidMemorySpace? {
    if (asSequence().any { it == null }) {
        return null
    }
    return ValidatedMemorySpace(this)
}

fun ValidMemorySpace.asSequence(): Sequence<UByte> = (0u until size).asSequence().map { this[it] }
fun ValidMemorySpace.getWord(address: UInt): UShort = joinBytes(this[address], this[address + 1u]).toUShort()
fun ValidMemorySpace.getLong(address: UInt): UInt24 = joinBytes(this[address], this[address + 1u], this[address + 2u]).toUInt24()
fun ValidMemorySpace.range(start: UInt, length: UInt): ValidMemorySpace = MemoryRange(this, start, length).validate()!!
fun ValidMemorySpace.range(start: SnesAddress, length: UInt): ValidMemorySpace = range(start.value.toUInt(), length).validate()!!

fun loadRomData(path: Path): MemorySpace {
    val bytes = Files.readAllBytes(path).toUByteArray()
    return ArrayMemorySpace(bytes)
}

class ArrayMemorySpace(private val bytes: UByteArray) : MemorySpace {
    override val size = bytes.size.toUInt()

    override fun get(address: UInt): UByte? {
        if (address >= size) {
            return null
        }
        return bytes[address.toInt()]
    }
}

class UnreadableMemory(override val size: UInt) : MemorySpace {
    override fun get(address: UInt): UByte? = null
}

private class MemoryRange(private val parent: MemorySpace, private val start: UInt, override val size: UInt) : MemorySpace {
    override fun get(address: UInt) = if (address < size) parent[start + address] else null
}


private class ValidatedMemorySpace(private val parent: MemorySpace) : ValidMemorySpace {
    override val size get() = parent.size
    override fun get(address: UInt) = parent[address]!!
}

private class ReindexedMemorySpace(
        private val parent: MemorySpace,
        override val size: UInt,
        private val mapper: (UInt) -> UInt
) : MemorySpace {
    override fun get(address: UInt): UByte? {
        if (address >= size) {
            return null
        }
        val mapped = mapper(address)
        return parent[mapped]
    }
}

fun MemorySpace.deinterleave(entries: UInt, vararg startOffsets: UInt): MemorySpace {
    val fieldCount = startOffsets.size.toUInt()
    return ReindexedMemorySpace(this, entries * fieldCount) {
        val entry = it / fieldCount
        val field = it.rem(fieldCount)
        startOffsets[field.toInt()] + entry
    }
}