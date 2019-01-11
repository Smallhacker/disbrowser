package com.smallhacker.disbrowser.asm

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.smallhacker.disbrowser.util.joinBytes
import com.smallhacker.disbrowser.util.toUInt24
import java.util.*

class Metadata {
    private val lines: MutableMap<Address, MetadataLine>

    constructor() {
        this.lines = HashMap()
    }

    @JsonCreator
    private constructor(@JsonProperty lines: Map<Address, MetadataLine>) {
        this.lines = HashMap(lines)
    }

    @JsonValue
    private fun serialize() = TreeMap(lines)

    operator fun set(address: Address, line: MetadataLine?): Metadata {
        if (line == null) {
            lines.remove(address)
        } else {
            lines[address] = line
        }
        return this
    }

    operator fun get(address: Address): MetadataLine? {
        return lines[address]
    }

    operator fun contains(address: Address) = lines[address] != null

    fun getOrCreate(address: Address): MetadataLine {
        val line = this[address]
        if (line != null) {
            return line
        }
        val newLine = MetadataLine()
        this[address] = newLine
        return newLine
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "flagType")
@JsonSubTypes(
        Type(value = NonReturningRoutine::class, name = "NonReturningRoutine"),
        Type(value = JmpIndirectLongInterleavedTable::class, name = "JmpIndirectLongInterleavedTable"),
        Type(value = JslTableRoutine::class, name = "JslTableRoutine")
)
interface InstructionFlag

object NonReturningRoutine : InstructionFlag {
    override fun toString() = "NonReturningRoutine"
}

class JmpIndirectLongInterleavedTable @JsonCreator constructor(
        @field:JsonProperty @JsonProperty private val start: Address,
        @field:JsonProperty @JsonProperty private val entries: Int
) : InstructionFlag {
    private val uEntries get() = entries.toUInt()

    fun readTable(data: RomData): Sequence<Address> {
        return (0u until uEntries)
                .asSequence()
                .map { it + start.pc }
                .map { pc -> joinBytes(data[pc], data[pc + uEntries], data[pc + uEntries + uEntries]).toUInt24() }
                .map { Address(it) }
    }

    fun generateCode(jumpInstruction: Instruction): Sequence<DataBlock> {
        val table = jumpInstruction.preState.data.deinterleave(uEntries,
                start.pc,
                (start + entries).pc,
                (start + (2u * uEntries).toInt()).pc
        )

        return (0u until uEntries)
                .asSequence()
                .map { index -> index * 3u }
                .map { offset ->
                    val target = table.getLong(offset)

                    DataBlock(
                            Opcode.POINTER_LONG,
                            table.range(offset, 3u),
                            jumpInstruction.postState.address + offset.toInt(),
                            jumpInstruction.relativeAddress,
                            jumpInstruction.opcode.mutate(jumpInstruction)
                                    .mutateAddress { Address(target) }
                                    .withOrigin(jumpInstruction)
                    )
//                    Instruction(
//                            data.range((start.value + offset).toUInt(), 3u),
//                            Opcode.POINTER_LONG,
//                            preState.mutateAddress { start -> start + offset.toInt() }
//                    )
                }
    }

    override fun toString() = "JmpIndirectLongInterleavedTable($start, $entries)"
}

class JslTableRoutine @JsonCreator constructor(
        @field:JsonProperty @JsonProperty private val entries: Int
) : InstructionFlag {

    fun readTable(postJsr: State): Sequence<Address> {
        val data = postJsr.data
        return (0u until entries.toUInt())
                .asSequence()
                .map { postJsr.address.pc + (it * 3u) }
                .map { pc -> joinBytes(data[pc], data[pc + 1u], data[pc + 2u]).toUInt24() }
                .map { Address(it) }
    }

    override fun toString() = "JslTableRoutine($entries)"
}