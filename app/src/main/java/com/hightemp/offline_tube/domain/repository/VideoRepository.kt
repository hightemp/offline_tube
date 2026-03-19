package com.hightemp.offline_tube.domain.repository

import com.hightemp.offline_tube.domain.model.Video
import com.hightemp.offline_tube.domain.model.VideoFormat
import com.hightemp.offline_tube.domain.model.PlaybackPosition
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for video metadata and playback operations.
 * Implemented in data layer.
 */
interface VideoRepository {
    /**
     * Extract video metadata from YouTube by video ID.
     * Returns video info including available formats.
     */
    suspend fun extractVideoInfo(videoId: String): Result<Video>

    /**
     * Get all downloaded videos as a reactive Flow.
     */
    fun observeDownloadedVideos(): Flow<List<Video>>

    /**
     * Get a single video by its YouTube video ID.
     */
    suspend fun getVideoById(videoId: String): Video?

    /**
     * Delete a downloaded video (metadata + file).
     */
    suspend fun deleteVideo(videoId: String)

    /**
     * Save playback position for a video.
     */
    suspend fun savePlaybackPosition(position: PlaybackPosition)

    /**
     * Get the last playback position for a video.
     */
    suspend fun getPlaybackPosition(videoId: String): PlaybackPosition?

    /**
     * Observe playback position changes.
     */
    fun observePlaybackPosition(videoId: String): Flow<PlaybackPosition?>
}
