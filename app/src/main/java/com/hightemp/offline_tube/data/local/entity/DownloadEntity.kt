package com.hightemp.offline_tube.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the download queue.
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val status: String,
    val progress: Int = 0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val filePath: String? = null,
    val selectedQuality: String,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val downloadUrl: String? = null
)
