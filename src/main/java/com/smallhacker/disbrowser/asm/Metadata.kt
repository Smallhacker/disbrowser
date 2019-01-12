package com.smallhacker.disbrowser.asm

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.smallhacker.disbrowser.util.joinNullableBytes
import com.smallhacker.disbrowser.util.toUInt24
import java.util.*

class Metadata {
    @JsonProperty
    private val code: TreeMap<SnesAddress, MetadataLine>

    constructor() {
        this.code = TreeMap()
    }

    @JsonCreator
    private constructor(@JsonProperty code: Map<SnesAddress, MetadataLine>) {
        this.code = TreeMap(code)
    }

    operator fun set(address: SnesAddress, line: MetadataLine?): Metadata {
        if (line == null) {
            code.remove(address)
        } else {
            code[address] = line
        }
        return this
    }

    operator fun get(address: SnesAddress): MetadataLine? {
        return code[address]
    }

    operator fun contains(address: SnesAddress) = code[address] != null

    fun getOrCreate(address: SnesAddress): MetadataLine {
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
        @field:JsonProperty @JsonProperty private val start: SnesAddress,
        @field:JsonProperty @JsonProperty private val entries: Int
) : InstructionFlag {
    private val uEntries get() = entries.toUInt()

    fun readTable(data: MemorySpace): Sequence<SnesAddress?> {
        return (0 until entries)
                .asSequence()
                .map { start + it }
                .map { address -> joinNullableBytes(data[address], data[address + entries], data[address + entries + entries])?.toUInt24() }
                .map { pointer -> pointer?.let { SnesAddress(it) } }
    }

    fun generateCode(jumpInstruction: Instruction): Sequence<DataBlock> {
        val table = jumpInstruction.preState.memory.deinterleave(uEntries,
                start.value.toUInt(),
                (start + entries).value.toUInt(),
                (start + (2 * uEntries.toInt())).value.toUInt()
        ).validate() ?: return emptySequence()

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
                                    .mutateAddress { SnesAddress(target) }
                                    .withOrigin(jumpInstruction)
                    )
                }
    }

    override fun toString() = "JmpIndirectLongInterleavedTable($start, $entries)"
}

class JslTableRoutine @JsonCreator constructor(
        @field:JsonProperty @JsonProperty private val entries: Int
) : InstructionFlag {

    fun readTable(postJsr: State): Sequence<SnesAddress?> {
        val data = postJsr.memory
        return (0 until entries)
                .asSequence()
                .map { postJsr.address + (it * 3) }
                .map { address -> joinNullableBytes(data[address], data[address + 1], data[address + 2])?.toUInt24() }
                .map { pointer -> pointer?.let { SnesAddress(it) } }
    }

    override fun toString() = "JslTableRoutine($entries)"
}