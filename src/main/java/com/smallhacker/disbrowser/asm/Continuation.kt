package com.smallhacker.disbrowser.asm

enum class Continuation(val shouldStop: Boolean) {
    CONTINUE(false),
    MAY_STOP(false),
    STOP(true),
    FATAL_ERROR(true),
    INSUFFICIENT_DATA(true),
}