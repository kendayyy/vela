package com.vela.notes.data.scanner

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.hypot

enum class ScanFilter {
    ORIGINAL,
    COLOR,
    GRAYSCALE,
    BLACK_AND_WHITE,
}

class ScanPageCorrector {
    fun correct(
        bitmap: Bitmap,
        quad: DocumentQuad,
        quarterTurns: Int,
        filter: ScanFilter,
    ): Bitmap {
        OpenCvRuntime.ensureLoaded()
        val source = if (bitmap.config == Bitmap.Config.ARGB_8888) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }
        val rgba = Mat()
        try {
            Utils.bitmapToMat(source, rgba)
            val warped = warp(rgba, quad)
            val rotated = rotate(warped, quarterTurns)
            applyFilter(rotated, filter)
            val output = Bitmap.createBitmap(rotated.cols(), rotated.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(rotated, output)
            rotated.release()
            return output
        } finally {
            rgba.release()
            if (source !== bitmap) source.recycle()
        }
    }

    private fun warp(rgba: Mat, quad: DocumentQuad): Mat {
        val width = max(
            quad.topLeft.distanceTo(quad.topRight),
            quad.bottomLeft.distanceTo(quad.bottomRight),
        ).toInt().coerceAtLeast(1)
        val height = max(
            quad.topLeft.distanceTo(quad.bottomLeft),
            quad.topRight.distanceTo(quad.bottomRight),
        ).toInt().coerceAtLeast(1)
        val source = MatOfPoint2f(
            quad.topLeft.toCv(),
            quad.topRight.toCv(),
            quad.bottomRight.toCv(),
            quad.bottomLeft.toCv(),
        )
        val destination = MatOfPoint2f(
            Point(0.0, 0.0),
            Point((width - 1).toDouble(), 0.0),
            Point((width - 1).toDouble(), (height - 1).toDouble()),
            Point(0.0, (height - 1).toDouble()),
        )
        val matrix = Imgproc.getPerspectiveTransform(source, destination)
        val warped = Mat()
        try {
            Imgproc.warpPerspective(rgba, warped, matrix, Size(width.toDouble(), height.toDouble()))
            return warped
        } finally {
            source.release()
            destination.release()
            matrix.release()
        }
    }

    private fun rotate(source: Mat, quarterTurns: Int): Mat {
        val turns = Math.floorMod(quarterTurns, 4)
        if (turns == 0) return source
        val code = when (turns) {
            1 -> Core.ROTATE_90_CLOCKWISE
            2 -> Core.ROTATE_180
            else -> Core.ROTATE_90_COUNTERCLOCKWISE
        }
        val rotated = Mat()
        Core.rotate(source, rotated, code)
        source.release()
        return rotated
    }

    private fun applyFilter(mat: Mat, filter: ScanFilter) {
        when (filter) {
            ScanFilter.ORIGINAL -> Unit
            ScanFilter.COLOR -> mat.convertTo(mat, -1, 1.25, -20.0)
            ScanFilter.GRAYSCALE -> {
                val gray = Mat()
                Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
                Imgproc.cvtColor(gray, mat, Imgproc.COLOR_GRAY2RGBA)
                gray.release()
            }
            ScanFilter.BLACK_AND_WHITE -> {
                val gray = Mat()
                Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
                Imgproc.adaptiveThreshold(
                    gray,
                    gray,
                    255.0,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY,
                    15,
                    8.0,
                )
                Imgproc.cvtColor(gray, mat, Imgproc.COLOR_GRAY2RGBA)
                gray.release()
            }
        }
    }
}

private fun ScanPoint.toCv(): Point = Point(x.toDouble(), y.toDouble())

private fun ScanPoint.distanceTo(other: ScanPoint): Float =
    hypot(x - other.x, y - other.y)
