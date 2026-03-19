package com.hightemp.offline_tube.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for tracking video playback positions.
 */
@Entity(tableName = "playback_positions")
data class PlaybackPositionEntity(
    @PrimaryKey
    val videoId: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAt: Long = System.currentTimeMillis()
)
