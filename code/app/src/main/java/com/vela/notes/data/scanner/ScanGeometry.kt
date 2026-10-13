package com.vela.notes.data.scanner

import kotlin.math.acos
import kotlin.math.hypot

data class ScanPoint(val x: Float, val y: Float)

data class DocumentQuad(
    val topLeft: ScanPoint,
    val topRight: ScanPoint,
    val bottomRight: ScanPoint,
    val bottomLeft: ScanPoint,
) {
    val corners: List<ScanPoint> = listOf(topLeft, topRight, bottomRight, bottomLeft)

    fun translate(dx: Float, dy: Float): DocumentQuad = DocumentQuad(
        topLeft = topLeft.offset(dx, dy),
        topRight = topRight.offset(dx, dy),
        bottomRight = bottomRight.offset(dx, dy),
        bottomLeft = bottomLeft.offset(dx, dy),
    )

    fun scale(scaleX: Float, scaleY: Float): DocumentQuad = DocumentQuad(
        topLeft = topLeft.scale(scaleX, scaleY),
        topRight = topRight.scale(scaleX, scaleY),
        bottomRight = bottomRight.scale(scaleX, scaleY),
        bottomLeft = bottomLeft.scale(scaleX, scaleY),
    )
}

internal const val MIN_DOCUMENT_AREA_RATIO = 0.10
internal const val MAX_DOCUMENT_AREA_RATIO = 0.97
internal const val MIN_EDGE_FRACTION = 0.12f
internal const val MIN_CORNER_ANGLE_DEGREES = 30f

fun orderCorners(points: List<ScanPoint>): DocumentQuad? {
    if (points.size != 4) return null
    val topLeft = points.minBy { it.x + it.y }
    val bottomRight = points.maxBy { it.x + it.y }
    val remaining = points.filter { it != topLeft && it != bottomRight }
    if (remaining.size != 2) return null
    val topRight = remaining.maxBy { it.x - it.y }
    val bottomLeft = remaining.minBy { it.x - it.y }
    return DocumentQuad(topLeft, topRight, bottomRight, bottomLeft)
}

fun isValidDocumentQuad(quad: DocumentQuad, imageWidth: Int, imageHeight: Int): Boolean {
    if (imageWidth <= 0 || imageHeight <= 0) return false
    if (quad.corners.any { !it.isInside(imageWidth, imageHeight) }) return false
    if (!isConvex(quad.corners)) return false
    val area = quadArea(quad)
    val imageArea = imageWidth.toDouble() * imageHeight.toDouble()
    val ratio = area / imageArea
    if (ratio < MIN_DOCUMENT_AREA_RATIO || ratio > MAX_DOCUMENT_AREA_RATIO) return false
    val shortestEdge = quad.corners.indices.minOf { index ->
        quad.corners[index].distanceTo(quad.corners[(index + 1) % 4])
    }
    val minEdge = MIN_EDGE_FRACTION * minOf(imageWidth, imageHeight)
    if (shortestEdge < minEdge) return false
    return minCornerAngleDegrees(quad) >= MIN_CORNER_ANGLE_DEGREES
}

fun quadArea(quad: DocumentQuad): Double {
    val corners = quad.corners
    var sum = 0.0
    for (index in corners.indices) {
        val current = corners[index]
        val next = corners[(index + 1) % corners.size]
        sum += current.x.toDouble() * next.y - next.x.toDouble() * current.y
    }
    return kotlin.math.abs(sum) / 2.0
}

fun maxCornerDrift(first: DocumentQuad, second: DocumentQuad): Float =
    first.corners.zip(second.corners).maxOf { (start, end) -> start.distanceTo(end) }

fun stabilityTolerance(imageWidth: Int, imageHeight: Int): Float =
    0.035f * hypot(imageWidth.toFloat(), imageHeight.toFloat())

private fun isConvex(corners: List<ScanPoint>): Boolean {
    var sign = 0
    for (index in corners.indices) {
        val start = corners[index]
        val middle = corners[(index + 1) % corners.size]
        val end = corners[(index + 2) % corners.size]
        val cross = (middle.x - start.x) * (end.y - middle.y) - (middle.y - start.y) * (end.x - middle.x)
        val current = when {
            cross > 1f -> 1
            cross < -1f -> -1
            else -> 0
        }
        if (current == 0) continue
        if (sign == 0) sign = current else if (sign != current) return false
    }
    return sign != 0
}

private fun minCornerAngleDegrees(quad: DocumentQuad): Float {
    val corners = quad.corners
    return corners.indices.minOf { index ->
        val previous = corners[(index + 3) % 4]
        val current = corners[index]
        val next = corners[(index + 1) % 4]
        val firstX = previous.x - current.x
        val firstY = previous.y - current.y
        val secondX = next.x - current.x
        val secondY = next.y - current.y
        val denominator = hypot(firstX, firstY) * hypot(secondX, secondY)
        if (denominator == 0f) return 0f
        val cosine = ((firstX * secondX + firstY * secondY) / denominator).coerceIn(-1f, 1f)
        Math.toDegrees(acos(cosine.toDouble())).toFloat()
    }
}

private fun ScanPoint.offset(dx: Float, dy: Float) = ScanPoint(x + dx, y + dy)

private fun ScanPoint.scale(scaleX: Float, scaleY: Float) = ScanPoint(x * scaleX, y * scaleY)

private fun ScanPoint.distanceTo(other: ScanPoint): Float = hypot(x - other.x, y - other.y)

private fun ScanPoint.isInside(imageWidth: Int, imageHeight: Int): Boolean =
    x >= -2f && y >= -2f && x <= imageWidth + 2f && y <= imageHeight + 2f
