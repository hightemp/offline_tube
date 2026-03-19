package com.hightemp.offline_tube.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * InnerTube API response from the /player endpoint.
 * Only includes fields we actually use — ignores unknown keys.
 */
@Serializable
data class PlayerResponse(
    val playabilityStatus: PlayabilityStatus? = null,
    val videoDetails: VideoDetails? = null,
    val streamingData: StreamingData? = null,
    val microformat: Microformat? = null
)

@Serializable
data class PlayabilityStatus(
    val status: String? = null,
    val reason: String? = null,
    val playableInEmbed: Boolean? = null
)

@Serializable
data class VideoDetails(
    val videoId: String? = null,
    val title: String? = null,
    val lengthSeconds: String? = null,
    val channelId: String? = null,
    val shortDescription: String? = null,
    val thumbnail: ThumbnailContainer? = null,
    val viewCount: String? = null,
    val author: String? = null,
    val isLive: Boolean? = null,
    val isLiveContent: Boolean? = null,
    val keywords: List<String>? = null
)

@Serializable
data class ThumbnailContainer(
    val thumbnails: List<Thumbnail>? = null
)

@Serializable
data class Thumbnail(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
data class StreamingData(
    val formats: List<StreamFormat>? = null,
    val adaptiveFormats: List<StreamFormat>? = null,
    val hlsManifestUrl: String? = null,
    val dashManifestUrl: String? = null,
    val expiresInSeconds: String? = null
)

@Serializable
data class StreamFormat(
    val itag: Int? = null,
    val url: String? = null,
    val mimeType: String? = null,
    val bitrate: Long? = null,
    val averageBitrate: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val contentLength: String? = null,
    val quality: String? = null,
    val qualityLabel: String? = null,
    val fps: Int? = null,
    val audioQuality: String? = null,
    val audioSampleRate: String? = null,
    val audioChannels: Int? = null,
    val approxDurationMs: String? = null,
    val projectionType: String? = null,
    val signatureCipher: String? = null
)

@Serializable
data class Microformat(
    val playerMicroformatRenderer: PlayerMicroformatRenderer? = null
)

@Serializable
data class PlayerMicroformatRenderer(
    val title: TextContainer? = null,
    val description: TextContainer? = null,
    val uploadDate: String? = null,
    val category: String? = null,
    val isFamilySafe: Boolean? = null
)

@Serializable
data class TextContainer(
    val simpleText: String? = null
)
