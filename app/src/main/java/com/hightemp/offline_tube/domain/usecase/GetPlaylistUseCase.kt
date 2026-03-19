package com.hightemp.offline_tube.domain.usecase

import com.hightemp.offline_tube.domain.model.Video
import com.hightemp.offline_tube.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

/**
 * Gets the list of downloaded videos (playlist).
 */
class GetPlaylistUseCase @Inject constructor(
    private val videoRepository: VideoRepository
) {
    operator fun invoke(): Flow<List<Video>> {
        Timber.d("GetPlaylistUseCase: observing downloaded videos")
        return videoRepository.observeDownloadedVideos()
    }
}
