package com.vela.notes.di

import com.vela.notes.data.preferences.AppLockSettingsStore
import com.vela.notes.data.preferences.DataStoreAppLockSettingsStore
import com.vela.notes.security.ElapsedRealtimeClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {
    @Binds
    @Singleton
    abstract fun bindAppLockSettingsStore(
        implementation: DataStoreAppLockSettingsStore,
    ): AppLockSettingsStore

    companion object {
        @Provides
        @Singleton
        fun provideElapsedRealtimeClock(): ElapsedRealtimeClock =
            ElapsedRealtimeClock { android.os.SystemClock.elapsedRealtime() }
    }
}
