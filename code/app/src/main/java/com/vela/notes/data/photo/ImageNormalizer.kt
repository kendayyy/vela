package com.vela.notes.data.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.vela.notes.data.attachment.AttachmentSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

data class NormalizedPhoto(
    val displayName: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val source: AttachmentSource,
)

fun interface ImageNormalizer {
    suspend fun normalize(selection: PhotoSelection): NormalizedPhoto
}

class AndroidImageNormalizer @Inject constructor() : ImageNormalizer {
    override suspend fun normalize(selection: PhotoSelection): NormalizedPhoto = withContext(Dispatchers.Default) {
        try {
            normalizeImage(selection)
        } catch (error: InvalidImageException) {
            throw error
        } catch (error: Exception) {
            throw InvalidImageException(error)
        }
    }

    private fun normalizeImage(selection: PhotoSelection): NormalizedPhoto {
        val header = selection.source.open().use { input ->
            ByteArray(12).also { input.read(it) }
        }
        if (!hasSupportedImageHeader(header)) throw InvalidImageException()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        selection.source.open().use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw InvalidImageException()

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        }
        val decoded = selection.source.open().use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw InvalidImageException()
        val orientation = selection.source.open().use { input ->
            ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }
        val oriented = applyOrientation(decoded, orientation)
        if (oriented !== decoded) decoded.recycle()
        val scaled = scaleDown(oriented, MAX_DIMENSION)
        if (scaled !== oriented) oriented.recycle()

        return try {
            val output = ByteArrayOutputStream()
            if (!scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                throw IOException("Unable to encode normalized image")
            }
            val bytes = output.toByteArray()
            NormalizedPhoto(
                displayName = selection.displayName.substringBeforeLast('.', selection.displayName) + ".jpg",
                mimeType = "image/jpeg",
                width = scaled.width,
                height = scaled.height,
                source = AttachmentSource { ByteArrayInputStream(bytes) },
            )
        } finally {
            scaled.recycle()
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maximum: Int): Int {
        var sample = 1
        while (max(width / sample, height / sample) > maximum * 2) sample *= 2
        return sample
    }

    private fun hasSupportedImageHeader(bytes: ByteArray): Boolean {
        fun ascii(start: Int, value: String): Boolean =
            value.indices.all { index -> bytes.getOrNull(start + index)?.toInt() == value[index].code }
        return (bytes[0].toInt() and 0xff == 0xff && bytes[1].toInt() and 0xff == 0xd8) ||
            (bytes[0].toInt() and 0xff == 0x89 && ascii(1, "PNG")) ||
            ascii(0, "GIF8") ||
            (ascii(0, "RIFF") && ascii(8, "WEBP")) ||
            ascii(4, "ftyp")
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDown(bitmap: Bitmap, maximum: Int): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= maximum) return bitmap
        val scale = maximum.toFloat() / largest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private companion object {
        const val MAX_DIMENSION = 4_096
        const val JPEG_QUALITY = 90
    }
}

class InvalidImageException(cause: Throwable? = null) :
    IOException("The selected file is not a readable image", cause)
