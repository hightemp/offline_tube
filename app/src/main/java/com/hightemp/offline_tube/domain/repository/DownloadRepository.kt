package com.hightemp.offline_tube.domain.repository

import com.hightemp.offline_tube.domain.model.DownloadTask
import com.hightemp.offline_tube.domain.model.DownloadStatus
import com.hightemp.offline_tube.domain.model.VideoQuality
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for download queue management.
 * Implemented in data layer.
 */
interface DownloadRepository {
    /**
     * Enqueue a new download task.
     */
    suspend fun enqueueDownload(
        videoId: String,
        title: String,
        thumbnailUrl: String,
        downloadUrl: String,
        quality: VideoQuality,
        totalBytes: Long
    ): Long

    /**
     * Observe the download queue as a reactive Flow.
     */
    fun observeQueue(): Flow<List<DownloadTask>>

    /**
     * Observe active (non-completed, non-cancelled) downloads.
     */
    fun observeActiveDownloads(): Flow<List<DownloadTask>>

    /**
     * Get a specific download task by ID.
     */
    suspend fun getDownloadById(id: Long): DownloadTask?

    /**
     * Get a download task by video ID.
     */
    suspend fun getDownloadByVideoId(videoId: String): DownloadTask?

    /**
     * Update download progress.
     */
    suspend fun updateProgress(id: Long, downloadedBytes: Long, totalBytes: Long, progress: Int)

    /**
     * Update download status.
     */
    suspend fun updateStatus(id: Long, status: DownloadStatus, errorMessage: String? = null)

    /**
     * Update file path when download completes.
     */
    suspend fun updateFilePath(id: Long, filePath: String)

    /**
     * Cancel a download.
     */
    suspend fun cancelDownload(id: Long)

    /**
     * Retry a failed download.
     */
    suspend fun retryDownload(id: Long)

    /**
     * Remove a download task from the queue.
     */
    suspend fun removeDownload(id: Long)

    /**
     * Get next pending download task.
     */
    suspend fun getNextPendingDownload(): DownloadTask?

    /**
     * Update the download URL for a task (e.g. when retrying with fresh URL).
     */
    suspend fun updateDownloadUrl(id: Long, downloadUrl: String)

    /**
     * Reset downloads stuck in DOWNLOADING state back to PENDING.
     * This handles recovery from worker crashes.
     */
    suspend fun resetStuckDownloads()
}
