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
        val client: Client,
        val thirdParty: ThirdParty? = null
    )

    @Serializable
    data class Client(
        val clientName: String,
        val clientVersion: String,
        val deviceMake: String? = null,
        val deviceModel: String? = null,
        val androidSdkVersion: Int? = null,
        val userAgent: String? = null,
        val osName: String? = null,
        val osVersion: String? = null,
        val hl: String = "en",
        val timeZone: String = "UTC",
        val utcOffsetMinutes: Int = 0
    )

    @Serializable
    data class ThirdParty(
        val embedUrl: String
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
