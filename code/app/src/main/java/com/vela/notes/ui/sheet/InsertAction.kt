package com.vela.notes.ui.sheet

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

enum class InsertAction(
    val label: String,
    val icon: ImageVector,
) {
    SCAN("Quét tài liệu", Icons.Rounded.DocumentScanner),
    CAMERA("Chụp ảnh", Icons.Rounded.CameraAlt),
    PHOTOS("Chọn ảnh", Icons.Rounded.PhotoLibrary),
    RECORD_AUDIO("Ghi âm", Icons.Rounded.Mic),
    ATTACH_FILE("Đính kèm tệp", Icons.Rounded.AttachFile),
}
