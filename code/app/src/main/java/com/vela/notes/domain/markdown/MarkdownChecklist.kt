package com.vela.notes.domain.markdown

import androidx.compose.ui.text.input.TextFieldValue

/** A checklist marker is intentionally strict so ordinary Markdown bullets remain untouched. */
data class ChecklistItem(
    val lineStart: Int,
    val lineIndex: Int,
    val label: String,
    val checked: Boolean,
)

object MarkdownChecklist {
    fun items(text: String): List<ChecklistItem> {
        if (text.isEmpty()) return emptyList()

        val result = mutableListOf<ChecklistItem>()
        var lineStart = 0
        var lineIndex = 0
        while (lineStart <= text.length) {
            val lineEnd = text.indexOf('\n', lineStart).let { if (it < 0) text.length else it }
            val line = text.substring(lineStart, lineEnd)
            val marker = line.marker()
            if (marker != null) {
                result += ChecklistItem(
                    lineStart = lineStart,
                    lineIndex = lineIndex,
                    label = line.substring(CHECKLIST_PREFIX_LENGTH),
                    checked = marker,
                )
            }
            if (lineEnd == text.length) break
            lineStart = lineEnd + 1
            lineIndex += 1
        }
        return result
    }

    fun toggle(value: TextFieldValue, lineStart: Int): TextFieldValue {
        val item = items(value.text).firstOrNull { it.lineStart == lineStart } ?: return value
        val markerOffset = lineStart + CHECKED_MARKER_OFFSET
        val replacement = if (item.checked) ' ' else 'x'
        val updated = value.text.toCharArray().also { chars -> chars[markerOffset] = replacement }.concatToString()
        return value.copy(text = updated, composition = null)
    }

    fun itemAtOffset(text: String, offset: Int): ChecklistItem? =
        items(text).firstOrNull { item ->
            offset in item.lineStart until (item.lineStart + MARKER_LENGTH)
        }

    private fun String.marker(): Boolean? = when {
        startsWith("- [ ] ") -> false
        startsWith("- [x] ", ignoreCase = true) -> true
        else -> null
    }

    private const val CHECKLIST_PREFIX_LENGTH = 6
    private const val CHECKED_MARKER_OFFSET = 3
    private const val MARKER_LENGTH = 5
}
