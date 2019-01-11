package com.smallhacker.disbrowser.asm

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class MetadataLine(
        var label: String? = null,
        var comment: String? = null,
        var preComment: String? = null,
        var length: Int? = null,
        val flags: MutableList<InstructionFlag> = ArrayList()
) {
    @JsonIgnore
    fun isEmpty() = (label == null) && (comment == null) && (preComment == null) && (length == 0) && (flags.isEmpty())
}
