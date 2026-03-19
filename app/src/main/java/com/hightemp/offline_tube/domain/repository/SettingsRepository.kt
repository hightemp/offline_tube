package com.hightemp.offline_tube.domain.repository

import com.hightemp.offline_tube.domain.model.VideoQuality
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app settings (DataStore-backed).
 * Implemented in data layer.
 */
interface SettingsRepository {
    /**
     * Get preferred video quality.
     */
    fun observeVideoQuality(): Flow<VideoQuality>
    suspend fun getVideoQuality(): VideoQuality
    suspend fun setVideoQuality(quality: VideoQuality)

    /**
     * Get whether to download only on WiFi.
     */
    fun observeWifiOnly(): Flow<Boolean>
    suspend fun getWifiOnly(): Boolean
    suspend fun setWifiOnly(wifiOnly: Boolean)

    /**
     * Get custom download directory path.
     */
    fun observeDownloadPath(): Flow<String>
    suspend fun getDownloadPath(): String
    suspend fun setDownloadPath(path: String)

    /**
     * Get max concurrent downloads.
     */
    fun observeMaxConcurrentDownloads(): Flow<Int>
    suspend fun getMaxConcurrentDownloads(): Int
    suspend fun setMaxConcurrentDownloads(count: Int)
}
