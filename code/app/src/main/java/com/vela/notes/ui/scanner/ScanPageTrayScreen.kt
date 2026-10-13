package com.vela.notes.ui.scanner

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vela.notes.data.scanner.ScanFilter
import com.vela.notes.data.scanner.ScanPage
import com.vela.notes.ui.theme.VelaSize
import com.vela.notes.ui.theme.VelaSpacing

@Composable
fun ScanPageTrayScreen(
    pages: List<ScanPage>,
    limitMessage: String?,
    confirmDiscardLast: Boolean,
    onAddPage: () -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onDelete: (id: String) -> Unit,
    onRotate: (id: String) -> Unit,
    onFilter: (id: String, filter: ScanFilter) -> Unit,
    onConfirmDiscard: () -> Unit,
    onDismissDiscard: () -> Unit,
    onDone: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selected = pages.getOrNull(selectedIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0)))
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(VelaSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Đóng khay trang")
            }
            Text(
                "Các trang",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text("${pages.size} trang", style = MaterialTheme.typography.bodyMedium)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(VelaSpacing.Md),
            horizontalArrangement = Arrangement.spacedBy(VelaSpacing.Sm),
        ) {
            pages.forEachIndexed { index, page ->
                FilterChip(
                    selected = page.id == selected?.id,
                    onClick = { selectedIndex = index },
                    label = { Text("Trang ${index + 1}") },
                    modifier = Modifier.heightIn(min = VelaSize.MinimumTouchTarget),
                )
            }
        }
        limitMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = VelaSpacing.Md))
        }
        selected?.let { page ->
            Text(
                "Đang chọn trang ${pages.indexOfFirst { it.id == page.id } + 1}",
                modifier = Modifier.padding(horizontal = VelaSpacing.Md),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(VelaSpacing.Md),
                horizontalArrangement = Arrangement.spacedBy(VelaSpacing.Sm),
            ) {
                TextButton(
                    onClick = { onMove(pages.indexOfFirst { it.id == page.id }, pages.indexOfFirst { it.id == page.id } - 1) },
                    enabled = pages.indexOfFirst { it.id == page.id } > 0,
                    modifier = Modifier.heightIn(min = VelaSize.MinimumTouchTarget),
                ) { Text("Sang trái") }
                TextButton(
                    onClick = {
                        val index = pages.indexOfFirst { it.id == page.id }
                        onMove(index, index + 1)
                    },
                    enabled = pages.indexOfFirst { it.id == page.id } < pages.lastIndex,
                    modifier = Modifier.heightIn(min = VelaSize.MinimumTouchTarget),
                ) { Text("Sang phải") }
                IconButton(onClick = { onRotate(page.id) }) {
                    Icon(Icons.AutoMirrored.Rounded.RotateRight, contentDescription = "Xoay trang")
                }
                TextButton(
                    onClick = { onDelete(page.id) },
                    modifier = Modifier.heightIn(min = VelaSize.MinimumTouchTarget),
                ) { Text("Xóa trang") }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = VelaSpacing.Md),
                horizontalArrangement = Arrangement.spacedBy(VelaSpacing.Sm),
            ) {
                ScanFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = page.filter == filter,
                        onClick = { onFilter(page.id, filter) },
                        label = { Text(filter.trayLabel()) },
                        modifier = Modifier.heightIn(min = VelaSize.MinimumTouchTarget),
                    )
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(VelaSpacing.Md),
            horizontalArrangement = Arrangement.spacedBy(VelaSpacing.Sm),
        ) {
            OutlinedButton(onClick = onAddPage, modifier = Modifier.heightIn(min = VelaSize.MinimumTouchTarget)) {
                Text("Thêm trang")
            }
            Button(
                onClick = onDone,
                enabled = pages.isNotEmpty(),
                modifier = Modifier.weight(1f).heightIn(min = VelaSize.MinimumTouchTarget),
            ) { Text("Lưu PDF") }
        }
    }
    if (confirmDiscardLast) {
        AlertDialog(
            onDismissRequest = onDismissDiscard,
            title = { Text("Xóa trang cuối?") },
            text = { Text("Bản quét sẽ bị hủy.") },
            confirmButton = {
                TextButton(onClick = onConfirmDiscard) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDiscard) { Text("Hủy") }
            },
        )
    }
}

private fun ScanFilter.trayLabel(): String = when (this) {
    ScanFilter.ORIGINAL -> "Gốc"
    ScanFilter.COLOR -> "Màu"
    ScanFilter.GRAYSCALE -> "Xám"
    ScanFilter.BLACK_AND_WHITE -> "Đen trắng"
}
