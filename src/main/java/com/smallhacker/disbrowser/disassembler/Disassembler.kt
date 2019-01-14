package com.smallhacker.disbrowser.disassembler

import com.smallhacker.disbrowser.asm.*
import com.smallhacker.disbrowser.game.GameData
import com.smallhacker.disbrowser.game.JmpIndirectLongInterleavedTable
import com.smallhacker.disbrowser.game.JslTableRoutine
import com.smallhacker.disbrowser.game.NonReturningRoutine
import java.util.*
import kotlin.collections.ArrayList

object Disassembler {
    fun disassemble(initialState: State, gameData: GameData, global: Boolean): Disassembly {
        val seen = HashSet<SnesAddress>()
        val queue = ArrayDeque<State>()

        fun tryAdd(state: State) {
            if (seen.add(state.address)) {
                queue.add(state)
            }
        }
        tryAdd(initialState)

        val instructions = ArrayList<CodeUnit>()
        while (queue.isNotEmpty()) {
            val state = queue.remove()

            val ins = disassembleInstruction(state)
            instructions.add(ins)

            var stop = (ins.opcode.continuation == Continuation.NO) or
                    (ins.opcode.mode.instructionLength(state) == null)

            gameData[ins.address]?.flags?.forEach { flag ->
                if (flag is JmpIndirectLongInterleavedTable) {
                    if (global) {
                        flag.readTable(state.memory)
                                .filterNotNull()
                                .map { ins.postState.copy(address = it) }
                                .forEach { tryAdd(it) }
                    }

                    flag.generateCode(ins)
                            .forEach { instructions.add(it) }

                    stop = true
                } else if (flag is JslTableRoutine) {
                    if (global) {
                        flag.readTable(ins.postState)
                                .filterNotNull()
                                .map { ins.postState.copy(address = it) }
                                .forEach { tryAdd(it) }
                    }
                    stop = true
                }
            }

            val linkedState = ins.linkedState

            if (linkedState != null) {
                gameData[linkedState.address]?.flags?.forEach {
                    if (it === NonReturningRoutine) {
                        stop = true
                        println(ins.address.toFormattedString())
                    }
                }
            }

            if (!stop) {
                tryAdd(ins.postState)
            }


            if (linkedState != null) {
                if (ins.opcode.branch || global) {
                    tryAdd(linkedState)
                }
            }
        }

        val instructionList = instructions
                .sortedBy { it.sortedAddress }
                .toList()

        return Disassembly(instructionList)
    }

    fun disassembleSegments(initialState: State): List<Segment> {
        val mapping = HashMap<SnesAddress, Segment>()
        val queue = ArrayDeque<State>()

        val segments = ArrayList<Segment>()

        fun tryAdd(state: State) {
            if (!mapping.containsKey(state.address)) {
                queue.add(state)
            }
        }
        tryAdd(initialState)


        while (queue.isNotEmpty()) {
            val state = queue.remove()

            if (mapping.containsKey(state.address)) {
                continue
            }

            val segment = disassembleSegment(state, mapping)
            if (segment.instructions.isEmpty()) {
                continue
            }

            segments.add(segment)

            val end = segment.end
            end.local.forEach { queue.add(it) }

            end.remote.forEach {

            }


        }

        return segments
    }

    fun disassembleSegment(initialState: State, mapping: MutableMap<SnesAddress, Segment>): Segment {
        val instructions = ArrayList<Instruction>()
        var lastState = initialState

        val queue = ArrayDeque<State>()
        val seen = HashSet<SnesAddress>()

        fun finalize(segment: Segment): Segment {
            instructions.forEach {
                mapping[it.address] = segment
            }
            return segment
        }

        fun tryAdd(state: State) {
            if (seen.add(state.address)) {
                queue.add(state)
            }
        }
        tryAdd(initialState)

        while (queue.isNotEmpty()) {
            val state = queue.remove()

            val ins = disassembleInstruction(state)
            instructions.add(ins)
            println(ins)

            val segmentEnd = ins.opcode.ender(ins)

            if (segmentEnd != null) {

                return finalize(Segment(initialState.address, segmentEnd, instructions))
            }

            tryAdd(ins.postState)
            lastState = ins.postState
        }

        return finalize(Segment(initialState.address, continuationSegmentEnd(lastState), instructions))
    }

    private fun disassembleInstruction(state: State): Instruction {
        val opcodeValue = state.memory[state.address] ?: return unreadableInstruction(state)
        val opcode = Opcode.opcode(opcodeValue)
        val length = opcode.mode.instructionLength(state) ?: 1u
        val bytes = state.memory.range(state.address.value.toUInt(), length).validate()
            ?: return unreadableInstruction(state)
        return Instruction(bytes, opcode, state)
    }

    private fun unreadableInstruction(state: State) =
            Instruction(EmptyMemorySpace, Opcode.UNKNOWN_OPCODE, state)
}
