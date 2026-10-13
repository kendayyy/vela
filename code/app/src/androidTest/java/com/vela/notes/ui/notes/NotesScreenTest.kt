package com.vela.notes.ui.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.vela.notes.ui.theme.VelaTheme
import com.vela.notes.domain.model.NoteBlockType
import com.vela.notes.domain.model.TodoProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NotesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val notes = listOf(
        NoteRowUi("one", "Kế hoạch Vela", "Hoàn thiện giao diện ghi chú", "10:30", true),
        NoteRowUi("two", "Danh sách", "Cà phê và bánh mì", "Hôm qua", primaryContentType = NoteBlockType.AUDIO),
    )

    @Test
    fun noteRowShowsTitlePreviewAndTimestamp() {
        setScreen()

        composeRule.onNodeWithText("Kế hoạch Vela", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Hoàn thiện giao diện ghi chú", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("10:30", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Ghi chú có âm thanh").assertIsDisplayed()
    }

    @Test
    fun noteRowShowsChecklistProgressOnlyWhenPresent() {
        val checklistNote = notes.first().copy(todoProgress = TodoProgress(total = 5, completed = 2))
        composeRule.setContent {
            VelaTheme {
                NotesScreen(
                    title = "Tất cả",
                    notes = listOf(checklistNote, notes[1]),
                    query = "",
                    onQueryChange = {},
                    onBack = {},
                    onNoteClick = {},
                    onArchive = {},
                    onDelete = {},
                    onCreateNote = {},
                )
            }
        }

        composeRule.onNodeWithTag("note_todo_progress_one", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("2/5 việc hoàn tất", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onAllNodesWithTag("note_todo_progress_two", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Kế hoạch Vela, đã ghim, 2/5 việc hoàn tất, 10:30")
            .assertIsDisplayed()
    }

    @Test
    fun collectionChipAndOverflowMenuProvideNonGestureActions() {
        var selected: NotesCollection? = null
        var deletedId: String? = null
        composeRule.setContent {
            VelaTheme {
                NotesScreen(
                    title = "Ghi chú",
                    notes = notes,
                    query = "",
                    onQueryChange = {},
                    onBack = {},
                    onNoteClick = {},
                    onArchive = {},
                    onDelete = { deletedId = it },
                    onCreateNote = {},
                    showBackButton = false,
                    onCollectionSelected = { selected = it },
                )
            }
        }

        composeRule.onNodeWithText("Đã ghim").performClick()
        assertEquals(NotesCollection.PINNED, selected)
        composeRule.onNodeWithContentDescription("Tùy chọn Kế hoạch Vela").performClick()
        composeRule.onNodeWithText("Xóa").performClick()
        assertEquals("one", deletedId)
    }

    @Test
    fun filterSheetCanLimitCardsByContentType() {
        setScreen()

        composeRule.onNodeWithContentDescription("Lọc và sắp xếp ghi chú").performClick()
        composeRule.onNodeWithTag("content_filter_row").performScrollToIndex(4)
        composeRule.onNodeWithText("Âm thanh").performClick()
        composeRule.onNodeWithText("Xong").performClick()

        composeRule.onNodeWithText("Danh sách", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("Kế hoạch Vela", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun searchFiltersAndCancelClearsQuery() {
        var query by mutableStateOf("")
        setScreen(query = { query }, onQueryChange = { query = it })

        composeRule.onNodeWithContentDescription("Tìm kiếm ghi chú").performTextInput("cà phê")
        composeRule.onNodeWithText("Danh sách", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Hủy").performClick()
        assertEquals("", query)
        composeRule.onNodeWithText("Kế hoạch Vela", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun swipeLeftDispatchesSoftDelete() {
        var deletedId: String? = null
        setScreen(onDelete = { deletedId = it })

        composeRule.onNodeWithTag("note_row_one").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals("one", deletedId)
    }

    @Test
    fun undoActionRestoresTheSwipedNote() {
        var restoredId: String? = null
        setScreen(onUndo = { restoredId = it })

        composeRule.onNodeWithTag("note_row_one").performTouchInput { swipeLeft() }
        composeRule.onNodeWithText("Hoàn tác").performClick()

        assertEquals("one", restoredId)
    }

    @Test
    fun trashConfirmsPermanentDeleteAndEmptyTrash() {
        var restoredId: String? = null
        var deletedId: String? = null
        var emptied = false
        composeRule.setContent {
            VelaTheme {
                NotesScreen(
                    title = "Đã xóa",
                    notes = notes.take(1),
                    query = "",
                    onQueryChange = {},
                    onBack = {},
                    onNoteClick = {},
                    onArchive = {},
                    onDelete = {},
                    onCreateNote = {},
                    isTrash = true,
                    onRestore = { restoredId = it },
                    onDeletePermanently = { deletedId = it },
                    onEmptyTrash = { emptied = true },
                )
            }
        }

        composeRule.onNodeWithText("Khôi phục").performClick()
        assertEquals("one", restoredId)
        composeRule.onNodeWithText("Xóa vĩnh viễn").performClick()
        composeRule.onNodeWithText("Xóa vĩnh viễn ghi chú này?").assertIsDisplayed()
        composeRule.onNodeWithText("Xóa").performClick()
        assertEquals("one", deletedId)
        composeRule.onNodeWithText("Dọn thùng rác").performClick()
        composeRule.onNodeWithText("Dọn thùng rác?").assertIsDisplayed()
        composeRule.onNodeWithText("Dọn").performClick()
        assertEquals(true, emptied)
    }

    @Test
    fun pinnedRowAnnouncesPinState() {
        setScreen()

        composeRule.onNodeWithContentDescription("Kế hoạch Vela, đã ghim, 10:30").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Danh sách, Hôm qua").assertIsDisplayed()
    }

    @Test
    fun largeFontLandscapeKeepsSearchCancelAndCreateAtLeast48Dp() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.FontScale(2f) then
                    DeviceConfigurationOverride.ForcedSize(DpSize(800.dp, 360.dp)),
            ) {
                VelaTheme {
                    NotesScreen(
                        title = "Tất cả",
                        notes = notes,
                        query = "vé",
                        onQueryChange = {},
                        onBack = {},
                        onNoteClick = {},
                        onArchive = {},
                        onDelete = {},
                        onCreateNote = {},
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Quay lại Thư mục").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tìm kiếm ghi chú").assertIsDisplayed()
        val cancel = composeRule.onNodeWithTag("search_cancel").getUnclippedBoundsInRoot()
        assertTrue((cancel.bottom - cancel.top).value >= 47.5f)
        assertTrue((cancel.right - cancel.left).value >= 47.5f)
        composeRule.onNodeWithContentDescription("Tạo ghi chú").assertIsDisplayed()
        val create = composeRule.onNodeWithTag(CREATE_NOTE_TEST_TAG).getUnclippedBoundsInRoot()
        assertTrue(
            "create ${(create.right - create.left).value}x${(create.bottom - create.top).value}",
            (create.bottom - create.top).value >= 47.5f && (create.right - create.left).value >= 47.5f,
        )
        assertTrue(
            "createTop=${create.top.value} cancelBottom=${cancel.bottom.value}",
            create.top >= cancel.bottom,
        )
    }

    @Test
    fun createActionDispatchesCommand() {
        var createCount = 0
        setScreen(onCreateNote = { createCount++ })

        composeRule.onNodeWithTag(CREATE_NOTE_TEST_TAG).performClick()

        assertEquals(1, createCount)
    }

    private fun setScreen(
        query: () -> String = { "" },
        onQueryChange: (String) -> Unit = {},
        onDelete: (String) -> Unit = {},
        onCreateNote: () -> Unit = {},
        onUndo: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            VelaTheme {
                NotesScreen(
                    title = "Tất cả",
                    notes = notes,
                    query = query(),
                    onQueryChange = onQueryChange,
                    onBack = {},
                    onNoteClick = {},
                    onArchive = {},
                    onDelete = onDelete,
                    onCreateNote = onCreateNote,
                    onUndo = onUndo,
                )
            }
        }
    }
}
