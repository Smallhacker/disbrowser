package com.smallhacker.disbrowser.game

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.smallhacker.disbrowser.asm.*
import com.smallhacker.disbrowser.util.joinNullableBytes
import com.smallhacker.disbrowser.util.removeIf
import com.smallhacker.disbrowser.util.toUInt24
import java.util.*

class GameData {
    @JsonProperty
    val name: String
    @JsonProperty
    val path: String
    @JsonProperty
    private val metadata: MutableMap<SnesAddress, MetadataLine>

    constructor(name: String, path: String) {
        this.name = name
        this.path = path
        this.metadata = TreeMap()
    }

    @JsonCreator
    private constructor(@JsonProperty name: String, @JsonProperty path: String, @JsonProperty metadata: Map<SnesAddress, MetadataLine>) {
        this.name = name
        this.path = path
        this.metadata = TreeMap(metadata)
    }

    operator fun set(address: SnesAddress, line: MetadataLine?): GameData {
        if (line == null) {
            metadata.remove(address)
        } else {
            metadata[address] = line
        }
        return this
    }

    operator fun get(address: SnesAddress): MetadataLine? {
        return metadata[address]
    }

    operator fun contains(address: SnesAddress) = metadata[address] != null

    fun getOrCreate(address: SnesAddress): MetadataLine {
        val line = this[address]
        if (line != null) {
            return line
        }
        val newLine = MetadataLine()
        this[address] = newLine
        return newLine
    }

    fun cleanUp() {
        metadata.removeIf { _, v -> v.isEmpty() }
    }
}

fun GameData.flagsAt(snesAddress: SnesAddress?): MetadataLineFlags =
        MetadataLineFlags(get(snesAddress)?.flags ?: emptyList())

inline class MetadataLineFlags(val flags: List<InstructionFlag>) {
    inline fun <reified F> findFlag() = flags.asSequence().filterIsInstance<F>().firstOrNull()
    inline fun <reified F> forFlag(action: F.() -> Unit) = findFlag<F>()?.run(action)
}

operator fun GameData.get(address: SnesAddress?) = if (address == null) null else this[address]

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "flagType")
@JsonSubTypes(
        Type(value = NonReturningRoutine::class, name = "NonReturningRoutine"),
        Type(value = JmpIndirectLongInterleavedTable::class, name = "JmpIndirectLongInterleavedTable"),
        Type(value = JslTableRoutine::class, name = "JslTableRoutine"),
        Type(value = JsrTableRoutine::class, name = "JsrTableRoutine"),
        Type(value = PointerTableLength::class, name = "PointerTableLength")
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

    fun generatePointerTable(jumpInstruction: Instruction): Sequence<DataBlock> {
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
                            Opcode.CODE_POINTER_LONG,
                            table.range(offset, 3u),
                            start + offset.toInt(),
                            jumpInstruction.postState.address + offset.toInt(),
                            jumpInstruction.relativeAddress,
                            jumpInstruction.opcode.mutate(jumpInstruction)
                                    .mutateAddress { SnesAddress(target) }
                                    .withOrigin(jumpInstruction),
                            jumpInstruction.memory,
                            Certainty.PROBABLY_CORRECT
                    )
                }
    }

    override fun toString() = "JmpIndirectLongInterleavedTable($start, $entries)"
}

interface DynamicJumpRoutine : InstructionFlag {
    fun readTable(postJump: State, entryCount: Int): Sequence<SnesAddress?> {
        return (0 until entryCount)
                .asSequence()
                .map { readEntry(postJump, it) }
    }

    fun readEntry(postJump: State, index: Int): SnesAddress?

    fun generatePointerTable(jumpInstruction: Instruction, entryCount: UInt?): Sequence<DataBlock> {
        val count: UInt
        var certainty: Certainty
        val certaintyDecrease: Int

        if (entryCount == null) {
            count = 30u
            certainty = Certainty.UNCERTAIN
            certaintyDecrease = 5
        } else {
            count = entryCount
            certainty = Certainty.PROBABLY_CORRECT
            certaintyDecrease = 0
        }

        return (0u until count)
                .asSequence()
                .mapNotNull { index ->
                    generatePointer(jumpInstruction, index, certainty)
                            ?.also {
                                certainty -= certaintyDecrease
                            }
                }
    }

    fun generatePointer(jumpInstruction: Instruction, index: UInt, certainty: Certainty): DataBlock?

    override fun toString(): String
}

class JslTableRoutine : DynamicJumpRoutine {
    override fun readEntry(postJump: State, index: Int): SnesAddress? {
        val data = postJump.memory
        val address = postJump.address + (index * 3)
        return joinNullableBytes(data[address], data[address + 1], data[address + 2])
                ?.toUInt24()
                ?.let { SnesAddress(it) }
    }

    override fun generatePointer(jumpInstruction: Instruction, index: UInt, certainty: Certainty): DataBlock? {
        val offset = index * 3u
        val pointerLoc = jumpInstruction.postState.address + offset.toInt()
        val addressRange = jumpInstruction.memory.range(pointerLoc, 3u).validate()

        if (addressRange == null) {
            return null
        } else {
            val target = addressRange.getLong(0u)

            return DataBlock(
                    Opcode.CODE_POINTER_LONG,
                    addressRange,
                    pointerLoc,
                    jumpInstruction.relativeAddress,
                    jumpInstruction.opcode.mutate(jumpInstruction)
                            .mutateAddress { SnesAddress(target) }
                            .withOrigin(jumpInstruction),
                    jumpInstruction.memory,
                    certainty
            )
        }
    }

    override fun toString() = "JslTableRoutine"
}

class JsrTableRoutine : DynamicJumpRoutine {
    override fun readEntry(postJump: State, index: Int): SnesAddress? {
        val data = postJump.memory
        val address = postJump.address + (index * 2)
        return joinNullableBytes(data[address], data[address + 1], postJump.pb)
                ?.toUInt24()
                ?.let { SnesAddress(it) }
    }

    override fun generatePointer(jumpInstruction: Instruction, index: UInt, certainty: Certainty): DataBlock? {
        val offset = index * 2u
        val pointerLoc = jumpInstruction.postState.address + offset.toInt()
        val addressRange = jumpInstruction.memory.range(pointerLoc, 2u).validate()

        if (addressRange == null) {
            return null
        } else {
            val target = addressRange.getWord(0u).toUInt24() or (jumpInstruction.postState.pb.toUInt24() shl 16)

            return DataBlock(
                    Opcode.CODE_POINTER_WORD,
                    addressRange,
                    pointerLoc,
                    jumpInstruction.relativeAddress,
                    jumpInstruction.opcode.mutate(jumpInstruction)
                            .mutateAddress { SnesAddress(target) }
                            .withOrigin(jumpInstruction),
                    jumpInstruction.memory,
                    certainty
            )
        }
    }

    override fun toString() = "JsrTableRoutine"
}

class PointerTableLength @JsonCreator constructor(
        @field:JsonProperty @JsonProperty val entries: Int
) : InstructionFlag {
    override fun toString() = "PointerTableLength($entries)"
}