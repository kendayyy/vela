package com.vela.notes.di

import com.vela.notes.data.repository.OfflineNoteRepository
import com.vela.notes.domain.repository.NoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindNoteRepository(implementation: OfflineNoteRepository): NoteRepository
}
