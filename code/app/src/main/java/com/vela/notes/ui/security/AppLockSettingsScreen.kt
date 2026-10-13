package com.vela.notes.ui.security

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vela.notes.data.preferences.AppLockSettings
import com.vela.notes.data.preferences.AppLockTimeout
import com.vela.notes.ui.theme.VelaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSettingsScreen(
    settings: AppLockSettings,
    isAuthenticationAvailable: Boolean,
    onBack: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onTimeoutChange: (AppLockTimeout) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTimeoutSheet by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Quay lại Ghi chú")
                    }
                },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ListItem(
                headlineContent = { Text("Khóa ứng dụng") },
                supportingContent = {
                    Text(
                        if (settings.enabled) {
                            "Yêu cầu xác thực khi bạn quay lại Vela."
                        } else {
                            "Bảo vệ ghi chú khi thiết bị đang được mở khóa."
                        },
                    )
                },
                leadingContent = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = settings.enabled,
                        onCheckedChange = { enabled ->
                            if (enabled && isAuthenticationAvailable) onEnable() else if (!enabled) onDisable()
                        },
                        modifier = Modifier.semantics { contentDescription = "Bật khóa ứng dụng" },
                    )
                },
            )
            if (!isAuthenticationAvailable) {
                Text(
                    text = "Hãy thiết lập khóa màn hình Android trước khi bật tính năng này.",
                    modifier = Modifier.padding(horizontal = VelaSpacing.Xl, vertical = VelaSpacing.Sm),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (settings.enabled) {
                ListItem(
                    headlineContent = { Text("Khóa sau") },
                    supportingContent = { Text(settings.timeout.label) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimeoutSheet = true }
                        .semantics { contentDescription = "Thời gian khóa: ${settings.timeout.label}" },
                )
            }
            Text(
                text = "Khi bật, Vela sẽ chặn ảnh chụp màn hình, quay màn hình và preview trong màn hình đa nhiệm. Dữ liệu lưu trên thiết bị không được mã hóa thêm.",
                modifier = Modifier.padding(VelaSpacing.Xl),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (showTimeoutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTimeoutSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Text(
                text = "Khóa sau",
                modifier = Modifier.padding(horizontal = VelaSpacing.Xl, vertical = VelaSpacing.Md),
                style = MaterialTheme.typography.titleMedium,
            )
            AppLockTimeout.entries.forEach { timeout ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onTimeoutChange(timeout)
                            showTimeoutSheet = false
                        }
                        .padding(horizontal = VelaSpacing.Xl, vertical = VelaSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = settings.timeout == timeout, onClick = null)
                    Text(timeout.label, modifier = Modifier.padding(start = VelaSpacing.Md))
                }
            }
        }
    }
}
