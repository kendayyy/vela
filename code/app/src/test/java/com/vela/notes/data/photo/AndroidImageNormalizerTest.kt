package com.vela.notes.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.vela.notes.data.attachment.AttachmentSource
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidImageNormalizerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val fixtures = mutableListOf<File>()
    private val normalizer = AndroidImageNormalizer()

    @After
    fun tearDown() {
        fixtures.forEach(File::delete)
    }

    @Test
    fun appliesExifRotationAndRemovesGpsMetadata() = runTest {
        val fixture = createJpegFixture(width = 40, height = 20).also { file ->
            ExifInterface(file).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                setLatLong(10.762622, 106.660172)
                saveAttributes()
            }
        }

        val result = normalizer.normalize(
            PhotoSelection(
                displayName = "camera-original.jpeg",
                mimeType = "image/jpeg",
                source = AttachmentSource { FileInputStream(fixture) },
            ),
        )
        val bytes = result.source.open().use { it.readBytes() }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val cleanExif = ExifInterface(ByteArrayInputStream(bytes))

        assertEquals(20, bitmap.width)
        assertEquals(40, bitmap.height)
        assertEquals("camera-original.jpg", result.displayName)
        assertEquals("image/jpeg", result.mimeType)
        assertNull(cleanExif.latLong)
        assertTrue(
            cleanExif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED,
            ) in setOf(ExifInterface.ORIENTATION_UNDEFINED, ExifInterface.ORIENTATION_NORMAL),
        )
        bitmap.recycle()
    }

    @Test
    fun rejectsUnreadableImage() = runTest {
        val error = runCatching {
            normalizer.normalize(
                PhotoSelection(
                    displayName = "fake.jpg",
                    mimeType = "image/jpeg",
                    source = AttachmentSource { ByteArrayInputStream("not an image".encodeToByteArray()) },
                ),
            )
        }.exceptionOrNull()

        assertTrue(error is InvalidImageException)
    }

    private fun createJpegFixture(width: Int, height: Int): File {
        val file = File.createTempFile("vela-photo-", ".jpg", context.cacheDir)
        fixtures += file
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output))
        }
        bitmap.recycle()
        return file
    }
}
