package com.smallhacker.disbrowser.asm

import java.util.HashMap

import com.smallhacker.disbrowser.asm.Mnemonic.*
import com.smallhacker.disbrowser.asm.Mode.*
import com.smallhacker.disbrowser.util.OldUByte

typealias SegmentEnder = Instruction.() -> SegmentEnd?

class Opcode private constructor(val mnemonic: Mnemonic, val mode: Mode, val ender: SegmentEnder, val mutate: (Instruction) -> State) {
    private var _continuation = Continuation.YES
    private var _link = false
    private var _branch = false

    val operandIndex
        get() = if (mode.dataMode) 0u else 1u

    val continuation: Continuation
        get() = _continuation

    val link: Boolean
        get() = _link

    val branch: Boolean
        get() = _branch

    private fun stop(): Opcode {
        this._continuation = Continuation.NO
        return this
    }

    private fun mayStop(): Opcode {
        this._continuation = Continuation.MAYBE
        return this
    }

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
        val DATA_BYTE = Opcode(Mnemonic.DB, Mode.DATA_BYTE, { null }, { it.preState })
        val DATA_WORD = Opcode(Mnemonic.DW, Mode.DATA_WORD, { null }, { it.preState })
        val DATA_LONG = Opcode(Mnemonic.DL, Mode.DATA_LONG, { null }, { it.preState })

        val POINTER_WORD = Opcode(Mnemonic.DW, Mode.DATA_WORD, { null }, { it.preState }).linking()
        val POINTER_LONG = Opcode(Mnemonic.DL, Mode.DATA_LONG, { null }, { it.preState }).linking()

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


            add(0x00, BRK, IMMEDIATE_8, alwaysStop).stop()
            add(0x02, COP, IMMEDIATE_8, alwaysStop).stop()
            add(0x42, WDM, IMMEDIATE_8, alwaysStop).stop()

            add(0xEA, NOP, IMPLIED, alwaysContinue)

            add(0xDB, STP, IMPLIED, alwaysStop).stop()
            add(0xCB, WAI, IMPLIED, alwaysContinue)

            add(0x10, BPL, RELATIVE, branching).branching()
            add(0x30, BMI, RELATIVE, branching).branching()
            add(0x50, BVC, RELATIVE, branching).branching()
            add(0x70, BVS, RELATIVE, branching).branching()
            add(0x80, BRA, RELATIVE, alwaysBranching).stop().branching()
            add(0x90, BCC, RELATIVE, branching).branching()
            add(0xB0, BCS, RELATIVE, branching).branching()
            add(0xD0, BNE, RELATIVE, branching).branching()
            add(0xF0, BEQ, RELATIVE, branching).branching()
            add(0x82, BRL, RELATIVE_LONG, alwaysBranching).stop().branching()

            add(0x4C, JMP, ABSOLUTE, jumping).linking().stop()
            add(0x5C, JML, ABSOLUTE_LONG, jumping).linking().stop()
            add(0x6C, JMP, ABSOLUTE_INDIRECT, dynamicJumping).stop()
            add(0x7C, JMP, ABSOLUTE_X_INDIRECT, dynamicJumping).stop()
            add(0xDC, JMP, ABSOLUTE_INDIRECT_LONG, dynamicJumping).stop()

            add(0x22, JSL, ABSOLUTE_LONG, subJumping).linking().mayStop()
            add(0x20, JSR, ABSOLUTE, subJumping).linking().mayStop()
            add(0xFC, JSR, ABSOLUTE_X_INDIRECT, dynamicSubJumping).mayStop()

            add(0x60, RTS, IMPLIED, returning).stop()
            add(0x6B, RTL, IMPLIED, returning).stop()
            add(0x40, RTI, IMPLIED, returning).stop()

            add(0x1B, TCS, IMPLIED, alwaysContinue)
            add(0x3B, TSC, IMPLIED, alwaysContinue)
            add(0x5B, TCD, IMPLIED, alwaysContinue)
            add(0x7B, TDC, IMPLIED, alwaysContinue)
            add(0xAA, TAX, IMPLIED, alwaysContinue)
            add(0xA8, TAY, IMPLIED, alwaysContinue)
            add(0xBA, TSX, IMPLIED, alwaysContinue)
            add(0x8A, TXA, IMPLIED, alwaysContinue)
            add(0x9A, TXS, IMPLIED, alwaysContinue)
            add(0x9B, TXY, IMPLIED, alwaysContinue)
            add(0x98, TYA, IMPLIED, alwaysContinue)
            add(0xBB, TYX, IMPLIED, alwaysContinue)
            add(0xEB, XBA, IMPLIED, alwaysContinue)

