package com.hightemp.offline_tube.domain.model

/**
 * Download status for a video in the download queue.
 */
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
