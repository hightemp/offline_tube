package com.hightemp.offline_tube.domain.usecase

import com.hightemp.offline_tube.domain.model.Video
import com.hightemp.offline_tube.domain.model.VideoFormat
import com.hightemp.offline_tube.domain.model.VideoQuality
import com.hightemp.offline_tube.domain.model.YouTubeError
import com.hightemp.offline_tube.domain.repository.DownloadRepository
import com.hightemp.offline_tube.domain.repository.SettingsRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Enqueues a video for download. Selects the best format based on quality settings.
 */
class DownloadVideoUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(video: Video, quality: VideoQuality? = null): Result<Long> {
        Timber.d("DownloadVideoUseCase: starting download for videoId=%s title=%s", video.videoId, video.title)

        val selectedQuality = quality ?: settingsRepository.getVideoQuality()
        Timber.d("DownloadVideoUseCase: selected quality=%s", selectedQuality.label)

        val format = selectBestFormat(video.formats, selectedQuality)
        if (format == null) {
            Timber.w("DownloadVideoUseCase: no formats available for videoId=%s quality=%s", video.videoId, selectedQuality.label)
            return Result.failure(YouTubeError.NoFormatsAvailable(video.videoId))
        }

        Timber.d("DownloadVideoUseCase: selected format itag=%d quality=%s size=%d", format.itag, format.qualityLabel, format.contentLength ?: -1)

        // Check if already queued
        val existingDownload = downloadRepository.getDownloadByVideoId(video.videoId)
        if (existingDownload != null) {
            Timber.d("DownloadVideoUseCase: video already in queue, id=%d status=%s", existingDownload.id, existingDownload.status)
            return Result.success(existingDownload.id)
        }

        val taskId = downloadRepository.enqueueDownload(
            videoId = video.videoId,
            title = video.title,
            thumbnailUrl = video.thumbnailUrl,
            downloadUrl = format.url,
            quality = selectedQuality,
            totalBytes = format.contentLength ?: 0L
        )

        Timber.d("DownloadVideoUseCase: enqueued download taskId=%d", taskId)
        return Result.success(taskId)
    }

    /**
     * Select best combined format (has both audio+video) within quality limit.
     * Prefers combined formats to avoid muxing.
     */
    private fun selectBestFormat(formats: List<VideoFormat>, quality: VideoQuality): VideoFormat? {
        // Prefer formats with both audio and video
        val combined = formats
            .filter { it.hasAudio && it.hasVideo && it.height != null && it.height <= quality.maxHeight }
            .sortedByDescending { it.height }
            .firstOrNull()

        if (combined != null) return combined

        // Fallback: any format with video within limits
        return formats
            .filter { it.hasVideo && it.height != null && it.height <= quality.maxHeight }
            .sortedByDescending { it.height }
            .firstOrNull()
    }
}
