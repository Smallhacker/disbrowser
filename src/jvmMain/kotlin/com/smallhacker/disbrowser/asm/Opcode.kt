package com.smallhacker.disbrowser.asm

import com.smallhacker.disbrowser.asm.Mnemonic.*

typealias SegmentEnder = Instruction.() -> SegmentEnd?

class Opcode private constructor(val mnemonic: Mnemonic, val mode: Mode, val ender: SegmentEnder, val mutate: (Instruction) -> State) {
    private var _link = false
    private var _branch = false

    val operandIndex
        get() = if (mode.dataMode) 0u else 1u

    var continuation: Continuation = Continuation.CONTINUE

    val link: Boolean
        get() = _link

    val branch: Boolean
        get() = _branch

    private fun insufficientData() = also { this.continuation = Continuation.INSUFFICIENT_DATA }

    private fun fatal() = also { this.continuation = Continuation.FATAL_ERROR }

    private fun stop() = also { this.continuation = Continuation.STOP }

    private fun mayStop() = also { this.continuation = Continuation.MAY_STOP }

    private fun linking(): Opcode {
        this._link = true
        return this
    }

    private fun branching(): Opcode {
        linking()
        this._branch = true
        return this
    }

    companion object {
        val DATA_BYTE = Opcode(Mnemonic.DB, DirectData.byte, { null }, { it.preState })
        val DATA_WORD = Opcode(Mnemonic.DW, DirectData.word, { null }, { it.preState })
        val DATA_LONG = Opcode(Mnemonic.DL, DirectData.long, { null }, { it.preState })
        val DATA_POINTER_WORD = Opcode(Mnemonic.DW, DataPointer.word, { null }, { it.preState }).linking()
        val DATA_POINTER_LONG = Opcode(Mnemonic.DL, DataPointer.long, { null }, { it.preState }).linking()
        val CODE_POINTER_WORD = Opcode(Mnemonic.DW, CodePointer.word, { null }, { it.preState }).linking()
        val CODE_POINTER_LONG = Opcode(Mnemonic.DL, CodePointer.long, { null }, { it.preState }).linking()

        val UNKNOWN_OPCODE: Opcode

        private val OPCODES: Array<Opcode>

        fun opcode(byteValue: UByte): Opcode {
            return OPCODES[byteValue.toInt()]
        }

        init {
            val ocs = HashMap<Int, Opcode>()

            fun add(value: Int, mnemonic: Mnemonic, mode: Mode, ender: SegmentEnder, mutate: (Instruction) -> State = { it.preState }): Opcode {
                val opcode = Opcode(mnemonic, mode, ender, mutate)
                ocs[value] = opcode
                return opcode
            }

            val alwaysContinue: SegmentEnder = { null }
            val alwaysStop: SegmentEnder = { stoppingSegmentEnd(address) }
            val branching: SegmentEnder = { branchingSegmentEnd(address, postState, linkedState!!) }
            val alwaysBranching: SegmentEnder = { alwaysBranchingSegmentEnd(address, linkedState!!) }
            val jumping: SegmentEnder = { jumpSegmentEnd(address, linkedState!!) }
            val dynamicJumping: SegmentEnder = { stoppingSegmentEnd(address) }
            val subJumping: SegmentEnder = { subroutineSegmentEnd(address, linkedState!!, postState.address) }
            val dynamicSubJumping: SegmentEnder = { stoppingSegmentEnd(address) }
            val returning: SegmentEnder = { returnSegmentEnd(address) }

            UNKNOWN_OPCODE = Opcode(UNKNOWN, Implied, alwaysStop, Instruction::preState).insufficientData()

            add(0x00, BRK, Immediate8, alwaysStop).fatal()
            add(0x02, COP, Immediate8, alwaysStop).fatal()
            add(0x42, WDM, Immediate8, alwaysStop).fatal()
            add(0xDB, STP, Implied, alwaysStop).fatal()

            add(0xEA, NOP, Implied, alwaysContinue)
            add(0xCB, WAI, Implied, alwaysContinue)

            add(0x10, BPL, Relative, branching).branching()
            add(0x30, BMI, Relative, branching).branching()
            add(0x50, BVC, Relative, branching).branching()
            add(0x70, BVS, Relative, branching).branching()
            add(0x80, BRA, Relative, alwaysBranching).stop().branching()
            add(0x90, BCC, Relative, branching).branching()
            add(0xB0, BCS, Relative, branching).branching()
            add(0xD0, BNE, Relative, branching).branching()
            add(0xF0, BEQ, Relative, branching).branching()
            add(0x82, BRL, RelativeLong, alwaysBranching).stop().branching()

            add(0x4C, JMP, AbsoluteCode, jumping).linking().stop()
            add(0x5C, JML, AbsoluteLong, jumping).linking().stop()
            add(0x6C, JMP, Absolute.indirect, dynamicJumping).stop()
            add(0x7C, JMP, Absolute.x.indirect, dynamicJumping).stop()
            add(0xDC, JMP, Absolute.indirectLong, dynamicJumping).stop()

            add(0x22, JSL, AbsoluteLong, subJumping).linking().mayStop()
            add(0x20, JSR, AbsoluteCode, subJumping).linking().mayStop()
            add(0xFC, JSR, Absolute.x.indirect, dynamicSubJumping).mayStop()

            add(0x60, RTS, Implied, returning).stop()
            add(0x6B, RTL, Implied, returning).stop()
            add(0x40, RTI, Implied, returning).stop()

            add(0x1B, TCS, Implied, alwaysContinue)
            add(0x3B, TSC, Implied, alwaysContinue)
            add(0x5B, TCD, Implied, alwaysContinue)
            add(0x7B, TDC, Implied, alwaysContinue)
            add(0xAA, TAX, Implied, alwaysContinue)
            add(0xA8, TAY, Implied, alwaysContinue)
            add(0xBA, TSX, Implied, alwaysContinue)
            add(0x8A, TXA, Implied, alwaysContinue)
            add(0x9A, TXS, Implied, alwaysContinue)
            add(0x9B, TXY, Implied, alwaysContinue)
            add(0x98, TYA, Implied, alwaysContinue)
            add(0xBB, TYX, Implied, alwaysContinue)
            add(0xEB, XBA, Implied, alwaysContinue)

            add(0x18, CLC, Implied, alwaysContinue)
            add(0x38, SEC, Implied, alwaysContinue)
            add(0x58, CLI, Implied, alwaysContinue)
            add(0x78, SEI, Implied, alwaysContinue)
            add(0xF8, SED, Implied, alwaysContinue)
            add(0xD8, CLD, Implied, alwaysContinue)
            add(0xB8, CLV, Implied, alwaysContinue)
            add(0xE2, SEP, Immediate8, alwaysContinue) { it.preState.sep(it.bytes[1u]) }
            add(0xC2, REP, Immediate8, alwaysContinue) { it.preState.rep(it.bytes[1u]) }
            add(0xFB, XCE, Implied, alwaysContinue)

            add(0xC1, CMP, Direct.x.indirect, alwaysContinue)
            add(0xC3, CMP, Direct.s, alwaysContinue)
            add(0xC5, CMP, Direct, alwaysContinue)
            add(0xC7, CMP, Direct.indirectLong, alwaysContinue)
            add(0xC9, CMP, ImmediateM, alwaysContinue)
            add(0xCD, CMP, Absolute, alwaysContinue)
            add(0xCF, CMP, AbsoluteLong, alwaysContinue)
            add(0xD1, CMP, Direct.indirect.y, alwaysContinue)
            add(0xD2, CMP, Direct.indirect, alwaysContinue)
            add(0xD3, CMP, Direct.s.indirect.y, alwaysContinue)
            add(0xD5, CMP, Direct.x, alwaysContinue)
            add(0xD7, CMP, Direct.indirectLong.y, alwaysContinue)
            add(0xD9, CMP, Absolute.y, alwaysContinue)
            add(0xDD, CMP, Absolute.x, alwaysContinue)
            add(0xDF, CMP, AbsoluteLong.x, alwaysContinue)
            add(0xE0, CPX, ImmediateX, alwaysContinue)
            add(0xE4, CPX, Direct, alwaysContinue)
            add(0xEC, CPX, Absolute, alwaysContinue)
            add(0xC0, CPY, ImmediateX, alwaysContinue)
            add(0xC4, CPY, Direct, alwaysContinue)
            add(0xCC, CPY, Absolute, alwaysContinue)

            add(0xA1, LDA, Direct.x.indirect, alwaysContinue)
            add(0xA3, LDA, Direct.s, alwaysContinue)
            add(0xA5, LDA, Direct, alwaysContinue)
            add(0xA7, LDA, Direct.indirectLong, alwaysContinue)
            add(0xA9, LDA, ImmediateM, alwaysContinue)
            add(0xAD, LDA, Absolute, alwaysContinue)
            add(0xAF, LDA, AbsoluteLong, alwaysContinue)
            add(0xB1, LDA, Direct.indirect.y, alwaysContinue)
            add(0xB2, LDA, Direct.indirect, alwaysContinue)
            add(0xB3, LDA, Direct.s.indirect.y, alwaysContinue)
            add(0xB5, LDA, Direct.x, alwaysContinue)
            add(0xB7, LDA, Direct.indirectLong.y, alwaysContinue)
            add(0xB9, LDA, Absolute.y, alwaysContinue)
            add(0xBD, LDA, Absolute.x, alwaysContinue)
            add(0xBF, LDA, AbsoluteLong.x, alwaysContinue)
            add(0xA2, LDX, ImmediateX, alwaysContinue)
            add(0xA6, LDX, Direct, alwaysContinue)
            add(0xAE, LDX, Absolute, alwaysContinue)
            add(0xB6, LDX, Direct.y, alwaysContinue)
            add(0xBE, LDX, Absolute.y, alwaysContinue)
            add(0xA0, LDY, ImmediateX, alwaysContinue)
            add(0xA4, LDY, Direct, alwaysContinue)
            add(0xAC, LDY, Absolute, alwaysContinue)
            add(0xB4, LDY, Direct.x, alwaysContinue)
            add(0xBC, LDY, Absolute.x, alwaysContinue)
            add(0x81, STA, Direct.x.indirect, alwaysContinue)
            add(0x83, STA, Direct.s, alwaysContinue)
            add(0x85, STA, Direct, alwaysContinue)
            add(0x87, STA, Direct.indirectLong, alwaysContinue)
            add(0x8D, STA, Absolute, alwaysContinue)
            add(0x8F, STA, AbsoluteLong, alwaysContinue)
            add(0x91, STA, Direct.indirect.y, alwaysContinue)
            add(0x92, STA, Direct.indirect, alwaysContinue)
            add(0x93, STA, Direct.s.indirect.y, alwaysContinue)
            add(0x95, STA, Direct.x, alwaysContinue)
            add(0x97, STA, Direct.indirectLong.y, alwaysContinue)
            add(0x99, STA, Absolute.y, alwaysContinue)
            add(0x9D, STA, Absolute.x, alwaysContinue)
            add(0x9F, STA, AbsoluteLong.x, alwaysContinue)
            add(0x86, STX, Direct, alwaysContinue)
            add(0x8E, STX, Absolute, alwaysContinue)
            add(0x96, STX, Direct.y, alwaysContinue)
            add(0x84, STY, Direct, alwaysContinue)
            add(0x8C, STY, Absolute, alwaysContinue)
            add(0x94, STY, Direct.x, alwaysContinue)
            add(0x64, STZ, Direct, alwaysContinue)
            add(0x74, STZ, Direct.x, alwaysContinue)
            add(0x9C, STZ, Absolute, alwaysContinue)
            add(0x9E, STZ, Absolute.x, alwaysContinue)

            add(0x48, PHA, Implied, alwaysContinue) { it.preState.pushUnknown(it.preState.mWidth) }
            add(0xDA, PHX, Implied, alwaysContinue) { it.preState.pushUnknown(it.preState.xWidth) }
            add(0x5A, PHY, Implied, alwaysContinue) { it.preState.pushUnknown(it.preState.xWidth) }
            add(0x68, PLA, Implied, alwaysContinue) { it.preState.pull(it.preState.mWidth) }
            add(0xFA, PLX, Implied, alwaysContinue) { it.preState.pull(it.preState.xWidth) }
            add(0x7A, PLY, Implied, alwaysContinue) { it.preState.pull(it.preState.xWidth) }

            add(0x8B, PHB, Implied, alwaysContinue) { it.preState.pushUnknown(1u) }
            add(0xAB, PLB, Implied, alwaysContinue) { it.preState.pull(1u) }
            add(0x0B, PHD, Implied, alwaysContinue) { it.preState.pushUnknown(1u) }
            add(0x2B, PLD, Implied, alwaysContinue) { it.preState.pull(1u) }
            add(0x4B, PHK, Implied, alwaysContinue) { it.preState.push((it.address.value shr 16).toUInt()) }
            add(0x08, PHP, Implied, alwaysContinue) { it.preState.push(it.preState.flags) }
            add(0x28, PLP, Implied, alwaysContinue) { it.preState.pull { copy(flags = it) } }

            add(0x3A, DEC, Implied, alwaysContinue)
            add(0xC6, DEC, Direct, alwaysContinue)
            add(0xCE, DEC, Absolute, alwaysContinue)
            add(0xD6, DEC, Direct.x, alwaysContinue)
            add(0xDE, DEC, Absolute.x, alwaysContinue)
            add(0xCA, DEX, Implied, alwaysContinue)
            add(0x88, DEY, Implied, alwaysContinue)
            add(0x1A, INC, Implied, alwaysContinue)
            add(0xE6, INC, Direct, alwaysContinue)
            add(0xEE, INC, Absolute, alwaysContinue)
            add(0xF6, INC, Direct.x, alwaysContinue)
            add(0xFE, INC, Absolute.x, alwaysContinue)
            add(0xE8, INX, Implied, alwaysContinue)
            add(0xC8, INY, Implied, alwaysContinue)

            add(0x06, ASL, Direct, alwaysContinue)
            add(0x0A, ASL, Implied, alwaysContinue)
            add(0x0E, ASL, Absolute, alwaysContinue)
            add(0x16, ASL, Direct.x, alwaysContinue)
            add(0x1E, ASL, Absolute.x, alwaysContinue)
            add(0x46, LSR, Direct, alwaysContinue)
            add(0x4A, LSR, Implied, alwaysContinue)
            add(0x4E, LSR, Absolute, alwaysContinue)
            add(0x56, LSR, Direct.x, alwaysContinue)
            add(0x5E, LSR, Absolute.x, alwaysContinue)
            add(0x26, ROL, Direct, alwaysContinue)
            add(0x2A, ROL, Implied, alwaysContinue)
            add(0x2E, ROL, Absolute, alwaysContinue)
            add(0x36, ROL, Direct.x, alwaysContinue)
            add(0x3E, ROL, Absolute.x, alwaysContinue)
            add(0x66, ROR, Direct, alwaysContinue)
            add(0x6A, ROR, Implied, alwaysContinue)
            add(0x6E, ROR, Absolute, alwaysContinue)
            add(0x76, ROR, Direct.x, alwaysContinue)
            add(0x7E, ROR, Absolute.x, alwaysContinue)

            add(0x61, ADC, Direct.x.indirect, alwaysContinue)
            add(0x63, ADC, Direct.s, alwaysContinue)
            add(0x65, ADC, Direct, alwaysContinue)
            add(0x67, ADC, Direct.indirectLong, alwaysContinue)
            add(0x69, ADC, ImmediateM, alwaysContinue)
            add(0x6D, ADC, Absolute, alwaysContinue)
            add(0x6F, ADC, AbsoluteLong, alwaysContinue)
            add(0x71, ADC, Direct.indirect.y, alwaysContinue)
            add(0x72, ADC, Direct.indirect, alwaysContinue)
            add(0x73, ADC, Direct.s.indirect.y, alwaysContinue)
            add(0x75, ADC, Direct.x, alwaysContinue)
            add(0x77, ADC, Direct.indirectLong.y, alwaysContinue)
            add(0x79, ADC, Absolute.y, alwaysContinue)
            add(0x7D, ADC, Absolute.x, alwaysContinue)
            add(0x7F, ADC, AbsoluteLong.x, alwaysContinue)
            add(0xE1, SBC, Direct.x.indirect, alwaysContinue)
            add(0xE3, SBC, Direct.s, alwaysContinue)
            add(0xE5, SBC, Direct, alwaysContinue)
            add(0xE7, SBC, Direct.indirectLong, alwaysContinue)
            add(0xE9, SBC, ImmediateM, alwaysContinue)
            add(0xED, SBC, Absolute, alwaysContinue)
            add(0xEF, SBC, AbsoluteLong, alwaysContinue)
            add(0xF1, SBC, Direct.indirect.y, alwaysContinue)
            add(0xF2, SBC, Direct.indirect, alwaysContinue)
            add(0xF3, SBC, Direct.s.indirect.y, alwaysContinue)
            add(0xF5, SBC, Direct.x, alwaysContinue)
            add(0xF7, SBC, Direct.indirectLong.y, alwaysContinue)
            add(0xF9, SBC, Absolute.y, alwaysContinue)
            add(0xFD, SBC, Absolute.x, alwaysContinue)
            add(0xFF, SBC, AbsoluteLong.x, alwaysContinue)

            add(0x21, AND, Direct.x.indirect, alwaysContinue)
            add(0x23, AND, Direct.s, alwaysContinue)
            add(0x25, AND, Direct, alwaysContinue)
            add(0x27, AND, Direct.indirectLong, alwaysContinue)
            add(0x29, AND, ImmediateM, alwaysContinue)
            add(0x2D, AND, Absolute, alwaysContinue)
            add(0x2F, AND, AbsoluteLong, alwaysContinue)
            add(0x31, AND, Direct.indirect.y, alwaysContinue)
            add(0x32, AND, Direct.indirect, alwaysContinue)
            add(0x33, AND, Direct.s.indirect.y, alwaysContinue)
            add(0x35, AND, Direct.x, alwaysContinue)
            add(0x37, AND, Direct.indirectLong.y, alwaysContinue)
            add(0x39, AND, Absolute.y, alwaysContinue)
            add(0x3D, AND, Absolute.x, alwaysContinue)
            add(0x3F, AND, AbsoluteLong.x, alwaysContinue)
            add(0x41, EOR, Direct.x.indirect, alwaysContinue)
            add(0x43, EOR, Direct.s, alwaysContinue)
            add(0x45, EOR, Direct, alwaysContinue)
            add(0x47, EOR, Direct.indirectLong, alwaysContinue)
            add(0x49, EOR, ImmediateM, alwaysContinue)
            add(0x4D, EOR, Absolute, alwaysContinue)
            add(0x4F, EOR, AbsoluteLong, alwaysContinue)
            add(0x51, EOR, Direct.indirect.y, alwaysContinue)
            add(0x52, EOR, Direct.indirect, alwaysContinue)
            add(0x53, EOR, Direct.s.indirect.y, alwaysContinue)
            add(0x55, EOR, Direct.x, alwaysContinue)
            add(0x57, EOR, Direct.indirectLong.y, alwaysContinue)
            add(0x59, EOR, Absolute.y, alwaysContinue)
            add(0x5D, EOR, Absolute.x, alwaysContinue)
            add(0x5F, EOR, AbsoluteLong.x, alwaysContinue)
            add(0x01, ORA, Direct.x.indirect, alwaysContinue)
            add(0x03, ORA, Direct.s, alwaysContinue)
            add(0x05, ORA, Direct, alwaysContinue)
            add(0x07, ORA, Direct.indirectLong, alwaysContinue)
            add(0x09, ORA, ImmediateM, alwaysContinue)
            add(0x0D, ORA, Absolute, alwaysContinue)
            add(0x0F, ORA, AbsoluteLong, alwaysContinue)
            add(0x11, ORA, Direct.indirect.y, alwaysContinue)
            add(0x12, ORA, Direct.indirect, alwaysContinue)
            add(0x13, ORA, Direct.s.indirect.y, alwaysContinue)
            add(0x15, ORA, Direct.x, alwaysContinue)
            add(0x17, ORA, Direct.indirectLong.y, alwaysContinue)
            add(0x19, ORA, Absolute.y, alwaysContinue)
            add(0x1D, ORA, Absolute.x, alwaysContinue)
            add(0x1F, ORA, AbsoluteLong.x, alwaysContinue)

            add(0x14, TRB, Direct, alwaysContinue)
            add(0x1C, TRB, Absolute, alwaysContinue)
            add(0x04, TSB, Direct, alwaysContinue)
            add(0x0C, TSB, Absolute, alwaysContinue)

            add(0x24, BIT, Direct, alwaysContinue)
            add(0x2C, BIT, Absolute, alwaysContinue)
            add(0x34, BIT, Direct.x, alwaysContinue)
            add(0x3C, BIT, Absolute.x, alwaysContinue)
            add(0x89, BIT, ImmediateM, alwaysContinue)

            add(0x54, MVN, BlockMove, alwaysContinue)
            add(0x44, MVP, BlockMove, alwaysContinue)

            add(0xF4, PEA, Immediate16, alwaysContinue)
            add(0xD4, PEI, Direct, alwaysContinue)
            add(0x62, PER, RelativeLong, alwaysContinue)

            OPCODES = Array(256) { ocs[it]!! }
        }
    }
}