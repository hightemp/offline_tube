package com.hightemp.offline_tube.data.remote

/**
 * InnerTube API configuration constants.
 * Uses ANDROID_VR client identity — provides direct URLs without cipher or PO tokens.
 */
object InnerTubeConfig {
    const val API_URL = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
    const val CLIENT_NAME = "ANDROID_VR"
    const val CLIENT_VERSION = "1.65.10"
    const val CLIENT_NAME_ID = "28"
    const val DEVICE_MAKE = "Oculus"
    const val DEVICE_MODEL = "Quest 3"
    const val ANDROID_SDK_VERSION = 32
    const val OS_NAME = "Android"
    const val OS_VERSION = "12L"
    const val USER_AGENT = "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"

    fun buildThumbnailUrl(videoId: String, quality: String = "hqdefault"): String {
        return "https://i.ytimg.com/vi/$videoId/$quality.jpg"
    }
}
