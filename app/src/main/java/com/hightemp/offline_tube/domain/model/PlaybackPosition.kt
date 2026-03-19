package com.hightemp.offline_tube.domain.model

/**
 * Tracks playback position for a video so user can resume from where they stopped.
 */
data class PlaybackPosition(
    val videoId: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAt: Long = System.currentTimeMillis()
)
