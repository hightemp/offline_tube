package com.hightemp.offline_tube.domain.usecase

import com.hightemp.offline_tube.domain.model.Video
import com.hightemp.offline_tube.domain.model.YouTubeError
import com.hightemp.offline_tube.domain.repository.VideoRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Extracts video metadata from a YouTube URL.
 * Parses the video ID from various URL formats, then calls the repository.
 */
class ExtractVideoInfoUseCase @Inject constructor(
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke(url: String): Result<Video> {
        Timber.d("ExtractVideoInfoUseCase: extracting info from url=%s", url)

        val videoId = extractVideoId(url)
        if (videoId == null) {
            Timber.w("ExtractVideoInfoUseCase: invalid URL=%s", url)
            return Result.failure(YouTubeError.InvalidUrl(url))
        }

        Timber.d("ExtractVideoInfoUseCase: extracted videoId=%s", videoId)
        return videoRepository.extractVideoInfo(videoId)
    }

    companion object {
        private val URL_PATTERNS = listOf(
            Regex("""(?:youtube\.com/watch\?.*v=|youtu\.be/|youtube\.com/embed/|youtube\.com/v/|youtube\.com/shorts/)([a-zA-Z0-9_-]{11})"""),
            Regex("""^([a-zA-Z0-9_-]{11})$""")
        )

        fun extractVideoId(url: String): String? {
            val trimmed = url.trim()
            for (pattern in URL_PATTERNS) {
                pattern.find(trimmed)?.groupValues?.get(1)?.let { return it }
            }
            return null
        }
    }
}
