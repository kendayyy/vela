package com.vela.notes.data.scanner

data class ImageFrame(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

fun defaultDocumentQuad(imageWidth: Int, imageHeight: Int): DocumentQuad {
    val insetX = imageWidth * 0.08f
    val insetY = imageHeight * 0.08f
    return DocumentQuad(
        topLeft = ScanPoint(insetX, insetY),
        topRight = ScanPoint(imageWidth - insetX, insetY),
        bottomRight = ScanPoint(imageWidth - insetX, imageHeight - insetY),
        bottomLeft = ScanPoint(insetX, imageHeight - insetY),
    )
}

fun clampToImage(x: Float, y: Float, imageWidth: Int, imageHeight: Int): ScanPoint = ScanPoint(
    x = x.coerceIn(0f, imageWidth.toFloat().coerceAtLeast(0f)),
    y = y.coerceIn(0f, imageHeight.toFloat().coerceAtLeast(0f)),
)

fun DocumentQuad.withCorner(index: Int, point: ScanPoint): DocumentQuad = when (index) {
    0 -> copy(topLeft = point)
    1 -> copy(topRight = point)
    2 -> copy(bottomRight = point)
    3 -> copy(bottomLeft = point)
    else -> this
}

fun fittedImageFrame(viewWidth: Float, viewHeight: Float, imageWidth: Int, imageHeight: Int): ImageFrame {
    if (viewWidth <= 0f || viewHeight <= 0f || imageWidth <= 0 || imageHeight <= 0) {
        return ImageFrame(0f, 0f, 0f, 0f)
    }
    val scale = minOf(viewWidth / imageWidth, viewHeight / imageHeight)
    val width = imageWidth * scale
    val height = imageHeight * scale
    return ImageFrame(
        left = (viewWidth - width) / 2f,
        top = (viewHeight - height) / 2f,
        width = width,
        height = height,
    )
}

fun ImageFrame.toImagePoint(screenX: Float, screenY: Float, imageWidth: Int, imageHeight: Int): ScanPoint {
    if (width <= 0f || height <= 0f) return ScanPoint(0f, 0f)
    val x = (screenX - left) / width * imageWidth
    val y = (screenY - top) / height * imageHeight
    return clampToImage(x, y, imageWidth, imageHeight)
}

fun ImageFrame.toScreenPoint(point: ScanPoint, imageWidth: Int, imageHeight: Int): ScanPoint {
    if (imageWidth <= 0 || imageHeight <= 0) return ScanPoint(left, top)
    return ScanPoint(
        x = left + point.x / imageWidth * width,
        y = top + point.y / imageHeight * height,
    )
}

fun magnifierSampleSquare(
    focus: ScanPoint,
    imageWidth: Int,
    imageHeight: Int,
    sampleSize: Int,
): ImageFrame {
    val size = sampleSize.coerceAtLeast(1)
    val left = (focus.x - size / 2f).coerceIn(0f, (imageWidth - size).coerceAtLeast(0).toFloat())
    val top = (focus.y - size / 2f).coerceIn(0f, (imageHeight - size).coerceAtLeast(0).toFloat())
    return ImageFrame(left, top, size.toFloat().coerceAtMost(imageWidth.toFloat()), size.toFloat().coerceAtMost(imageHeight.toFloat()))
}
