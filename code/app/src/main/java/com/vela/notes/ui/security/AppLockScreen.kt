package com.vela.notes.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vela.notes.ui.theme.VelaSpacing

@Composable
fun AppLockScreen(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(VelaSpacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Vela đang được khóa",
                modifier = Modifier.padding(top = VelaSpacing.Lg),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Xác thực bằng khóa màn hình của thiết bị để tiếp tục.",
                modifier = Modifier.padding(top = VelaSpacing.Sm),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onUnlock,
                modifier = Modifier
                    .padding(top = VelaSpacing.Xl)
                    .semantics { contentDescription = "Mở khóa Vela" },
            ) {
                Text("Mở khóa")
            }
        }
    }
}
