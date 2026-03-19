package com.hightemp.offline_tube.data.mapper

import com.hightemp.offline_tube.data.local.entity.DownloadEntity
import com.hightemp.offline_tube.data.local.entity.PlaybackPositionEntity
import com.hightemp.offline_tube.domain.model.DownloadStatus
import com.hightemp.offline_tube.domain.model.DownloadTask
import com.hightemp.offline_tube.domain.model.PlaybackPosition
import com.hightemp.offline_tube.domain.model.VideoQuality
import javax.inject.Inject

/**
 * Maps between DownloadTask / PlaybackPosition domain models and Room entities.
 */
class DownloadMapper @Inject constructor() {

    fun toEntity(task: DownloadTask): DownloadEntity {
        return DownloadEntity(
            id = task.id,
            videoId = task.videoId,
            title = task.title,
            thumbnailUrl = task.thumbnailUrl,
            status = task.status.name,
            progress = task.progress,
            downloadedBytes = task.downloadedBytes,
            totalBytes = task.totalBytes,
            filePath = task.filePath,
            selectedQuality = task.selectedQuality.name,
            errorMessage = task.errorMessage,
            createdAt = task.createdAt,
            downloadUrl = task.downloadUrl
        )
    }

    fun fromEntity(entity: DownloadEntity): DownloadTask {
        return DownloadTask(
            id = entity.id,
            videoId = entity.videoId,
            title = entity.title,
            thumbnailUrl = entity.thumbnailUrl,
            status = try { DownloadStatus.valueOf(entity.status) } catch (_: Exception) { DownloadStatus.PENDING },
            progress = entity.progress,
            downloadedBytes = entity.downloadedBytes,
            totalBytes = entity.totalBytes,
            filePath = entity.filePath,
            selectedQuality = try { VideoQuality.valueOf(entity.selectedQuality) } catch (_: Exception) { VideoQuality.Q_720P },
            errorMessage = entity.errorMessage,
            createdAt = entity.createdAt,
            downloadUrl = entity.downloadUrl
        )
    }

    fun toPositionEntity(position: PlaybackPosition): PlaybackPositionEntity {
        return PlaybackPositionEntity(
            videoId = position.videoId,
            positionMs = position.positionMs,
            durationMs = position.durationMs,
            lastPlayedAt = position.lastPlayedAt
        )
    }

    fun fromPositionEntity(entity: PlaybackPositionEntity): PlaybackPosition {
        return PlaybackPosition(
            videoId = entity.videoId,
            positionMs = entity.positionMs,
            durationMs = entity.durationMs,
            lastPlayedAt = entity.lastPlayedAt
        )
    }
}
