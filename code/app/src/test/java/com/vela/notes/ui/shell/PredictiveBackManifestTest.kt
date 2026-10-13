package com.vela.notes.ui.shell

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictiveBackManifestTest {
    @Test
    fun applicationOptsIntoPredictiveBack() {
        val manifest = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        ).first { it.exists() }.readText()

        assertTrue(manifest.contains("android:enableOnBackInvokedCallback=\"true\""))
    }
}
