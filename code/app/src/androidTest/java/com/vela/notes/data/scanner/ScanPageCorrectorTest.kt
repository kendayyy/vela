package com.vela.notes.data.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanPageCorrectorTest {
    private val corrector = ScanPageCorrector()

    @Test
    fun perspectiveCropKeepsTheDocumentColor() {
        val bitmap = coloredPage(Color.RED)
        val corrected = corrector.correct(bitmap, pageQuad(), 0, ScanFilter.ORIGINAL)
        val center = corrected.getPixel(corrected.width / 2, corrected.height / 2)

        assertTrue(corrected.width in 140..170)
        assertTrue(corrected.height in 100..130)
        assertTrue(Color.red(center) > 200)
        assertTrue(Color.green(center) < 40)
        bitmap.recycle()
        corrected.recycle()
    }

    @Test
    fun rotateSwapsTheCorrectedDimensions() {
        val bitmap = coloredPage(Color.WHITE)
        val upright = corrector.correct(bitmap, pageQuad(), 0, ScanFilter.ORIGINAL)
        val turned = corrector.correct(bitmap, pageQuad(), 1, ScanFilter.ORIGINAL)

        assertEquals(upright.width, turned.height)
        assertEquals(upright.height, turned.width)
        bitmap.recycle()
        upright.recycle()
        turned.recycle()
    }

    @Test
    fun grayscaleCollapsesChannelsAndColorShiftsMidtone() {
        val bitmap = coloredPage(Color.rgb(0, 180, 0))
        val gray = corrector.correct(bitmap, pageQuad(), 0, ScanFilter.GRAYSCALE)
        val pixel = gray.getPixel(gray.width / 2, gray.height / 2)

        assertEquals(Color.red(pixel), Color.green(pixel))
        assertEquals(Color.green(pixel), Color.blue(pixel))
        assertTrue(Color.red(pixel) > 80)

        val neutral = coloredPage(Color.rgb(128, 128, 128))
        val original = corrector.correct(neutral, pageQuad(), 0, ScanFilter.ORIGINAL)
        val color = corrector.correct(neutral, pageQuad(), 0, ScanFilter.COLOR)
        val originalTone = Color.red(original.getPixel(original.width / 2, original.height / 2))
        val colorTone = Color.red(color.getPixel(color.width / 2, color.height / 2))
        assertTrue(kotlin.math.abs(colorTone - originalTone) >= 8)
        bitmap.recycle()
        gray.recycle()
        neutral.recycle()
        original.recycle()
        color.recycle()
    }

    @Test
    fun blackAndWhiteBecomesBinary() {
        val bitmap = Bitmap.createBitmap(200, 160, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val paint = Paint()
        paint.color = Color.WHITE
        canvas.drawRect(20f, 20f, 180f, 140f, paint)
        paint.color = Color.BLACK
        canvas.drawRect(40f, 40f, 90f, 120f, paint)
        val corrected = corrector.correct(bitmap, pageQuad(), 0, ScanFilter.BLACK_AND_WHITE)
        var dark = 0
        var light = 0
        for (y in 0 until corrected.height step 4) {
            for (x in 0 until corrected.width step 4) {
                val tone = Color.red(corrected.getPixel(x, y))
                if (tone < 20) dark += 1
                if (tone > 235) light += 1
            }
        }
        assertTrue(dark > 0)
        assertTrue(light > 0)
        bitmap.recycle()
        corrected.recycle()
    }

    private fun pageQuad() = DocumentQuad(
        ScanPoint(20f, 20f),
        ScanPoint(180f, 20f),
        ScanPoint(180f, 140f),
        ScanPoint(20f, 140f),
    )

    private fun coloredPage(color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(200, 160, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val paint = Paint().apply { this.color = color }
        canvas.drawRect(20f, 20f, 180f, 140f, paint)
        return bitmap
    }
}
