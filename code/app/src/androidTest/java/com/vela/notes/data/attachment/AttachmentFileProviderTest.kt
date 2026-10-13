package com.vela.notes.data.attachment

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttachmentFileProviderTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val root = File(context.filesDir, "attachments")
    private val staging = File(context.cacheDir, "provider-test-staging")
    private val store = AppPrivateAttachmentStore(root, staging)
    private val handler = AttachmentActionHandler(store)

    @After
    fun tearDown() {
        root.deleteRecursively()
        staging.deleteRecursively()
    }

    @Test
    fun openAndShareUseReadOnlyContentUris() = runBlocking {
        val stored = store.import("note", "attachment", "photo.jpg") {
            "image".byteInputStream()
        }

        val open = handler.openIntent(context, stored.relativePath, "image/jpeg")
        val share = handler.shareIntent(context, stored.relativePath, "image/jpeg")
        val stream = share.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)

        assertEquals(Intent.ACTION_VIEW, open.action)
        assertEquals("content", open.data?.scheme)
        assertEquals("image/jpeg", open.type)
        assertTrue(open.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(Intent.ACTION_SEND, share.action)
        assertEquals("content", stream?.scheme)
        assertTrue(share.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertFalse(share.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0)
    }
}
