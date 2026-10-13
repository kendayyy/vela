package com.vela.notes.data.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class ScanOcrRunner<T>(
    private val recognizePage: suspend (T) -> String,
) {
    suspend fun recognize(
        pages: List<T>,
        onProgress: (completed: Int, total: Int) -> Unit,
    ): String? {
        if (pages.isEmpty()) return null
        onProgress(0, pages.size)
        val parts = mutableListOf<String>()
        pages.forEachIndexed { index, page ->
            currentCoroutineContext().ensureActive()
            val text = recognizePage(page).trim()
            if (text.isNotEmpty()) parts += text
            onProgress(index + 1, pages.size)
        }
        return parts.joinToString(separator = "\n").ifBlank { null }
    }
}

fun interface ScanOcr {
    suspend fun recognize(
        pages: List<ScanPage>,
        onProgress: (completed: Int, total: Int) -> Unit,
    ): String?
}

class AndroidScanOcr @Inject constructor() : ScanOcr {
    override suspend fun recognize(
        pages: List<ScanPage>,
        onProgress: (completed: Int, total: Int) -> Unit,
    ): String? = withContext(Dispatchers.Default) {
        val bitmaps = pages.map { page ->
            BitmapFactory.decodeFile(
                page.imageFile.absolutePath,
                BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 },
            ) ?: error("Không thể mở trang để nhận dạng.")
        }
        try {
            ScanOcrRunner { bitmap: Bitmap -> recognizeBitmap(bitmap) }.recognize(bitmaps, onProgress)
        } finally {
            bitmaps.forEach { bitmap -> if (!bitmap.isRecycled) bitmap.recycle() }
        }
    }

    private suspend fun recognizeBitmap(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnCompleteListener { task ->
                recognizer.close()
                if (!continuation.isActive) return@addOnCompleteListener
                val failure = task.exception
                if (failure != null) continuation.resumeWithException(failure)
                else continuation.resume(task.result.text)
            }
        continuation.invokeOnCancellation { recognizer.close() }
    }
}
