package com.smallhacker.disbrowser.asm

enum class Mnemonic(private val nameOverride: String? = null, val alternativeName: String? = null, val showLengthSuffix: Boolean = true) {
    ADC, AND, ASL, BCC(alternativeName = "BLT"), BCS(alternativeName = "BGE"), BEQ, BIT, BMI, BNE, BPL, BRA,
    BRK, BRL, BVC, BVS, CLC, CLD, CLI, CLV, CMP, COP, CPX,
    CPY, DEC, DEX, DEY, EOR, INC, INX, INY, JMP(showLengthSuffix = false), JML(showLengthSuffix = false), JSL(showLengthSuffix = false),
    JSR(showLengthSuffix = false), LDA, LDX, LDY, LSR, MVN, MVP, NOP, ORA, PEA, PEI,
    PER, PHA, PHB, PHD, PHK, PHP, PHX, PHY, PLA, PLB, PLD,
    PLP, PLX, PLY, REP, ROL, ROR, RTI, RTL, RTS, SBC, SEC,
    SED, SEI, SEP, STA, STP, STX, STY, STZ, TAX, TAY, TCD,
    TCS, TDC, TRB, TSB, TSC, TSX, TXA, TXS, TXY, TYA, TYX,
    WAI, WDM, XBA, XCE,

    DB(nameOverride = ".db"), DW(nameOverride = ".dw"), DL(nameOverride = ".dl"),
    UNKNOWN(nameOverride = "???");

    val displayName get() = nameOverride ?: name
}
