package com.vela.notes.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

fun Modifier.ensureTouchTarget(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val minSize = VelaSize.MinimumTouchTarget.roundToPx()
    val width = maxOf(placeable.width, minSize)
    val height = maxOf(placeable.height, minSize)
    layout(width, height) {
        placeable.place((width - placeable.width) / 2, (height - placeable.height) / 2)
    }
}
