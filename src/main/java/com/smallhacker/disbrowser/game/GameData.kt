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

class JslTableRoutine : InstructionFlag {
    fun readTable(postJsr: State, entryCount: Int): Sequence<SnesAddress?> {
        val data = postJsr.memory
        return (0 until entryCount)
                .asSequence()
                .map { postJsr.address + (it * 3) }
                .map { address -> joinNullableBytes(data[address], data[address + 1], data[address + 2])?.toUInt24() }
                .map { pointer -> pointer?.let { SnesAddress(it) } }
    }

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

        val start = jumpInstruction.postState.address
        val memory = jumpInstruction.memory

        return (0u until count.toUInt())
                .asSequence()
                .map { index -> index * 3u }
                .mapNotNull { offset ->
                    val pointerLoc= start + offset.toInt()
                    val addressRange = memory.range(pointerLoc, 3u).validate()

                    if (addressRange == null) {
                        null
                    } else {
                        val target = addressRange.getLong(0u)

                        val block = DataBlock(
                                Opcode.CODE_POINTER_LONG,
                                addressRange,
                                pointerLoc,
                                jumpInstruction.relativeAddress,
                                jumpInstruction.opcode.mutate(jumpInstruction)
                                        .mutateAddress { SnesAddress(target) }
                                        .withOrigin(jumpInstruction),
                                memory,
                                certainty
                        )
                        certainty -= certaintyDecrease
                        block
                    }
                }
    }

    override fun toString() = "JslTableRoutine"
}

class PointerTableLength @JsonCreator constructor(
        @field:JsonProperty @JsonProperty val entries: Int
) : InstructionFlag {
    override fun toString() = "PointerTableLength($entries)"
}