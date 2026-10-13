package com.vela.notes.di

import com.vela.notes.data.attachment.AppPrivateAttachmentStore
import com.vela.notes.data.attachment.AttachmentStore
import com.vela.notes.data.photo.AndroidPhotoSelectionReader
import com.vela.notes.data.photo.AndroidImageNormalizer
import com.vela.notes.data.photo.ImageNormalizer
import com.vela.notes.data.photo.PhotoSelectionReader
import com.vela.notes.data.file.AndroidDocumentSelectionReader
import com.vela.notes.data.file.DocumentSelectionReader
import com.vela.notes.data.audio.AndroidAudioRecorderEngine
import com.vela.notes.data.audio.AudioRecorderEngine
import com.vela.notes.data.audio.AndroidAudioWaveformExtractor
import com.vela.notes.data.audio.AudioWaveformExtractor
import com.vela.notes.data.audio.AndroidAudioPlaybackController
import com.vela.notes.data.audio.AudioPlaybackController
import com.vela.notes.data.scanner.AndroidScanDocumentExporter
import com.vela.notes.data.scanner.AndroidScanOcr
import com.vela.notes.data.scanner.ScanDocumentExporter
import com.vela.notes.data.scanner.ScanOcr
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {
    @Binds
    @Singleton
    abstract fun bindAttachmentStore(implementation: AppPrivateAttachmentStore): AttachmentStore

    @Binds
    @Singleton
    abstract fun bindPhotoSelectionReader(implementation: AndroidPhotoSelectionReader): PhotoSelectionReader

    @Binds
    @Singleton
    abstract fun bindImageNormalizer(implementation: AndroidImageNormalizer): ImageNormalizer

    @Binds
    @Singleton
    abstract fun bindDocumentSelectionReader(
        implementation: AndroidDocumentSelectionReader,
    ): DocumentSelectionReader

    @Binds
    @Singleton
    abstract fun bindAudioRecorderEngine(implementation: AndroidAudioRecorderEngine): AudioRecorderEngine

    @Binds
    @Singleton
    abstract fun bindAudioWaveformExtractor(
        implementation: AndroidAudioWaveformExtractor,
    ): AudioWaveformExtractor

    @Binds
    abstract fun bindAudioPlaybackController(
        implementation: AndroidAudioPlaybackController,
    ): AudioPlaybackController

    @Binds
    abstract fun bindScanDocumentExporter(
        implementation: AndroidScanDocumentExporter,
    ): ScanDocumentExporter

    @Binds
    abstract fun bindScanOcr(implementation: AndroidScanOcr): ScanOcr
}
