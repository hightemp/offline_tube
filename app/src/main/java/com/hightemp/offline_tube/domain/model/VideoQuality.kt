package com.hightemp.offline_tube.domain.model

/**
 * Video quality options available for download.
 */
enum class VideoQuality(val maxHeight: Int, val label: String) {
    Q_360P(360, "360p"),
    Q_480P(480, "480p"),
    Q_720P(720, "720p"),
    Q_1080P(1080, "1080p");

    companion object {
        fun fromHeight(height: Int): VideoQuality {
            return entries.firstOrNull { it.maxHeight == height } ?: Q_720P
        }

        fun fromLabel(label: String): VideoQuality {
            return entries.firstOrNull { it.label == label } ?: Q_720P
        }
    }
}
