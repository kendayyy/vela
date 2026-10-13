package com.vela.notes.core.time

fun interface VelaClock {
    fun nowMillis(): Long
}
