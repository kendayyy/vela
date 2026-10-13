package com.vela.notes.ui.collections

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.vela.notes.ui.theme.VelaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CollectionsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersGroupedCollectionsAndFolders() {
        composeRule.setContent {
            VelaTheme {
                CollectionsScreen(
                    collections = DefaultCollections,
                    folders = listOf(FolderRowUi("work", "Công việc", 12)),
                    onCollectionClick = {},
                    onFolderClick = {},
                )
            }
        }

        composeRule.onNodeWithTag(COLLECTIONS_TOP_BAR_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Ghi chú", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Công việc, 12 ghi chú")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun rowsDispatchStableIdentifiers() {
        var selectedCollection: CollectionKind? = null
        var selectedFolder: String? = null
        composeRule.setContent {
            VelaTheme {
                CollectionsScreen(
                    collections = DefaultCollections,
                    folders = listOf(FolderRowUi("personal", "Cá nhân", 3)),
                    onCollectionClick = { selectedCollection = it },
                    onFolderClick = { selectedFolder = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Đã ghim, 0 ghi chú").performClick()
        assertEquals(CollectionKind.PINNED, selectedCollection)

        composeRule.onNodeWithContentDescription("Cá nhân, 3 ghi chú").performClick()
        assertEquals("personal", selectedFolder)
    }
}
