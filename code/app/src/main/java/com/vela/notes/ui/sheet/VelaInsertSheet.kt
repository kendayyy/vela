package com.vela.notes.ui.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vela.notes.ui.theme.VelaSize
import com.vela.notes.ui.theme.VelaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VelaInsertSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAction: (InsertAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = VelaSpacing.Md),
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.outlineVariant,
            ) {
                androidx.compose.foundation.layout.Box(
                    Modifier.size(width = 36.dp, height = 4.dp),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = VelaSpacing.Sm),
        ) {
            Text(
                text = "Thêm vào ghi chú",
                modifier = Modifier.padding(
                    start = VelaSpacing.Xl,
                    end = VelaSpacing.Xl,
                    bottom = VelaSpacing.Sm,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            InsertAction.entries.forEach { action ->
                InsertActionRow(
                    action = action,
                    onClick = {
                        onAction(action)
                        onDismiss()
                    },
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = VelaSpacing.Sm),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = VelaSize.MinimumTouchTarget)
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .semantics { contentDescription = "Hủy thêm nội dung" }
                    .padding(horizontal = VelaSpacing.Xl, vertical = VelaSpacing.Md),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Hủy",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun InsertActionRow(
    action: InsertAction,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = action.label }
            .padding(horizontal = VelaSpacing.Xl, vertical = VelaSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VelaSpacing.Lg),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.padding(VelaSpacing.Sm).size(VelaSize.Icon),
            )
        }
        Text(
            text = action.label,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
