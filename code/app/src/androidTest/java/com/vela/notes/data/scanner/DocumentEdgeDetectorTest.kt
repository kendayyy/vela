package com.vela.notes.data.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Random
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentEdgeDetectorTest {
    private val detector = DocumentEdgeDetector()

    @Test
    fun noEdgeSceneReturnsNoQuad() {
        val bitmap = scene(640, 480) { canvas, _ ->
            canvas.drawColor(Color.rgb(70, 90, 110))
            val paint = Paint().apply { color = Color.rgb(40, 60, 80) }
            canvas.drawCircle(180f, 160f, 90f, paint)
            canvas.drawCircle(460f, 300f, 120f, paint)
        }

        assertNull(detector.detect(bitmap))
        bitmap.recycle()
    }

    @Test
    fun detectsSkewedDocument() {
        val bitmap = scene(640, 480) { canvas, paint ->
            canvas.drawColor(Color.rgb(20, 24, 28))
            paint.color = Color.rgb(236, 232, 220)
            canvas.drawPath(trapezoid(), paint)
        }

        val quad = detector.detect(bitmap)
        assertNotNull(quad)
        assertTrue(isValidDocumentQuad(quad!!, bitmap.width, bitmap.height))
        bitmap.recycle()
    }

    @Test
    fun detectsDocumentOnNoisyBackground() {
        val bitmap = scene(640, 480) { canvas, paint ->
            canvas.drawColor(Color.rgb(18, 18, 18))
            val noise = Paint()
            val random = Random(22)
            repeat(400) {
                noise.color = Color.rgb(random.nextInt(80), random.nextInt(80), random.nextInt(80))
                canvas.drawCircle(random.nextInt(640).toFloat(), random.nextInt(480).toFloat(), 6f, noise)
            }
            paint.color = Color.WHITE
            canvas.drawPath(trapezoid(), paint)
        }

        assertNotNull(detector.detect(bitmap))
        bitmap.recycle()
    }

    @Test
    fun detectsRotatedDocument() {
        val bitmap = scene(640, 480) { canvas, paint ->
            canvas.drawColor(Color.rgb(16, 32, 28))
            paint.color = Color.rgb(230, 226, 214)
            canvas.save()
            canvas.rotate(18f, 320f, 240f)
            canvas.drawRect(180f, 90f, 460f, 390f, paint)
            canvas.restore()
        }

        assertNotNull(detector.detect(bitmap))
        bitmap.recycle()
    }

    @Test
    fun detectsLowLightDocument() {
        val bitmap = scene(640, 480) { canvas, paint ->
            canvas.drawColor(Color.rgb(10, 10, 12))
            paint.color = Color.rgb(42, 40, 36)
            canvas.drawPath(trapezoid(), paint)
        }

        assertNotNull(detector.detect(bitmap))
        bitmap.recycle()
    }

    private fun trapezoid(): Path = Path().apply {
        moveTo(120f, 70f)
        lineTo(520f, 90f)
        lineTo(490f, 400f)
        lineTo(150f, 420f)
        close()
    }

    private fun scene(
        width: Int,
        height: Int,
        draw: (Canvas, Paint) -> Unit,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), Paint(Paint.ANTI_ALIAS_FLAG))
        return bitmap
    }
}
