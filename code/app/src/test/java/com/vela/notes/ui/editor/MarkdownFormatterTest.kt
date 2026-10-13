package com.vela.notes.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownFormatterTest {
    @Test
    fun boldWrapsSelectionAndKeepsInnerTextSelected() {
        val result = MarkdownFormatter.apply(
            TextFieldValue("Vela note", selection = TextRange(0, 4)),
            MarkdownStyle.BOLD,
        )

        assertEquals("**Vela** note", result.text)
        assertEquals(TextRange(2, 6), result.selection)
    }

    @Test
    fun italicWithoutSelectionPlacesCursorBetweenMarkers() {
        val result = MarkdownFormatter.apply(
            TextFieldValue("Note", selection = TextRange(4)),
            MarkdownStyle.ITALIC,
        )

        assertEquals("Note__", result.text)
        assertEquals(TextRange(5), result.selection)
    }

    @Test
    fun headingTogglesCurrentLineAndMovesCursor() {
        val added = MarkdownFormatter.apply(
            TextFieldValue("Title", selection = TextRange(5)),
            MarkdownStyle.HEADING,
        )
        val removed = MarkdownFormatter.apply(added, MarkdownStyle.HEADING)

        assertEquals("# Title", added.text)
        assertEquals(TextRange(7), added.selection)
        assertEquals("Title", removed.text)
        assertEquals(TextRange(5), removed.selection)
    }

    @Test
    fun checklistPrefixesOnlyCurrentLine() {
        val result = MarkdownFormatter.apply(
            TextFieldValue("First\nSecond", selection = TextRange(12)),
            MarkdownStyle.CHECKLIST,
        )

        assertEquals("First\n- [ ] Second", result.text)
        assertEquals(TextRange(18), result.selection)
    }
}
