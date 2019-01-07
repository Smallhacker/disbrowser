package com.smallhacker.disbrowser.asm

class Segment (val start: Address, val end: SegmentEnd, val instructions: List<Instruction>)

class SegmentEnd(val address: Address, val local: List<State> = emptyList(), val remote: List<State> = emptyList(), val returnAddress: Address? = null, val returning: Boolean = false)

fun stoppingSegmentEnd(address: Address)
        = SegmentEnd(address)

fun branchingSegmentEnd(address: Address, continueState: State, branchState: State)
        = SegmentEnd(address, local = listOf(continueState, branchState))

fun alwaysBranchingSegmentEnd(address: Address, branchState: State)
        = SegmentEnd(address, local = listOf(branchState))

fun jumpSegmentEnd(address: Address, targetState: State)
        = SegmentEnd(address, remote = listOf(targetState))

fun subroutineSegmentEnd(address: Address, targetState: State, returnAddress: Address)
        = SegmentEnd(address, remote = listOf(targetState), returnAddress = returnAddress)

fun returnSegmentEnd(address: Address)
        = SegmentEnd(address, returning = true)

fun continuationSegmentEnd(state: State)
    = SegmentEnd(state.address, local = listOf(state))

fun Segment.toDisassembly() = Disassembly(instructions)

