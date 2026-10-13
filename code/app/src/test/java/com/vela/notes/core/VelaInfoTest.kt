package com.vela.notes.core

import org.junit.Assert.assertEquals
import org.junit.Test

class VelaInfoTest {
    @Test
    fun appName_isStable() {
        assertEquals("Vela", VelaInfo.AppName)
    }
}
