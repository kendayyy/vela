package com.vela.notes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vela.notes.data.local.dao.NoteDao
import com.vela.notes.data.local.entity.AttachmentEntity
import com.vela.notes.data.local.entity.NoteBlockEntity
import com.vela.notes.data.local.entity.NoteEntity
import com.vela.notes.data.local.entity.NoteSearchEntity

@Database(
    entities = [
        NoteEntity::class,
        NoteBlockEntity::class,
        AttachmentEntity::class,
        NoteSearchEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(VelaTypeConverters::class)
abstract class VelaDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
