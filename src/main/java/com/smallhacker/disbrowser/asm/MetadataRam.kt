package com.smallhacker.disbrowser.asm

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class MetadataRam(
        var label: String? = null,
        var length: Int?
) {
    @JsonIgnore
    fun isEmpty() = (label == null) && (length == null)
}
