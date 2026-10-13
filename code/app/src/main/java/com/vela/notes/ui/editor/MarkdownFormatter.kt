package com.vela.notes.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

enum class MarkdownStyle {
    HEADING,
    BOLD,
    ITALIC,
    BULLET_LIST,
    CHECKLIST,
    QUOTE,
    CODE,
}

object MarkdownFormatter {
    fun apply(value: TextFieldValue, style: MarkdownStyle): TextFieldValue = when (style) {
        MarkdownStyle.BOLD -> wrap(value, "**", "**")
        MarkdownStyle.ITALIC -> wrap(value, "_", "_")
        MarkdownStyle.CODE -> wrap(value, "`", "`")
        MarkdownStyle.HEADING -> toggleLinePrefix(value, "# ")
        MarkdownStyle.BULLET_LIST -> toggleLinePrefix(value, "- ")
        MarkdownStyle.CHECKLIST -> toggleLinePrefix(value, "- [ ] ")
        MarkdownStyle.QUOTE -> toggleLinePrefix(value, "> ")
    }

    private fun wrap(
        value: TextFieldValue,
        before: String,
        after: String,
    ): TextFieldValue {
        val start = value.selection.min.coerceIn(0, value.text.length)
        val end = value.selection.max.coerceIn(start, value.text.length)
        val selected = value.text.substring(start, end)
        val replacement = before + selected + after
        val selection = if (start == end) {
            TextRange(start + before.length)
        } else {
            TextRange(start + before.length, end + before.length)
        }
        return value.copy(
            text = value.text.replaceRange(start, end, replacement),
            selection = selection,
            composition = null,
        )
    }

    private fun toggleLinePrefix(
        value: TextFieldValue,
        prefix: String,
    ): TextFieldValue {
        val cursor = value.selection.min.coerceIn(0, value.text.length)
        val lineStart = value.text.lastIndexOf('\n', startIndex = (cursor - 1).coerceAtLeast(0))
            .let { if (it < 0) 0 else it + 1 }
        val hasPrefix = value.text.startsWith(prefix, startIndex = lineStart)
        val newText = if (hasPrefix) {
            value.text.removeRange(lineStart, lineStart + prefix.length)
        } else {
            value.text.replaceRange(lineStart, lineStart, prefix)
        }
        val delta = if (hasPrefix) -prefix.length else prefix.length
        fun move(offset: Int): Int = if (offset >= lineStart) (offset + delta).coerceAtLeast(lineStart) else offset
        return value.copy(
            text = newText,
            selection = TextRange(move(value.selection.start), move(value.selection.end)),
            composition = null,
        )
    }
}
