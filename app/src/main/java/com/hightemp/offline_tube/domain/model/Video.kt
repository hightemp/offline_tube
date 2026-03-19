package com.hightemp.offline_tube.domain.model

/**
 * Domain entity representing a YouTube video's metadata.
 */
data class Video(
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
    val formats: List<VideoFormat> = emptyList(),
    val filePath: String? = null
)

/**
 * Represents a single available video format/quality option.
 */
data class VideoFormat(
    val itag: Int,
    val url: String,
    val mimeType: String,
    val quality: String,
    val qualityLabel: String,
    val width: Int?,
    val height: Int?,
    val fps: Int?,
    val bitrate: Long?,
    val contentLength: Long?,
    val hasAudio: Boolean,
    val hasVideo: Boolean
)
