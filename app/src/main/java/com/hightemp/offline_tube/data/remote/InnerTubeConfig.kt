package com.hightemp.offline_tube.data.remote

/**
 * InnerTube API configuration with multiple client identities for fallback.
 * Mirrors yt-dlp's strategy: android_vr → tv → web_embedded.
 * These clients provide direct URLs without cipher, PO tokens, or authentication.
 */
object InnerTubeConfig {
    const val API_URL = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"

    /**
     * Represents an InnerTube client identity used for /player requests.
     */
    data class ClientProfile(
        val clientName: String,
        val clientVersion: String,
        val clientNameId: String,
        val userAgent: String,
        val deviceMake: String? = null,
        val deviceModel: String? = null,
        val androidSdkVersion: Int? = null,
        val osName: String? = null,
        val osVersion: String? = null,
        /** If non-null, add thirdParty.embedUrl to the request context */
        val embedUrlTemplate: String? = null
    )

    /** Primary client — no JS, no PO tokens, no auth needed. "Made for kids" may not work. */
    val ANDROID_VR = ClientProfile(
        clientName = "ANDROID_VR",
        clientVersion = "1.65.10",
        clientNameId = "28",
        deviceMake = "Oculus",
        deviceModel = "Quest 3",
        androidSdkVersion = 32,
        osName = "Android",
        osVersion = "12L",
        userAgent = "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
    )

    /** TV client — Cobalt-based, no auth for unauthenticated use. */
    val TV = ClientProfile(
        clientName = "TVHTML5",
        clientVersion = "7.20260114.12.00",
        clientNameId = "7",
        userAgent = "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/25.lts.30.1034943-gold (unlike Gecko), Unknown_TV_Unknown_0/Unknown (Unknown, Unknown)"
    )

    /** Embedded web player — needs thirdParty.embedUrl in context. */
    val WEB_EMBEDDED = ClientProfile(
        clientName = "WEB_EMBEDDED_PLAYER",
        clientVersion = "1.20260115.01.00",
        clientNameId = "56",
        userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
        embedUrlTemplate = "https://www.youtube.com/embed/{VIDEO_ID}?html5=1"
    )

    /** Ordered list of clients to try. First success wins. */
    val FALLBACK_CLIENTS = listOf(ANDROID_VR, TV, WEB_EMBEDDED)

    /** Statuses that are acceptable (don't trigger fallback). */
    val ACCEPTABLE_STATUSES = setOf("OK", "LIVE_STREAM_OFFLINE")

    fun buildThumbnailUrl(videoId: String, quality: String = "hqdefault"): String {
        return "https://i.ytimg.com/vi/$videoId/$quality.jpg"
    }
}
