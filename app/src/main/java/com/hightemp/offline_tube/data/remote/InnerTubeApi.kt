package com.hightemp.offline_tube.data.remote

import com.hightemp.offline_tube.data.remote.dto.PlayerRequest
import com.hightemp.offline_tube.data.remote.dto.PlayerResponse
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client for YouTube's InnerTube API.
 * Uses the ANDROID_VR client identity to get direct download URLs.
 */
@Singleton
class InnerTubeApi @Inject constructor(
    private val httpClient: OkHttpClient,
    private val json: Json
) {
    /**
     * Fetch video player data from InnerTube API.
     * @param videoId The 11-character YouTube video ID.
     * @return Parsed PlayerResponse with video metadata and streaming URLs.
     * @throws IOException on network errors.
     * @throws kotlinx.serialization.SerializationException on parse errors.
     */
    suspend fun getPlayerResponse(videoId: String): PlayerResponse {
        Timber.d("InnerTubeApi: requesting player data for videoId=%s", videoId)

        val requestBody = buildRequestBody(videoId)
        val requestJson = json.encodeToString(PlayerRequest.serializer(), requestBody)

        Timber.d("InnerTubeApi: request body size=%d bytes", requestJson.length)

        val request = Request.Builder()
            .url(InnerTubeConfig.API_URL)
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .addHeader("User-Agent", InnerTubeConfig.USER_AGENT)
            .addHeader("X-YouTube-Client-Name", InnerTubeConfig.CLIENT_NAME_ID)
            .addHeader("X-YouTube-Client-Version", InnerTubeConfig.CLIENT_VERSION)
            .addHeader("Origin", "https://www.youtube.com")
            .addHeader("Content-Type", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()

        Timber.d("InnerTubeApi: response code=%d for videoId=%s", response.code, videoId)

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "No response body"
            Timber.e("InnerTubeApi: HTTP error %d: %s", response.code, errorBody.take(200))
            throw IOException("InnerTube API error: HTTP ${response.code}")
        }

        val responseBody = response.body?.string()
            ?: throw IOException("Empty response body from InnerTube API")

        Timber.d("InnerTubeApi: response body size=%d bytes for videoId=%s", responseBody.length, videoId)

        val playerResponse = json.decodeFromString(PlayerResponse.serializer(), responseBody)

        Timber.d(
            "InnerTubeApi: parsed response status=%s formats=%d adaptiveFormats=%d for videoId=%s",
            playerResponse.playabilityStatus?.status,
            playerResponse.streamingData?.formats?.size ?: 0,
            playerResponse.streamingData?.adaptiveFormats?.size ?: 0,
            videoId
        )

        return playerResponse
    }

    private fun buildRequestBody(videoId: String): PlayerRequest {
        return PlayerRequest(
            context = PlayerRequest.Context(
                client = PlayerRequest.Client(
                    clientName = InnerTubeConfig.CLIENT_NAME,
                    clientVersion = InnerTubeConfig.CLIENT_VERSION,
                    deviceMake = InnerTubeConfig.DEVICE_MAKE,
                    deviceModel = InnerTubeConfig.DEVICE_MODEL,
                    androidSdkVersion = InnerTubeConfig.ANDROID_SDK_VERSION,
                    userAgent = InnerTubeConfig.USER_AGENT,
                    osName = InnerTubeConfig.OS_NAME,
                    osVersion = InnerTubeConfig.OS_VERSION
                )
            ),
            videoId = videoId
        )
    }
}
