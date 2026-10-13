package com.vela.notes.data.local

import androidx.room.TypeConverter
import com.vela.notes.data.local.entity.AttachmentKind
import com.vela.notes.data.local.entity.BlockType

class VelaTypeConverters {
    @TypeConverter
    fun blockTypeToString(value: BlockType): String = value.name

    @TypeConverter
    fun stringToBlockType(value: String): BlockType = BlockType.valueOf(value)

    @TypeConverter
    fun attachmentKindToString(value: AttachmentKind): String = value.name

    @TypeConverter
    fun stringToAttachmentKind(value: String): AttachmentKind = AttachmentKind.valueOf(value)
}
