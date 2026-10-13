package com.vela.notes.data.scanner

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanDocumentExporterTest {
    private val exporter = AndroidScanDocumentExporter()

    @Test
    fun pdfPageCountMatchesTheScanAndPartialOutputIsRemoved() {
        val first = jpeg(Color.RED)
        val second = jpeg(Color.BLUE)
        val destination = File.createTempFile("vela-scan-doc-", ".pdf")
        exporter.export(
            listOf(
                ScanPage("a", first, first),
                ScanPage("b", second, second, quarterTurns = 1, filter = ScanFilter.GRAYSCALE),
            ),
            destination,
        )
        ParcelFileDescriptor.open(destination, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                assertEquals(2, renderer.pageCount)
            }
        }
        destination.delete()
        first.delete()
        second.delete()

        val broken = File.createTempFile("broken-page-", ".jpg")
        broken.writeBytes(byteArrayOf(1, 2, 3))
        val failed = File.createTempFile("failed-scan-", ".pdf")
        val error = runCatching {
            exporter.export(listOf(ScanPage("broken", broken, broken)), failed)
        }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        assertFalse(failed.exists())
        broken.delete()
    }

    private fun jpeg(color: Int): File {
        val bitmap = Bitmap.createBitmap(40, 60, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        val file = File.createTempFile("scan-page-", ".jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return file
    }
}
