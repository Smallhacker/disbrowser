package com.smallhacker.disbrowser.asm

import com.smallhacker.disbrowser.datatype.MutableRangeMap
import com.smallhacker.disbrowser.datatype.NaiveRangeMap
import com.smallhacker.disbrowser.util.toUInt24

abstract class SnesMemory: MemorySpace {
    override val size = 0x100_0000u
    private val areas: MutableRangeMap<UInt, UIntRange, MapperEntry> = NaiveRangeMap()

    protected fun add(start: UInt, canonicalStart: UInt, memorySpace: MemorySpace) {
        val range = start until (start + memorySpace.size)
        areas[range] = MapperEntry(start, SnesAddress(canonicalStart.toUInt24()), memorySpace)
    }

    override fun get(address: UInt): UByte? {
        val entry = areas[address] ?: return null
        val offset = address - entry.start
        return entry.space[offset]
    }

    fun toCanonical(address: SnesAddress): SnesAddress {
        val entry = areas[address.value.toUInt()] ?: return address
        val offset = address.value - entry.start
        return entry.canonicalStart + offset.toInt()
    }

    companion object {
        fun loadRom(fileData: UByteArray): SnesMemory {
            val romSpace = ArrayMemorySpace(fileData)
            // TODO: Auto-detect ROM type
            return SnesLoRom(romSpace)
        }
    }
}

operator fun MemorySpace.get(address: SnesAddress) = get(address.value.toUInt())
fun MemorySpace.getWord(address: SnesAddress) = getWord(address.value.toUInt())
fun MemorySpace.getLong(address: SnesAddress) = getLong(address.value.toUInt())

data class MapperEntry(val start: UInt, val canonicalStart: SnesAddress, val space: MemorySpace)

class SnesLoRom(romData: MemorySpace): SnesMemory() {
    init {
        val ram = UnreadableMemory(0x2_0000u)
        val ramMirror = ram.range(0x00_0000u, 0x00_2000u)
        val registers = UnreadableMemory(0x6_0000u)
        val sram = UnreadableMemory(0x0_8000u)
        val ramStart = 0x7e_0000u
        val regStart = 0x00_2000u
        val srmStart = 0x70_0000u
        var pc = 0x00_0000u
        val high = 0x80_0000u

        for (bank in 0x00u..0x3Fu) {
            val ramArea = (bank shl 16)
            val regArea = (bank shl 16) or 0x00_2000u
            val romArea = (bank shl 16) or 0x00_8000u
            add(ramArea, ramStart, ramMirror)
            add(regArea, regStart, registers)
            add(romArea, romArea, romData.range(pc, 0x00_8000u))
            add(ramArea + high, ramStart, ramMirror)
            add(regArea + high, regStart, registers)
            add(romArea + high, romArea, romData.range(pc, 0x00_8000u))
            pc += 0x00_8000u
        }

        for (bank in 0x40u..0x6Fu) {
            val lowerRomArea = (bank shl 16)
            val upperRomArea = (bank shl 16) or 0x00_8000u
            // Mirror upper and lower banks to the same ROM space.
            // Some games map it this way, some leave the lower half completely unmapped
            // While not 100% correct, we do the former to support as many games as possible

            // Of note, we choose to explicitly define the upper half to be the canonical half.

            add(lowerRomArea, upperRomArea, romData.range(pc, 0x00_8000u))
            add(lowerRomArea + high, upperRomArea, romData.range(pc, 0x00_8000u))
            add(upperRomArea, upperRomArea, romData.range(pc, 0x00_8000u))
            add(upperRomArea + high, upperRomArea, romData.range(pc, 0x00_8000u))
            pc += 0x00_8000u
        }

        for (bank in 0x70u..0x7Du) {
            val srmArea = (bank shl 16)
            val romArea = (bank shl 16) or 0x00_8000u
            add(srmArea, srmStart, sram)
            add(srmArea + high, srmStart, sram)
            add(romArea, romArea, romData.range(pc, 0x00_8000u))
            add(romArea + high, romArea, romData.range(pc, 0x00_8000u))
            pc += 0x00_8000u
        }

        for (bank in 0xFEu..0xFFu) {
            val romArea = (bank shl 16) or 0x00_8000u
            add(romArea, romArea, romData.range(pc, 0x00_8000u))
            pc += 0x00_8000u
        }

        add(ramStart, ramStart, ram)
    }
}