            add(0x18, CLC, IMPLIED, alwaysContinue)
            add(0x38, SEC, IMPLIED, alwaysContinue)
            add(0x58, CLI, IMPLIED, alwaysContinue)
            add(0x78, SEI, IMPLIED, alwaysContinue)
            add(0xF8, SED, IMPLIED, alwaysContinue)
            add(0xD8, CLD, IMPLIED, alwaysContinue)
            add(0xB8, CLV, IMPLIED, alwaysContinue)
            add(0xE2, SEP, IMMEDIATE_8, alwaysContinue) { it.preState.sep(it.bytes[1u]) }
            add(0xC2, REP, IMMEDIATE_8, alwaysContinue) { it.preState.rep(it.bytes[1u]) }
            add(0xFB, XCE, IMPLIED, alwaysContinue)

            add(0xC1, CMP, DIRECT_X_INDIRECT, alwaysContinue)
            add(0xC3, CMP, DIRECT_S, alwaysContinue)
            add(0xC5, CMP, DIRECT, alwaysContinue)
            add(0xC7, CMP, DIRECT_INDIRECT_LONG, alwaysContinue)
            add(0xC9, CMP, IMMEDIATE_M, alwaysContinue)
            add(0xCD, CMP, ABSOLUTE, alwaysContinue)
            add(0xCF, CMP, ABSOLUTE_LONG, alwaysContinue)
            add(0xD1, CMP, DIRECT_INDIRECT_Y, alwaysContinue)
            add(0xD2, CMP, DIRECT_INDIRECT, alwaysContinue)
            add(0xD3, CMP, DIRECT_S_INDIRECT_Y, alwaysContinue)
            add(0xD5, CMP, DIRECT_X, alwaysContinue)
            add(0xD7, CMP, DIRECT_INDIRECT_LONG_Y, alwaysContinue)
            add(0xD9, CMP, ABSOLUTE_Y, alwaysContinue)
            add(0xDD, CMP, ABSOLUTE_X, alwaysContinue)
            add(0xDF, CMP, ABSOLUTE_LONG_X, alwaysContinue)
            add(0xE0, CPX, IMMEDIATE_X, alwaysContinue)
            add(0xE4, CPX, DIRECT, alwaysContinue)
            add(0xEC, CPX, ABSOLUTE, alwaysContinue)
            add(0xC0, CPY, IMMEDIATE_X, alwaysContinue)
            add(0xC4, CPY, DIRECT, alwaysContinue)
            add(0xCC, CPY, ABSOLUTE, alwaysContinue)

