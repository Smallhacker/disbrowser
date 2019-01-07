package com.smallhacker.disbrowser.asm

class Disassembly(lines: List<Instruction>) : Iterable<Instruction> {
    override fun iterator() = lines.values.iterator() as Iterator<Instruction>

    private val lines = LinkedHashMap<Address, Instruction>()

    init {
        lines.forEach { this.lines[it.address] = it }
    }

    operator fun contains(address: Address) = address in lines
}