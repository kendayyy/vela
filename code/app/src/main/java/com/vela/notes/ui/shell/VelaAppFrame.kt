package com.vela.notes.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.vela.notes.ui.theme.VelaSize
import com.vela.notes.ui.theme.VelaSpacing
import com.vela.notes.ui.theme.ensureTouchTarget

@Composable
fun VelaAppFrame(
    focusState: FocusCapsuleState,
    onCapsuleClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val statusTopPx = WindowInsets.statusBars.getTop(density)
    var capsuleBottomPx by remember { mutableIntStateOf(0) }
    val capsuleVisible = focusState !is FocusCapsuleState.Hidden
    val contentTop = with(density) {
        if (capsuleVisible) (capsuleBottomPx - statusTopPx).coerceAtLeast(0).toDp() else 0.dp
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal))
                .padding(top = contentTop),
        ) {
            content()
        }

        AnimatedVisibility(
            visible = capsuleVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            FocusCapsule(
                state = focusState,
                onClick = onCapsuleClick,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = VelaSpacing.Sm)
                    .onGloballyPositioned { coordinates ->
                        capsuleBottomPx = coordinates.boundsInRoot().bottom.roundToInt()
                    },
            )
        }
    }
}

@Composable
private fun FocusCapsule(
    state: FocusCapsuleState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val presentation = state.toPresentation() ?: return
    val containerColor = if (presentation.isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondary
    }
    val contentColor = if (presentation.isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondary
    }

    val actionable = onClick != null
    Surface(
        modifier = modifier
            .then(if (actionable) Modifier.ensureTouchTarget() else Modifier)
            .testTag(FOCUS_CAPSULE_TEST_TAG)
            .heightIn(min = if (actionable) VelaSize.MinimumTouchTarget else VelaSize.FocusCapsuleHeight)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .semantics {
                liveRegion = LiveRegionMode.Polite
                if (state is FocusCapsuleState.Ocr) contentDescription = "Hủy nhận dạng"
            },
        shape = RoundedCornerShape(percent = 50),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = VelaSpacing.Lg, vertical = VelaSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VelaSpacing.Sm),
        ) {
            CapsuleIndicator(state, presentation.progress, contentColor)
            Text(text = presentation.label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CapsuleIndicator(
    state: FocusCapsuleState,
    progress: Float?,
    contentColor: Color,
) {
    when {
        state is FocusCapsuleState.Saved -> Icon(Icons.Rounded.Check, contentDescription = null)
        state is FocusCapsuleState.SaveFailed -> Icon(Icons.Rounded.ErrorOutline, contentDescription = null)
        state is FocusCapsuleState.Recording -> Icon(
            Icons.Rounded.FiberManualRecord,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        progress != null -> CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(18.dp),
            color = contentColor,
            trackColor = contentColor.copy(alpha = 0.25f),
            strokeWidth = 2.dp,
        )
        else -> CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = contentColor,
            trackColor = contentColor.copy(alpha = 0.25f),
            strokeWidth = 2.dp,
        )
    }
}

const val FOCUS_CAPSULE_TEST_TAG = "vela_focus_capsule"