            add(0xA1, LDA, DIRECT_X_INDIRECT, alwaysContinue)
            add(0xA3, LDA, DIRECT_S, alwaysContinue)
            add(0xA5, LDA, DIRECT, alwaysContinue)
            add(0xA7, LDA, DIRECT_INDIRECT_LONG, alwaysContinue)
            add(0xA9, LDA, IMMEDIATE_M, alwaysContinue)
            add(0xAD, LDA, ABSOLUTE, alwaysContinue)
            add(0xAF, LDA, ABSOLUTE_LONG, alwaysContinue)
            add(0xB1, LDA, DIRECT_INDIRECT_Y, alwaysContinue)
            add(0xB2, LDA, DIRECT_INDIRECT, alwaysContinue)
            add(0xB3, LDA, DIRECT_S_INDIRECT_Y, alwaysContinue)
            add(0xB5, LDA, DIRECT_X, alwaysContinue)
            add(0xB7, LDA, DIRECT_INDIRECT_LONG_Y, alwaysContinue)
            add(0xB9, LDA, ABSOLUTE_Y, alwaysContinue)
            add(0xBD, LDA, ABSOLUTE_X, alwaysContinue)
            add(0xBF, LDA, ABSOLUTE_LONG_X, alwaysContinue)
            add(0xA2, LDX, IMMEDIATE_X, alwaysContinue)
            add(0xA6, LDX, DIRECT, alwaysContinue)
            add(0xAE, LDX, ABSOLUTE, alwaysContinue)
            add(0xB6, LDX, DIRECT_Y, alwaysContinue)
            add(0xBE, LDX, ABSOLUTE_Y, alwaysContinue)
            add(0xA0, LDY, IMMEDIATE_X, alwaysContinue)
            add(0xA4, LDY, DIRECT, alwaysContinue)
            add(0xAC, LDY, ABSOLUTE, alwaysContinue)
            add(0xB4, LDY, DIRECT_X, alwaysContinue)
            add(0xBC, LDY, ABSOLUTE_X, alwaysContinue)
            add(0x81, STA, DIRECT_X_INDIRECT, alwaysContinue)
            add(0x83, STA, DIRECT_S, alwaysContinue)
            add(0x85, STA, DIRECT, alwaysContinue)
            add(0x87, STA, DIRECT_INDIRECT_LONG, alwaysContinue)
            add(0x8D, STA, ABSOLUTE, alwaysContinue)
            add(0x8F, STA, ABSOLUTE_LONG, alwaysContinue)
            add(0x91, STA, DIRECT_INDIRECT_Y, alwaysContinue)
            add(0x92, STA, DIRECT_INDIRECT, alwaysContinue)
            add(0x93, STA, DIRECT_S_INDIRECT_Y, alwaysContinue)
            add(0x95, STA, DIRECT_X, alwaysContinue)
            add(0x97, STA, DIRECT_INDIRECT_LONG_Y, alwaysContinue)
            add(0x99, STA, ABSOLUTE_Y, alwaysContinue)
            add(0x9D, STA, ABSOLUTE_X, alwaysContinue)
            add(0x9F, STA, ABSOLUTE_LONG_X, alwaysContinue)
            add(0x86, STX, DIRECT, alwaysContinue)
            add(0x8E, STX, ABSOLUTE, alwaysContinue)
            add(0x96, STX, DIRECT_Y, alwaysContinue)
            add(0x84, STY, DIRECT, alwaysContinue)
            add(0x8C, STY, ABSOLUTE, alwaysContinue)
            add(0x94, STY, DIRECT_X, alwaysContinue)
            add(0x64, STZ, DIRECT, alwaysContinue)
            add(0x74, STZ, DIRECT_X, alwaysContinue)
            add(0x9C, STZ, ABSOLUTE, alwaysContinue)
            add(0x9E, STZ, ABSOLUTE_X, alwaysContinue)

            add(0x48, PHA, IMPLIED, alwaysContinue) { it.preState.pushUnknown(it.preState.mWidth) }
            add(0xDA, PHX, IMPLIED, alwaysContinue) { it.preState.pushUnknown(it.preState.xWidth) }
            add(0x5A, PHY, IMPLIED, alwaysContinue) { it.preState.pushUnknown(it.preState.xWidth) }
            add(0x68, PLA, IMPLIED, alwaysContinue) { it.preState.pull(it.preState.mWidth) }
            add(0xFA, PLX, IMPLIED, alwaysContinue) { it.preState.pull(it.preState.xWidth) }
            add(0x7A, PLY, IMPLIED, alwaysContinue) { it.preState.pull(it.preState.xWidth) }

            add(0x8B, PHB, IMPLIED, alwaysContinue) { it.preState.pushUnknown(1u) }
            add(0xAB, PLB, IMPLIED, alwaysContinue) { it.preState.pull(1u) }
            add(0x0B, PHD, IMPLIED, alwaysContinue) { it.preState.pushUnknown(1u) }
            add(0x2B, PLD, IMPLIED, alwaysContinue) { it.preState.pull(1u) }
            add(0x4B, PHK, IMPLIED, alwaysContinue) { it.preState.push((it.address.value shr 16).toUInt()) }
            add(0x08, PHP, IMPLIED, alwaysContinue) { it.preState.push(it.preState.flags) }
            add(0x28, PLP, IMPLIED, alwaysContinue) { it.preState.pull { copy(flags = it) } }

            add(0x3A, DEC, IMPLIED, alwaysContinue)
            add(0xC6, DEC, DIRECT, alwaysContinue)
            add(0xCE, DEC, ABSOLUTE, alwaysContinue)
            add(0xD6, DEC, DIRECT_X, alwaysContinue)
            add(0xDE, DEC, ABSOLUTE_X, alwaysContinue)
            add(0xCA, DEX, IMPLIED, alwaysContinue)
            add(0x88, DEY, IMPLIED, alwaysContinue)
            add(0x1A, INC, IMPLIED, alwaysContinue)
            add(0xE6, INC, DIRECT, alwaysContinue)
            add(0xEE, INC, ABSOLUTE, alwaysContinue)
            add(0xF6, INC, DIRECT_X, alwaysContinue)
            add(0xFE, INC, ABSOLUTE_X, alwaysContinue)
            add(0xE8, INX, IMPLIED, alwaysContinue)
            add(0xC8, INY, IMPLIED, alwaysContinue)

