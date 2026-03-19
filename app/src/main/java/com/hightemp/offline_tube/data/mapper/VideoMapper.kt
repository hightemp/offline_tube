package com.hightemp.offline_tube.data.mapper

import com.hightemp.offline_tube.data.local.entity.VideoEntity
import com.hightemp.offline_tube.data.remote.InnerTubeConfig
import com.hightemp.offline_tube.data.remote.dto.PlayerResponse
import com.hightemp.offline_tube.data.remote.dto.StreamFormat
import com.hightemp.offline_tube.domain.model.Video
import com.hightemp.offline_tube.domain.model.VideoFormat
import javax.inject.Inject

/**
 * Maps between Video domain model, VideoEntity (Room), and PlayerResponse (API).
 */
class VideoMapper @Inject constructor() {

    /**
     * Map InnerTube API response to domain Video model.
     */
    fun fromResponse(response: PlayerResponse): Video {
        val details = response.videoDetails
        val microformat = response.microformat?.playerMicroformatRenderer

        val thumbnailUrl = details?.thumbnail?.thumbnails
            ?.maxByOrNull { it.width ?: 0 }
            ?.url
            ?: details?.videoId?.let { InnerTubeConfig.buildThumbnailUrl(it) }
            ?: ""

        val formats = mutableListOf<VideoFormat>()

        // Map combined formats (audio+video)
        response.streamingData?.formats?.forEach { fmt ->
            mapStreamFormat(fmt, isCombined = true)?.let { formats.add(it) }
        }

        // Map adaptive formats
        response.streamingData?.adaptiveFormats?.forEach { fmt ->
            mapStreamFormat(fmt, isCombined = false)?.let { formats.add(it) }
        }

        return Video(
            videoId = details?.videoId ?: "",
            title = details?.title ?: "Unknown",
            description = details?.shortDescription ?: "",
            durationSeconds = details?.lengthSeconds?.toLongOrNull() ?: 0L,
            thumbnailUrl = thumbnailUrl,
            channelName = details?.author ?: "Unknown",
            channelId = details?.channelId ?: "",
            viewCount = details?.viewCount?.toLongOrNull() ?: 0L,
            uploadDate = microformat?.uploadDate ?: "",
            category = microformat?.category ?: "",
            isLive = details?.isLive ?: false,
            formats = formats
        )
    }

    private fun mapStreamFormat(fmt: StreamFormat, isCombined: Boolean): VideoFormat? {
        val url = fmt.url ?: return null
        val mimeType = fmt.mimeType ?: return null

        val hasVideo = mimeType.startsWith("video/")
        val hasAudio = isCombined || mimeType.startsWith("audio/") || fmt.audioQuality != null

        return VideoFormat(
            itag = fmt.itag ?: return null,
            url = url,
            mimeType = mimeType,
            quality = fmt.quality ?: "",
            qualityLabel = fmt.qualityLabel ?: "",
            width = fmt.width,
            height = fmt.height,
            fps = fmt.fps,
            bitrate = fmt.bitrate,
            contentLength = fmt.contentLength?.toLongOrNull(),
            hasAudio = hasAudio,
            hasVideo = hasVideo
        )
    }

    /**
     * Map domain Video to Room entity.
     */
    fun toEntity(video: Video): VideoEntity {
        return VideoEntity(
            videoId = video.videoId,
            title = video.title,
            description = video.description,
            durationSeconds = video.durationSeconds,
            thumbnailUrl = video.thumbnailUrl,
            channelName = video.channelName,
            channelId = video.channelId,
            viewCount = video.viewCount,
            uploadDate = video.uploadDate,
            category = video.category,
            isLive = video.isLive
        )
    }

    /**
     * Map Room entity to domain Video model. Formats are not stored in DB.
     */
    fun fromEntity(entity: VideoEntity): Video {
        return Video(
            videoId = entity.videoId,
            title = entity.title,
            description = entity.description,
            durationSeconds = entity.durationSeconds,
            thumbnailUrl = entity.thumbnailUrl,
            channelName = entity.channelName,
            channelId = entity.channelId,
            viewCount = entity.viewCount,
            uploadDate = entity.uploadDate,
            category = entity.category,
            isLive = entity.isLive,
            formats = emptyList(),
            filePath = entity.filePath
        )
    }
}
