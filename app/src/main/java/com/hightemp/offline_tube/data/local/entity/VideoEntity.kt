package com.hightemp.offline_tube.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing video metadata.
 */
@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val description: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val channelName: String,
    val channelId: String,
    val viewCount: Long,
    val uploadDate: String,
    val category: String,
    val isLive: Boolean = false,
    val filePath: String? = null,
    val isDownloaded: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
