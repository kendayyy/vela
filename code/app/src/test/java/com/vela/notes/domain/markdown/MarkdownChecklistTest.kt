package com.vela.notes.domain.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownChecklistTest {
    @Test
    fun parsesOnlyChecklistMarkersAtLineStart() {
        val items = MarkdownChecklist.items("- [ ] Mua tài liệu\ntext - [ ] inline\n- [x] Gửi email\n- [X] Đã xong")

        assertEquals(listOf(0, 37, 53), items.map { it.lineStart })
        assertEquals(listOf("Mua tài liệu", "Gửi email", "Đã xong"), items.map { it.label })
        assertFalse(items[0].checked)
        assertTrue(items[1].checked)
        assertTrue(items[2].checked)
    }

    @Test
    fun parsesFirstMiddleLastAndMultipleItems() {
        val items = MarkdownChecklist.items("- [ ] First\ntext\n- [x] Middle\n- [ ] Last")

        assertEquals(listOf(0, 17, 30), items.map { it.lineStart })
        assertEquals(listOf(0, 2, 3), items.map { it.lineIndex })
    }

    @Test
    fun togglePreservesSelectionBecauseMarkerLengthDoesNotChange() {
        val value = TextFieldValue(
            "- [ ] Viết tiếng Việt\n- [x] Done",
            selection = TextRange(8, 12),
        )

        val checked = MarkdownChecklist.toggle(value, lineStart = 0)
        val unchecked = MarkdownChecklist.toggle(checked, lineStart = 22)

        assertEquals("- [x] Viết tiếng Việt\n- [x] Done", checked.text)
        assertEquals(value.selection, checked.selection)
        assertEquals("- [x] Viết tiếng Việt\n- [ ] Done", unchecked.text)
    }

    @Test
    fun invalidLineDoesNotChangeValue() {
        val value = TextFieldValue("- task", selection = TextRange(2))

        assertEquals(value, MarkdownChecklist.toggle(value, lineStart = 0))
    }

    @Test
    fun markerHitTestMatchesOnlyChecklistMarkerCharacters() {
        val text = "- [ ] Việc đầu ✅\n- [x] Việc sau\n- bullet"
        val secondMarkerStart = text.indexOf("- [x]")

        (0..4).forEach { offset -> assertEquals(0, MarkdownChecklist.itemAtOffset(text, offset)?.lineStart) }
        (secondMarkerStart until secondMarkerStart + 5).forEach { offset ->
            assertEquals(secondMarkerStart, MarkdownChecklist.itemAtOffset(text, offset)?.lineStart)
        }
        assertEquals(null, MarkdownChecklist.itemAtOffset(text, 5))
        assertEquals(null, MarkdownChecklist.itemAtOffset(text, 11))
        assertEquals(null, MarkdownChecklist.itemAtOffset(text, text.lastIndex))
    }
}
