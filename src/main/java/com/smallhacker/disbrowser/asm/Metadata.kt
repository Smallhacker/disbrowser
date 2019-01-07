package com.smallhacker.disbrowser.asm

import com.smallhacker.disbrowser.util.left
import com.smallhacker.disbrowser.util.or
import com.smallhacker.disbrowser.util.toLong

class Metadata {
    private val lines = HashMap<Address, MetadataLine>()

    fun add(line: MetadataLine): Metadata {
        lines[line.address] = line
        return this
    }

    operator fun get(address: Address): MetadataLine? {
        return lines[address]
    }
}

fun Metadata.at(address: Int, runner: MetadataLine.() -> Unit) {
    val line = MetadataLine(Address(address))
    this.add(line)
    runner(line)
}

fun metadata(runner: Metadata.() -> Unit): Metadata {
    val metadata = Metadata()
    runner(metadata)
    return metadata
}

interface InstructionFlag

object NonReturningRoutine : InstructionFlag

class JmpIndirectLongInterleavedTable(private val start: Address, private val entries: Int) : InstructionFlag {
    fun readTable(data: RomData): Sequence<Address> {
        return (0 until entries)
                .asSequence()
                .map { it + start.pc }
                .map { pc -> data[pc].toLong() or (data[pc + entries].toLong() left 8) or (data[pc + entries + entries].toLong() left 16) }
                .map { Address(it.value) }

    }
}

class JslTableRoutine(private val entries: Int) : InstructionFlag {
    fun readTable(postJsr: State): Sequence<Address> {
        val data = postJsr.data
        return (0 until entries)
                .asSequence()
                .map { postJsr.address.pc + (it * 3) }
                .map { pc -> data[pc].toLong() or (data[pc + 1].toLong() left 8) or (data[pc + 2].toLong() left 16) }
                .map { Address(it.value) }
    }

//    fun dataInstructions(postJsr: State) {
//        val data = postJsr.data
//        return (0 until entries)
//                .asSequence()
//                .map {
//                    val offset = it * 3
//                    Instruction(data.range(postJsr.address.pc + offset, 3), Opcode.POINTER_LONG, postJsr.mutateAddress { it + offset })
//                }
//    }
}