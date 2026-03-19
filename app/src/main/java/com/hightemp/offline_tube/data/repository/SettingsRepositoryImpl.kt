package com.hightemp.offline_tube.data.repository

import android.content.Context
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hightemp.offline_tube.domain.model.VideoQuality
import com.hightemp.offline_tube.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : SettingsRepository {

    companion object {
        private val KEY_VIDEO_QUALITY = stringPreferencesKey("video_quality")
        private val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
        private val KEY_DOWNLOAD_PATH = stringPreferencesKey("download_path")
        private val KEY_MAX_CONCURRENT = intPreferencesKey("max_concurrent_downloads")

        private const val DEFAULT_MAX_CONCURRENT = 1
        private const val DEFAULT_WIFI_ONLY = false
    }

    private fun defaultDownloadPath(): String {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "OfflineTube"
        )
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    override fun observeVideoQuality(): Flow<VideoQuality> {
        return dataStore.data.map { prefs ->
            val label = prefs[KEY_VIDEO_QUALITY]
            if (label != null) VideoQuality.fromLabel(label) else VideoQuality.Q_720P
        }
    }

    override suspend fun getVideoQuality(): VideoQuality = withContext(Dispatchers.IO) {
        val prefs = dataStore.data.first()
        val label = prefs[KEY_VIDEO_QUALITY]
        if (label != null) VideoQuality.fromLabel(label) else VideoQuality.Q_720P
    }

    override suspend fun setVideoQuality(quality: VideoQuality) = withContext(Dispatchers.IO) {
        Timber.d("SettingsRepositoryImpl: setVideoQuality %s", quality.label)
        dataStore.edit { prefs ->
            prefs[KEY_VIDEO_QUALITY] = quality.label
        }
        Unit
    }

    override fun observeWifiOnly(): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            prefs[KEY_WIFI_ONLY] ?: DEFAULT_WIFI_ONLY
        }
    }

    override suspend fun getWifiOnly(): Boolean = withContext(Dispatchers.IO) {
        dataStore.data.first()[KEY_WIFI_ONLY] ?: DEFAULT_WIFI_ONLY
    }

    override suspend fun setWifiOnly(enabled: Boolean) = withContext(Dispatchers.IO) {
        Timber.d("SettingsRepositoryImpl: setWifiOnly %s", enabled)
        dataStore.edit { prefs ->
            prefs[KEY_WIFI_ONLY] = enabled
        }
        Unit
    }

    override fun observeDownloadPath(): Flow<String> {
        return dataStore.data.map { prefs ->
            prefs[KEY_DOWNLOAD_PATH] ?: defaultDownloadPath()
        }
    }

    override suspend fun getDownloadPath(): String = withContext(Dispatchers.IO) {
        dataStore.data.first()[KEY_DOWNLOAD_PATH] ?: defaultDownloadPath()
    }

    override suspend fun setDownloadPath(path: String) = withContext(Dispatchers.IO) {
        Timber.d("SettingsRepositoryImpl: setDownloadPath %s", path)
        dataStore.edit { prefs ->
            prefs[KEY_DOWNLOAD_PATH] = path
        }
        Unit
    }

    override fun observeMaxConcurrentDownloads(): Flow<Int> {
        return dataStore.data.map { prefs ->
            prefs[KEY_MAX_CONCURRENT] ?: DEFAULT_MAX_CONCURRENT
        }
    }

    override suspend fun getMaxConcurrentDownloads(): Int = withContext(Dispatchers.IO) {
        dataStore.data.first()[KEY_MAX_CONCURRENT] ?: DEFAULT_MAX_CONCURRENT
    }

    override suspend fun setMaxConcurrentDownloads(count: Int) = withContext(Dispatchers.IO) {
        Timber.d("SettingsRepositoryImpl: setMaxConcurrentDownloads %d", count)
        dataStore.edit { prefs ->
            prefs[KEY_MAX_CONCURRENT] = count
        }
        Unit
    }
}
