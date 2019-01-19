package com.smallhacker.disbrowser.disassembler

import com.smallhacker.disbrowser.asm.*
import com.smallhacker.disbrowser.game.*
import com.smallhacker.disbrowser.util.LifoQueue
import com.smallhacker.disbrowser.util.mutableMultiMap
import com.smallhacker.disbrowser.util.putSingle
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object Disassembler {
    fun disassemble(initialState: State, gameData: GameData, global: Boolean): Disassembly {
        val seen = HashSet<SnesAddress>()
        val queue = LifoQueue<State>()
        val origins = mutableMultiMap<SnesAddress, SnesAddress>()
        val instructionMap = HashMap<SnesAddress, MutableInstruction>()

        fun tryAdd(state: State, origin: SnesAddress?) {
            if (origin != null) {
                origins.putSingle(state.address, origin)
            }
            if (seen.add(state.address)) {
                queue.add(state)
            }
        }
        tryAdd(initialState, null)

        val instructions = ArrayList<CodeUnit>()
        while (queue.isNotEmpty()) {
            val state = queue.removeNext()

            val ins = disassembleInstruction(state)
            instructions.add(ins)
            instructionMap[ins.address] = ins

            if (ins.opcode.mode.instructionLength(state) == null) {
                ins.continuation = Continuation.INSUFFICIENT_DATA
            }

            val linkedState = ins.linkedState

            val localAddress = ins.address
            val remoteAddress = linkedState?.address

            val localFlags = gameData.flagsAt(localAddress)
            val remoteFlags = gameData.flagsAt(remoteAddress)

            val pointerTableEntries = localFlags.findFlag<PointerTableLength>()?.entries

            localFlags.forFlag<JmpIndirectLongInterleavedTable> {
                ins.continuation = Continuation.STOP

                if (global) {
                    readTable(state.memory)
                            .filterNotNull()
                            .map { ins.postState.copy(address = it) }
                            .forEach { tryAdd(it, ins.address) }
                }

                generatePointerTable(ins).forEach {
                    instructions.add(it)
                }
            }

            remoteFlags.forFlag<DynamicJumpRoutine> {
                ins.continuation = Continuation.STOP

                if (pointerTableEntries != null) {
                    if (global) {
                        readTable(ins.postState, pointerTableEntries)
                                .filterNotNull()
                                .map { ins.postState.copy(address = it) }
                                .forEach { tryAdd(it, ins.address) }
                    }
                }

                generatePointerTable(ins, pointerTableEntries?.toUInt()).forEach {
                    instructions.add(it)
                }

            }

            remoteFlags.forFlag<NonReturningRoutine> {
                ins.continuation = Continuation.STOP
            }


            if (!ins.continuation.shouldStop) {
                tryAdd(ins.postState, ins.address)
            }


            if (linkedState != null) {
                if (ins.opcode.branch || global) {
                    tryAdd(linkedState, ins.address)
                }
            }
        }

        val fatalSeen = HashSet<SnesAddress>()
        val fatalQueue = LifoQueue<SnesAddress>()
        fun tryAddFatal(snesAddress: SnesAddress) {
            if (fatalSeen.add(snesAddress)) {
                fatalQueue.add(snesAddress)
            }
        }

        instructions.asSequence()
                .filterIsInstance<Instruction>()
                .filter { it.continuation == Continuation.FATAL_ERROR }
                .forEach { tryAddFatal(it.address) }

        while (fatalQueue.isNotEmpty()) {
            val badAddress = fatalQueue.removeNext()
            val instruction = instructionMap[badAddress] ?: continue
            val mnemonic = instruction.opcode.mnemonic
            if (mnemonic == Mnemonic.JSL || mnemonic == Mnemonic.JSR) continue
            instruction.certainty = Certainty.PROBABLY_WRONG
            origins[badAddress]?.forEach{tryAddFatal(it)}
        }

        val instructionList = instructions
                .sortedBy { it.sortedAddress }
                .toList()

        return Disassembly(instructionList)
    }

    fun disassembleSegments(initialState: State): List<Segment> {
        val mapping = HashMap<SnesAddress, Segment>()
        val queue = LifoQueue<State>()

        val segments = ArrayList<Segment>()

        fun tryAdd(state: State) {
            if (!mapping.containsKey(state.address)) {
                queue.add(state)
            }
        }
        tryAdd(initialState)


        while (queue.isNotEmpty()) {
            val state = queue.removeNext()

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
                queue.add(it)
            }
        }

        return segments
    }

    fun disassembleSegment(initialState: State, mapping: MutableMap<SnesAddress, Segment>): Segment {
        val instructions = ArrayList<Instruction>()
        var lastState = initialState

        val queue = LifoQueue<State>()
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
            val state = queue.removeNext()

            val ins = disassembleInstruction(state)
            instructions.add(ins)
            //println(ins)

            val segmentEnd = ins.opcode.ender(ins)

            if (segmentEnd != null) {
                return finalize(Segment(initialState.address, segmentEnd, instructions))
            }

            tryAdd(ins.postState)
            lastState = ins.postState
        }

        return finalize(Segment(initialState.address, continuationSegmentEnd(lastState), instructions))
    }

    private fun disassembleInstruction(state: State): MutableInstruction {
        val opcodeValue = state.memory[state.address] ?: return unreadableInstruction(state)
        val opcode = Opcode.opcode(opcodeValue)
        val length = opcode.mode.instructionLength(state) ?: 1u
        val bytes = state.memory.range(state.address.value.toUInt(), length).validate()
                ?: return unreadableInstruction(state)
        val continuation = opcode.continuation
        val certainty = Certainty.PROBABLY_CORRECT
        return MutableInstruction(bytes, opcode, state, continuation, certainty)
    }

    private fun unreadableInstruction(state: State) =
            MutableInstruction(EmptyMemorySpace, Opcode.UNKNOWN_OPCODE, state, Continuation.INSUFFICIENT_DATA, Certainty.PROBABLY_WRONG)
}
