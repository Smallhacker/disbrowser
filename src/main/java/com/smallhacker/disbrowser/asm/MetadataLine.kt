package com.smallhacker.disbrowser.asm

data class MetadataLine(
        val address: Address,
        var label: String? = null,
        var comment: String? = null,
        var preComment: String? = null,
        val flags: MutableList<InstructionFlag> = ArrayList()
)
