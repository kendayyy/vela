package com.vela.notes.data.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.max

fun interface ScanDocumentExporter {
    fun export(pages: List<ScanPage>, destination: File)
}

class AndroidScanDocumentExporter @Inject constructor() : ScanDocumentExporter {
    override fun export(pages: List<ScanPage>, destination: File) {
        require(pages.isNotEmpty()) { "A scan document needs at least one page" }
        require(pages.size <= MAX_SCAN_PAGES) { "A scan document can contain at most $MAX_SCAN_PAGES pages" }
        val document = PdfDocument()
        val rendered = mutableListOf<Bitmap>()
        try {
            pages.forEachIndexed { index, page ->
                val bitmap = render(page)
                rendered += bitmap
                val info = PdfDocument.PageInfo.Builder(
                    bitmap.width.coerceAtLeast(1),
                    bitmap.height.coerceAtLeast(1),
                    index + 1,
                ).create()
                val pdfPage = document.startPage(info)
                pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                document.finishPage(pdfPage)
            }
            FileOutputStream(destination).use { output -> document.writeTo(output) }
        } catch (error: Throwable) {
            destination.delete()
            throw error
        } finally {
            document.close()
            rendered.forEach { bitmap -> if (!bitmap.isRecycled) bitmap.recycle() }
        }
    }

    private fun render(page: ScanPage): Bitmap {
        val decoded = BitmapFactory.decodeFile(
            page.imageFile.absolutePath,
            BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 },
        ) ?: error("Không thể mở trang quét.")
        val prepared = if (page.quarterTurns % 4 == 0 && page.filter == ScanFilter.ORIGINAL) {
            decoded
        } else {
            val corrected = ScanPageCorrector().correct(
                bitmap = decoded,
                quad = fullImageQuad(decoded.width, decoded.height),
                quarterTurns = page.quarterTurns,
                filter = page.filter,
            )
            if (corrected !== decoded && !decoded.isRecycled) decoded.recycle()
            corrected
        }
        return scaleDown(prepared, MAX_PDF_EDGE_PX)
    }

    private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private companion object {
        const val MAX_PDF_EDGE_PX = 1600
    }
}

fun fullImageQuad(width: Int, height: Int): DocumentQuad {
    val right = (width - 1).coerceAtLeast(0).toFloat()
    val bottom = (height - 1).coerceAtLeast(0).toFloat()
    return DocumentQuad(
        topLeft = ScanPoint(0f, 0f),
        topRight = ScanPoint(right, 0f),
        bottomRight = ScanPoint(right, bottom),
        bottomLeft = ScanPoint(0f, bottom),
    )
}
