package com.vela.notes.di

import android.content.Context
import androidx.room.Room
import com.vela.notes.data.local.VelaDatabase
import com.vela.notes.data.local.dao.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VelaDatabase =
        Room.databaseBuilder(context, VelaDatabase::class.java, "vela.db").build()

    @Provides
    fun provideNoteDao(database: VelaDatabase): NoteDao = database.noteDao()
}