            add(0x06, ASL, DIRECT, alwaysContinue)
            add(0x0A, ASL, IMPLIED, alwaysContinue)
            add(0x0E, ASL, ABSOLUTE, alwaysContinue)
            add(0x16, ASL, DIRECT_X, alwaysContinue)
            add(0x1E, ASL, ABSOLUTE_X, alwaysContinue)
            add(0x46, LSR, DIRECT, alwaysContinue)
            add(0x4A, LSR, IMPLIED, alwaysContinue)
            add(0x4E, LSR, ABSOLUTE, alwaysContinue)
            add(0x56, LSR, DIRECT_X, alwaysContinue)
            add(0x5E, LSR, ABSOLUTE_X, alwaysContinue)
            add(0x26, ROL, DIRECT, alwaysContinue)
            add(0x2A, ROL, IMPLIED, alwaysContinue)
            add(0x2E, ROL, ABSOLUTE, alwaysContinue)
            add(0x36, ROL, DIRECT_X, alwaysContinue)
            add(0x3E, ROL, ABSOLUTE_X, alwaysContinue)
            add(0x66, ROR, DIRECT, alwaysContinue)
            add(0x6A, ROR, IMPLIED, alwaysContinue)
            add(0x6E, ROR, ABSOLUTE, alwaysContinue)
            add(0x76, ROR, DIRECT_X, alwaysContinue)
            add(0x7E, ROR, ABSOLUTE_X, alwaysContinue)

            add(0x61, ADC, DIRECT_X_INDIRECT, alwaysContinue)
            add(0x63, ADC, DIRECT_S, alwaysContinue)
            add(0x65, ADC, DIRECT, alwaysContinue)
            add(0x67, ADC, DIRECT_INDIRECT_LONG, alwaysContinue)
            add(0x69, ADC, IMMEDIATE_M, alwaysContinue)
            add(0x6D, ADC, ABSOLUTE, alwaysContinue)
            add(0x6F, ADC, ABSOLUTE_LONG, alwaysContinue)
            add(0x71, ADC, DIRECT_INDIRECT_Y, alwaysContinue)
            add(0x72, ADC, DIRECT_INDIRECT, alwaysContinue)
            add(0x73, ADC, DIRECT_S_INDIRECT_Y, alwaysContinue)
            add(0x75, ADC, DIRECT_X, alwaysContinue)
            add(0x77, ADC, DIRECT_INDIRECT_LONG_Y, alwaysContinue)
            add(0x79, ADC, ABSOLUTE_Y, alwaysContinue)
            add(0x7D, ADC, ABSOLUTE_X, alwaysContinue)
            add(0x7F, ADC, ABSOLUTE_LONG_X, alwaysContinue)
            add(0xE1, SBC, DIRECT_X_INDIRECT, alwaysContinue)
            add(0xE3, SBC, DIRECT_S, alwaysContinue)
            add(0xE5, SBC, DIRECT, alwaysContinue)
            add(0xE7, SBC, DIRECT_INDIRECT_LONG, alwaysContinue)
            add(0xE9, SBC, IMMEDIATE_M, alwaysContinue)
            add(0xED, SBC, ABSOLUTE, alwaysContinue)
            add(0xEF, SBC, ABSOLUTE_LONG, alwaysContinue)
            add(0xF1, SBC, DIRECT_INDIRECT_Y, alwaysContinue)
            add(0xF2, SBC, DIRECT_INDIRECT, alwaysContinue)
            add(0xF3, SBC, DIRECT_S_INDIRECT_Y, alwaysContinue)
            add(0xF5, SBC, DIRECT_X, alwaysContinue)
            add(0xF7, SBC, DIRECT_INDIRECT_LONG_Y, alwaysContinue)
            add(0xF9, SBC, ABSOLUTE_Y, alwaysContinue)
            add(0xFD, SBC, ABSOLUTE_X, alwaysContinue)
            add(0xFF, SBC, ABSOLUTE_LONG_X, alwaysContinue)

