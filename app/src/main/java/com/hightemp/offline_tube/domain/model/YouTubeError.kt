package com.hightemp.offline_tube.domain.model

/**
 * Sealed class hierarchy for YouTube-specific errors.
 * Pure Kotlin — no Android dependencies.
 */
sealed class YouTubeError(override val message: String) : Exception(message) {
    data class InvalidUrl(val url: String) : YouTubeError("Invalid YouTube URL: $url")
    data class VideoUnavailable(val reason: String) : YouTubeError("Video unavailable: $reason")
    data class NetworkError(override val cause: Throwable) : YouTubeError("Network error: ${cause.message}")
    data class NoFormatsAvailable(val videoId: String) : YouTubeError("No downloadable formats for video: $videoId")
    data class DownloadFailed(val httpCode: Int, val reason: String) : YouTubeError("Download failed (HTTP $httpCode): $reason")
    data class StorageFull(val requiredBytes: Long) : YouTubeError("Not enough storage. Need ${requiredBytes / 1_048_576}MB.")
    data object ParsingError : YouTubeError("Failed to parse YouTube response")
}
