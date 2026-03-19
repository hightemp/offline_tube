package com.hightemp.offline_tube.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * InnerTube API request body for the /player endpoint.
 */
@Serializable
data class PlayerRequest(
    val context: Context,
    val videoId: String,
    val playbackContext: PlaybackContext = PlaybackContext(),
    val contentCheckOk: Boolean = true,
    val racyCheckOk: Boolean = true
) {
    @Serializable
    data class Context(
        val client: Client
    )

    @Serializable
    data class Client(
        val clientName: String,
        val clientVersion: String,
        val deviceMake: String,
        val deviceModel: String,
        val androidSdkVersion: Int,
        val userAgent: String,
        val osName: String,
        val osVersion: String,
        val hl: String = "en",
        val timeZone: String = "UTC",
        val utcOffsetMinutes: Int = 0
    )

    @Serializable
    data class PlaybackContext(
        val contentPlaybackContext: ContentPlaybackContext = ContentPlaybackContext()
    )

    @Serializable
    data class ContentPlaybackContext(
        val html5Preference: String = "HTML5_PREF_WANTS"
    )
}
