package com.smallhacker.disbrowser.asm

class Segment (val start: SnesAddress, val end: SegmentEnd, val instructions: List<Instruction>)

class SegmentEnd(val address: SnesAddress, val local: List<State> = emptyList(), val remote: List<State> = emptyList(), val returnAddress: SnesAddress? = null, val returning: Boolean = false)

fun stoppingSegmentEnd(address: SnesAddress)
        = SegmentEnd(address)

fun branchingSegmentEnd(address: SnesAddress, continueState: State, branchState: State)
        = SegmentEnd(address, local = listOf(continueState, branchState))

fun alwaysBranchingSegmentEnd(address: SnesAddress, branchState: State)
        = SegmentEnd(address, local = listOf(branchState))

fun jumpSegmentEnd(address: SnesAddress, targetState: State)
        = SegmentEnd(address, remote = listOf(targetState))

fun subroutineSegmentEnd(address: SnesAddress, targetState: State, returnAddress: SnesAddress)
        = SegmentEnd(address, remote = listOf(targetState), returnAddress = returnAddress)

fun returnSegmentEnd(address: SnesAddress)
        = SegmentEnd(address, returning = true)

fun continuationSegmentEnd(state: State)
    = SegmentEnd(state.address, local = listOf(state))

fun Segment.toDisassembly() = Disassembly(instructions)

