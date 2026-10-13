package com.vela.notes.data.local

import com.vela.notes.data.local.entity.AttachmentKind
import com.vela.notes.data.local.entity.BlockType
import org.junit.Assert.assertEquals
import org.junit.Test

class VelaTypeConvertersTest {
    private val converters = VelaTypeConverters()

    @Test
    fun blockType_roundTrips() {
        BlockType.entries.forEach { type ->
            assertEquals(type, converters.stringToBlockType(converters.blockTypeToString(type)))
        }
    }

    @Test
    fun attachmentKind_roundTrips() {
        AttachmentKind.entries.forEach { kind ->
            assertEquals(
                kind,
                converters.stringToAttachmentKind(converters.attachmentKindToString(kind)),
            )
        }
    }
}
