package com.vela.notes.ui.collections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.ui.graphics.vector.ImageVector

enum class CollectionKind {
    ALL,
    PINNED,
    DELETED,
}

data class CollectionRowUi(
    val kind: CollectionKind,
    val title: String,
    val count: Int,
    val icon: ImageVector,
)

data class FolderRowUi(
    val id: String,
    val name: String,
    val count: Int,
)

val DefaultCollections = listOf(
    CollectionRowUi(CollectionKind.ALL, "Tất cả", 0, Icons.AutoMirrored.Rounded.Notes),
    CollectionRowUi(CollectionKind.PINNED, "Đã ghim", 0, Icons.Rounded.PushPin),
    CollectionRowUi(CollectionKind.DELETED, "Đã xóa", 0, Icons.Rounded.DeleteOutline),
)
