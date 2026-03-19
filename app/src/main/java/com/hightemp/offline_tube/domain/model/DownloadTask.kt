package com.hightemp.offline_tube.domain.model

/**
 * Domain entity representing a download task in the queue.
 */
data class DownloadTask(
    val id: Long = 0,
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val filePath: String? = null,
    val selectedQuality: VideoQuality = VideoQuality.Q_720P,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val downloadUrl: String? = null
)
