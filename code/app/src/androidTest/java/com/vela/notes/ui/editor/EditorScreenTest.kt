package com.vela.notes.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.vela.notes.data.audio.AudioPlaybackState
import com.vela.notes.domain.model.NoteBlockType
import com.vela.notes.ui.sheet.InsertAction
import com.vela.notes.ui.theme.VelaTheme
import com.vela.notes.ui.audio.RecorderState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersReadyAndMissingMediaBlocks() {
        setScreen(
            state = state(
                mediaBlocks = listOf(
                    media("photo", "anh-vela.jpg", EditorMediaStatus.READY, position = 10),
                    media("missing", "Tệp đính kèm", EditorMediaStatus.MISSING, position = 20),
                ),
            ),
        )

        composeRule.onNodeWithTag("editor_media_photo").assertIsDisplayed()
        composeRule.onNodeWithText("anh-vela.jpg").assertIsDisplayed()
        composeRule.onNodeWithText("Không tìm thấy tệp").assertIsDisplayed()
    }

    @Test
    fun retryMissingMediaDispatchesBlockId() {
        var retriedId: String? = null
        setScreen(
            state = state(mediaBlocks = listOf(media("missing", "Ảnh", EditorMediaStatus.MISSING))),
            onRetryMedia = { retriedId = it },
        )

        composeRule.onNodeWithText("Thử lại").performClick()

        assertEquals("missing", retriedId)
    }

    @Test
    fun mediaMenuDispatchesRemoveCommand() {
        var selected: Pair<String, EditorMediaAction>? = null
        val ready = media("photo", "Ảnh", EditorMediaStatus.READY)
        composeRule.setContent {
            VelaTheme {
                EditorScreen(
                    state = state(mediaBlocks = listOf(ready)),
                    onAction = {},
                    onBack = {},
                    onMediaAction = { block, action -> selected = block.id to action },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Tùy chọn Ảnh").performClick()
        composeRule.onNodeWithText("Xóa khỏi ghi chú").performClick()

        assertEquals("photo" to EditorMediaAction.REMOVE, selected)
    }

    @Test
    fun addButtonOpensInsertSheetAndDispatchesAction() {
        var selected: InsertAction? = null
        setScreen(onInsertAction = { selected = it })

        composeRule.onNodeWithContentDescription("Thêm nội dung").performClick()
        composeRule.onNodeWithContentDescription("Chọn ảnh").assertIsDisplayed().performClick()

        assertEquals(InsertAction.PHOTOS, selected)
    }

    @Test
    fun formattingBarRemainsAvailableWhileEditing() {
        var editorState by mutableStateOf(state())
        composeRule.setContent {
            VelaTheme {
                EditorScreen(
                    state = editorState,
                    onAction = { editorState = editorState.reduce(it) },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("editor_block_text").performClick().performTextInput("Vela")

        composeRule.onNodeWithContentDescription("In đậm").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Thêm nội dung").assertIsDisplayed()
    }

    @Test
    fun topBarBackUsesEditorBackCallback() {
        var backCount = 0
        setScreen(onBack = { backCount += 1 })

        composeRule.onNodeWithContentDescription("Quay lại Ghi chú").performClick()
        composeRule.waitForIdle()

        assertEquals(1, backCount)
    }

    @Test
    fun recordingControlsDispatchPauseResumeAndStop() {
        var pauseCount = 0
        var resumeCount = 0
        var stopCount = 0
        var recorderState by mutableStateOf<RecorderState>(RecorderState.Recording(0))
        composeRule.setContent {
            VelaTheme {
                EditorScreen(
                    state = state(),
                    onAction = {},
                    onBack = {},
                    recorderState = recorderState,
                    recordingElapsedSeconds = 65,
                    onPauseRecording = {
                        pauseCount += 1
                        recorderState = RecorderState.Paused(100)
                    },
                    onResumeRecording = {
                        resumeCount += 1
                        recorderState = RecorderState.Recording(100)
                    },
                    onStopRecording = { stopCount += 1 },
                )
            }
        }

        composeRule.onNodeWithText("Đang ghi 01:05").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tạm dừng ghi").performClick()
        composeRule.onNodeWithText("Đã tạm dừng 01:05").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tiếp tục ghi").performClick()
        composeRule.onNodeWithContentDescription("Dừng và lưu bản ghi").performClick()

        assertEquals(1, pauseCount)
        assertEquals(1, resumeCount)
        assertEquals(1, stopCount)
    }

    @Test
    fun audioBlockRendersPlayerAndDispatchesPlaybackActions() {
        val block = audioBlock()
        var toggles = 0
        var seekPosition: Long? = null
        var selectedSpeed: Float? = null
        var playing by mutableStateOf(false)
        var speed by mutableStateOf(1f)
        composeRule.setContent {
            VelaTheme {
                EditorScreen(
                    state = state(mediaBlocks = listOf(block)),
                    onAction = {},
                    onBack = {},
                    playbackState = AudioPlaybackState(
                        attachmentId = block.attachmentId,
                        isPlaying = playing,
                        positionMs = 1_500,
                        durationMs = 10_000,
                        speed = speed,
                    ),
                    onToggleAudio = {
                        toggles += 1
                        playing = !playing
                    },
                    onSeekAudio = { seekPosition = it },
                    onAudioSpeedChange = {
                        selectedSpeed = it
                        speed = it
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Dạng sóng âm").assertIsDisplayed()
        composeRule.onNodeWithText("00:01 / 00:10").assertIsDisplayed()
        composeRule.onNodeWithText("1x").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tùy chọn Ghi âm").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Phát Ghi âm").performClick()
        composeRule.onNodeWithContentDescription("Tạm dừng Ghi âm").performClick()
        assertEquals(2, toggles)

        composeRule.onNodeWithContentDescription("Vị trí phát").performSemanticsAction(
            SemanticsActions.SetProgress,
        ) { it(4_000f) }
        assertEquals(4_000L, seekPosition)

        composeRule.onNodeWithContentDescription("Tốc độ phát").performClick()
        composeRule.onNodeWithText("0.5x").assertIsDisplayed()
        composeRule.onNodeWithText("1.5x").assertIsDisplayed()
        composeRule.onNodeWithText("2x").performClick()
        assertEquals(2f, selectedSpeed)
        composeRule.onNodeWithText("2x").assertIsDisplayed()
    }

    @Test
    fun titleFocusPrecedesTheBodyAndFormattingStaysAboveTheKeyboard() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(800.dp, 360.dp)),
            ) {
                VelaTheme {
                    EditorScreen(
                        state = state(),
                        onAction = {},
                        onBack = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("editor_title").performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("editor_title").assertIsFocused()
        composeRule.onNodeWithContentDescription("Quay lại Ghi chú").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("In đậm").assertIsDisplayed()

        val bar = composeRule.onNodeWithTag("editor_formatting_bar").getUnclippedBoundsInRoot()
        val title = composeRule.onNodeWithTag("editor_title").getUnclippedBoundsInRoot()
        assertTrue(
            "titleBottom=${title.bottom.value} barTop=${bar.top.value}",
            title.bottom <= bar.top + 1.dp,
        )
        assertTrue((bar.bottom - bar.top).value >= 47.5f)
    }

    @Test
    fun tappingMarkdownMarkerDispatchesToggleWithoutRenderingCheckbox() {
        var receivedAction: EditorAction? = null
        var toggleCount = 0
        var editorState by mutableStateOf(state(text = "- [ ] Mua tài liệu"))
        composeRule.setContent {
            VelaTheme {
                EditorScreen(
                    state = editorState,
                    onAction = {
                        receivedAction = it
                        if (it is EditorAction.ToggleChecklistItem) toggleCount += 1
                        editorState = editorState.reduce(it)
                    },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("editor_block_text")
            .performTouchInput { click(Offset(16f, 16f)) }

        assertEquals(EditorAction.ToggleChecklistItem("text", 0), receivedAction)
        composeRule.onNodeWithText("- [x] Mua tài liệu", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onAllNodes(
            matcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox),
            useUnmergedTree = true,
        ).assertCountEquals(0)

        composeRule.onNodeWithTag("editor_block_text")
            .performTouchInput { click(Offset(180f, 16f)) }
            .performTextInput("!")

        assertEquals(1, toggleCount)
        assertTrue(editorState.textBlocks.single().text.contains("!"))
    }

    private fun setScreen(
        state: EditorState = state(),
        onBack: () -> Unit = {},
        onInsertAction: (InsertAction) -> Unit = {},
        onRetryMedia: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            VelaTheme {
                EditorScreen(
                    state = state,
                    onAction = {},
                    onBack = onBack,
                    onInsertAction = onInsertAction,
                    onRetryMedia = onRetryMedia,
                )
            }
        }
    }

    private fun state(
        mediaBlocks: List<EditorMediaBlock> = emptyList(),
        text: String = "",
    ) = EditorState(
        noteId = "note",
        title = "Vela",
        timestamp = "Hôm nay",
        textBlocks = listOf(
            EditorTextBlock(
                id = "text",
                value = TextFieldValue(text, TextRange(text.length)),
                position = 0,
            ),
        ),
        mediaBlocks = mediaBlocks,
    )

    private fun media(
        id: String,
        name: String,
        status: EditorMediaStatus,
        position: Long = 10,
    ) = EditorMediaBlock(
        id = id,
        type = NoteBlockType.IMAGE,
        position = position,
        displayName = name,
        mimeType = "image/jpeg",
        sizeBytes = 100,
        status = status,
    )

    private fun audioBlock() = EditorMediaBlock(
        id = "audio",
        type = NoteBlockType.AUDIO,
        position = 10,
        displayName = "Ghi âm",
        mimeType = "audio/mp4",
        sizeBytes = 1_000,
        durationMs = 10_000,
        waveform = byteArrayOf(20, 80, 40),
        attachmentId = "attachment-audio",
        relativePath = "notes/audio.m4a",
        status = EditorMediaStatus.READY,
    )
}
