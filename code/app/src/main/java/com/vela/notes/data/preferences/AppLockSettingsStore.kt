package com.vela.notes.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class AppLockTimeout(val label: String, val milliseconds: Long) {
    IMMEDIATELY("Ngay lập tức", 0L),
    THIRTY_SECONDS("30 giây", 30_000L),
    ONE_MINUTE("1 phút", 60_000L),
    FIVE_MINUTES("5 phút", 300_000L),
}

data class AppLockSettings(
    val enabled: Boolean = false,
    val timeout: AppLockTimeout = AppLockTimeout.THIRTY_SECONDS,
)

interface AppLockSettingsStore {
    val settings: Flow<AppLockSettings>

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setTimeout(timeout: AppLockTimeout)
}

private val Context.appLockDataStore: DataStore<Preferences> by preferencesDataStore(name = "vela_security")

@Singleton
class DataStoreAppLockSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppLockSettingsStore {
    override val settings: Flow<AppLockSettings> = context.appLockDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            AppLockSettings(
                enabled = preferences[ENABLED] ?: false,
                timeout = preferences[TIMEOUT_MS].toAppLockTimeout(),
            )
        }

    override suspend fun setEnabled(enabled: Boolean) {
        context.appLockDataStore.edit { it[ENABLED] = enabled }
    }

    override suspend fun setTimeout(timeout: AppLockTimeout) {
        context.appLockDataStore.edit { it[TIMEOUT_MS] = timeout.milliseconds }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("app_lock_enabled")
        val TIMEOUT_MS = longPreferencesKey("app_lock_timeout_ms")
    }
}

private fun Long?.toAppLockTimeout(): AppLockTimeout =
    AppLockTimeout.entries.firstOrNull { it.milliseconds == this } ?: AppLockTimeout.THIRTY_SECONDS