            add(0x21, AND, DIRECT_X_INDIRECT, alwaysContinue)
            add(0x23, AND, DIRECT_S, alwaysContinue)
            add(0x25, AND, DIRECT, alwaysContinue)
            add(0x27, AND, DIRECT_INDIRECT_LONG, alwaysContinue)
            add(0x29, AND, IMMEDIATE_M, alwaysContinue)
            add(0x2D, AND, ABSOLUTE, alwaysContinue)
            add(0x2F, AND, ABSOLUTE_LONG, alwaysContinue)
            add(0x31, AND, DIRECT_INDIRECT_Y, alwaysContinue)
            add(0x32, AND, DIRECT_INDIRECT, alwaysContinue)
            add(0x33, AND, DIRECT_S_INDIRECT_Y, alwaysContinue)
            add(0x35, AND, DIRECT_X, alwaysContinue)
            add(0x37, AND, DIRECT_INDIRECT_LONG_Y, alwaysContinue)
            add(0x39, AND, ABSOLUTE_Y, alwaysContinue)
            add(0x3D, AND, ABSOLUTE_X, alwaysContinue)
            add(0x3F, AND, ABSOLUTE_LONG_X, alwaysContinue)
            add(0x41, EOR, DIRECT_X_INDIRECT, alwaysContinue)
            add(0x43, EOR, DIRECT_S, alwaysContinue)
            add(0x45, EOR, DIRECT, alwaysContinue)
            add(0x47, EOR, DIRECT_INDIRECT_LONG, alwaysContinue)
            add(0x49, EOR, IMMEDIATE_M, alwaysContinue)
            add(0x4D, EOR, ABSOLUTE, alwaysContinue)
            add(0x4F, EOR, ABSOLUTE_LONG, alwaysContinue)
            add(0x51, EOR, DIRECT_INDIRECT_Y, alwaysContinue)
            add(0x52, EOR, DIRECT_INDIRECT, alwaysContinue)
            add(0x53, EOR, DIRECT_S_INDIRECT_Y, alwaysContinue)
            add(0x55, EOR, DIRECT_X, alwaysContinue)
            add(0x57, EOR, DIRECT_INDIRECT_LONG_Y, alwaysContinue)
            add(0x59, EOR, ABSOLUTE_Y, alwaysContinue)
            add(0x5D, EOR, ABSOLUTE_X, alwaysContinue)
            add(0x5F, EOR, ABSOLUTE_LONG_X, alwaysContinue)
            add(0x01, ORA, DIRECT_X_INDIRECT, alwaysContinue)
            add(0x03, ORA, DIRECT_S, alwaysContinue)
            add(0x05, ORA, DIRECT, alwaysContinue)
            add(0x07, ORA, DIRECT_INDIRECT_LONG, alwaysContinue)
            add(0x09, ORA, IMMEDIATE_M, alwaysContinue)
            add(0x0D, ORA, ABSOLUTE, alwaysContinue)
            add(0x0F, ORA, ABSOLUTE_LONG, alwaysContinue)
            add(0x11, ORA, DIRECT_INDIRECT_Y, alwaysContinue)
            add(0x12, ORA, DIRECT_INDIRECT, alwaysContinue)
            add(0x13, ORA, DIRECT_S_INDIRECT_Y, alwaysContinue)
            add(0x15, ORA, DIRECT_X, alwaysContinue)
            add(0x17, ORA, DIRECT_INDIRECT_LONG_Y, alwaysContinue)
            add(0x19, ORA, ABSOLUTE_Y, alwaysContinue)
            add(0x1D, ORA, ABSOLUTE_X, alwaysContinue)
            add(0x1F, ORA, ABSOLUTE_LONG_X, alwaysContinue)

            add(0x14, TRB, DIRECT, alwaysContinue)
            add(0x1C, TRB, ABSOLUTE, alwaysContinue)
            add(0x04, TSB, DIRECT, alwaysContinue)
            add(0x0C, TSB, ABSOLUTE, alwaysContinue)

            add(0x24, BIT, DIRECT, alwaysContinue)
            add(0x2C, BIT, ABSOLUTE, alwaysContinue)
            add(0x34, BIT, DIRECT_X, alwaysContinue)
            add(0x3C, BIT, ABSOLUTE_X, alwaysContinue)
            add(0x89, BIT, IMMEDIATE_M, alwaysContinue)

            add(0x54, MVN, BLOCK_MOVE, alwaysContinue)
            add(0x44, MVP, BLOCK_MOVE, alwaysContinue)

            add(0xF4, PEA, IMMEDIATE_16, alwaysContinue)
            add(0xD4, PEI, DIRECT, alwaysContinue)
            add(0x62, PER, RELATIVE_LONG, alwaysContinue)

            OPCODES = Array(256) { ocs[it]!! }
        }
    }
}