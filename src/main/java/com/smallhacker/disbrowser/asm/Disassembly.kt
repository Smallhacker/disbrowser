package com.smallhacker.disbrowser.asm

class Disassembly(lines: List<CodeUnit>) : Iterable<CodeUnit> {
    override fun iterator() = lineList.iterator() as Iterator<CodeUnit>

    private val knownAddresses = HashSet<Address>()
    private val lineList = ArrayList<CodeUnit>()

    init {
        lines.forEach {
            val address = it.address
            if (address != null) {
                knownAddresses += address
            }
            lineList.add(it)
        }
    }

    operator fun contains(address: Address) = address in knownAddresses
}