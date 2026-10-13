package com.vela.notes.data.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidScanOcrTest {
    private val ocr = AndroidScanOcr()

    @Test
    fun bundledLatinModelReadsAVietnameseFixtureAndIgnoresABlankPage() = runBlocking {
        val progress = mutableListOf<Pair<Int, Int>>()
        val labeled = page("Hà Nội")
        val blank = page(null)
        val text = ocr.recognize(listOf(labeled, blank)) { completed, total ->
            progress += completed to total
        }
        labeled.imageFile.delete()
        blank.imageFile.delete()

        assertTrue("OCR trả về: $text", text?.contains("Hà") == true || text?.contains("Ha") == true)
        assertTrue(progress.first() == 0 to 2)
        assertTrue(progress.last() == 2 to 2)

        val empty = ocr.recognize(listOf(page(null).also { it.imageFile.deleteOnExit() })) { _, _ -> }
        assertNull(empty)
    }

    private fun page(label: String?): ScanPage {
        val bitmap = Bitmap.createBitmap(900, 240, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        if (label != null) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 120f
            }
            canvas.drawText(label, 48f, 160f, paint)
        }
        val file = File.createTempFile("ocr-page-", ".png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return ScanPage(id = file.name, imageFile = file, thumbnailFile = file)
    }
}
