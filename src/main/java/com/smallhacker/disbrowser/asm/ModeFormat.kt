package com.smallhacker.disbrowser.asm

abstract class ModeFormat {
    abstract fun print(instruction: Instruction, metadata: Metadata): String

    //fun wrap(prefix: String = "", suffix: String = ""): ModeFormat {
    //
    //}
}

//private class AddressModeFormat: ModeFormat() {
//    override fun print(instruction: Instruction, metadata: Metadata): String {
//        val mode = instruction.opcode.mode
//        val operandLength = mode.operandLength(instruction.preState)
//        instruction.
//
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//}