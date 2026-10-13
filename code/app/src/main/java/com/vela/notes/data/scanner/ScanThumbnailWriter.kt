package com.vela.notes.data.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ScanThumbnailWriter {
    const val MAX_EDGE_PX = 160

    fun write(source: File, destination: File) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        val sample = sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE_PX)
        val decoded = BitmapFactory.decodeFile(
            source.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        ) ?: error("Không thể tạo ảnh thu nhỏ.")
        val scale = MAX_EDGE_PX.toFloat() / max(decoded.width, decoded.height).coerceAtLeast(1)
        val width = (decoded.width * scale).toInt().coerceAtLeast(1)
        val height = (decoded.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (width == decoded.width && height == decoded.height) {
            decoded
        } else {
            Bitmap.createScaledBitmap(decoded, width, height, true)
        }
        try {
            FileOutputStream(destination).use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, output)
            }
        } finally {
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
        }
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (width / sample > maxEdge * 2 && height / sample > maxEdge * 2) sample *= 2
        return sample
    }
}
