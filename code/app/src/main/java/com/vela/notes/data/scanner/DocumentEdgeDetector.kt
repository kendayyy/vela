package com.vela.notes.data.scanner

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max

class DocumentEdgeDetector {
    fun detect(bitmap: Bitmap): DocumentQuad? {
        OpenCvRuntime.ensureLoaded()
        val scaled = scaleDown(bitmap)
        val scaleX = bitmap.width.toFloat() / scaled.width
        val scaleY = bitmap.height.toFloat() / scaled.height
        val rgba = Mat()
        try {
            Utils.bitmapToMat(scaled, rgba)
            val found = findQuad(rgba, equalize = false) ?: findQuad(rgba, equalize = true)
            return found?.scale(scaleX, scaleY)
        } finally {
            rgba.release()
            if (scaled !== bitmap) scaled.recycle()
        }
    }

    private fun findQuad(rgba: Mat, equalize: Boolean): DocumentQuad? {
        val gray = Mat()
        val blurred = Mat()
        val edges = Mat()
        val hierarchy = Mat()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        val contours = ArrayList<MatOfPoint>()
        try {
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            if (equalize) Imgproc.equalizeHist(gray, gray)
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            Imgproc.Canny(blurred, edges, 40.0, 120.0)
            Imgproc.dilate(edges, edges, kernel)
            Imgproc.findContours(
                edges,
                contours,
                hierarchy,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE,
            )
            var best: DocumentQuad? = null
            var bestArea = 0.0
            contours.forEach { contour ->
                val contour2f = MatOfPoint2f(*contour.toArray())
                val approx = MatOfPoint2f()
                try {
                    val perimeter = Imgproc.arcLength(contour2f, true)
                    if (perimeter <= 0.0) return@forEach
                    Imgproc.approxPolyDP(contour2f, approx, 0.02 * perimeter, true)
                    val polygon = approx.toArray()
                    if (polygon.size != 4) return@forEach
                    val quad = orderCorners(
                        polygon.map { ScanPoint(it.x.toFloat(), it.y.toFloat()) },
                    ) ?: return@forEach
                    if (!isValidDocumentQuad(quad, rgba.cols(), rgba.rows())) return@forEach
                    val area = quadArea(quad)
                    if (area > bestArea) {
                        bestArea = area
                        best = quad
                    }
                } finally {
                    contour2f.release()
                    approx.release()
                }
            }
            return best
        } finally {
            gray.release()
            blurred.release()
            edges.release()
            hierarchy.release()
            kernel.release()
            contours.forEach { it.release() }
        }
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longestEdge = max(bitmap.width, bitmap.height)
        if (longestEdge <= MAX_ANALYSIS_EDGE_PX) return bitmap
        val scale = MAX_ANALYSIS_EDGE_PX.toFloat() / longestEdge
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
