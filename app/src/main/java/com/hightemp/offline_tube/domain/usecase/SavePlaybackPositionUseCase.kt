package com.hightemp.offline_tube.domain.usecase

import com.hightemp.offline_tube.domain.model.PlaybackPosition
import com.hightemp.offline_tube.domain.repository.VideoRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Saves the playback position for a video so it can be resumed later.
 */
class SavePlaybackPositionUseCase @Inject constructor(
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke(videoId: String, positionMs: Long, durationMs: Long) {
        Timber.d("SavePlaybackPositionUseCase: videoId=%s positionMs=%d durationMs=%d", videoId, positionMs, durationMs)
        videoRepository.savePlaybackPosition(
            PlaybackPosition(
                videoId = videoId,
                positionMs = positionMs,
                durationMs = durationMs
            )
        )
    }
}
