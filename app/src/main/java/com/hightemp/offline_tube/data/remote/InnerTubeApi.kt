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
 *
 * Implements yt-dlp's two-phase approach:
 * 1. Fetch a YouTube webpage to obtain visitor data (session bootstrap)
 * 2. Send /player API requests with the visitor data in X-Goog-Visitor-Id header
 *
 * Tries multiple client identities (android_vr → tv → web_embedded) to avoid
 * LOGIN_REQUIRED / UNPLAYABLE responses.
 */
@Singleton
class InnerTubeApi @Inject constructor(
    private val httpClient: OkHttpClient,
    private val json: Json,
    private val visitorDataManager: VisitorDataManager
) {
    companion object {
        /** Cookies sent with every API request, matching yt-dlp's _real_initialize. */
        private const val CONSENT_COOKIES = "SOCS=CAI; PREF=hl=en&tz=UTC"
    }

    /**
     * Fetch video player data from InnerTube API with automatic client fallback.
     * First obtains visitor data from a YouTube page, then iterates through
     * [InnerTubeConfig.FALLBACK_CLIENTS] until a successful response is obtained.
     *
     * @param videoId The 11-character YouTube video ID.
     * @return Parsed PlayerResponse with video metadata and streaming URLs.
     * @throws IOException on network errors or when all clients fail.
     */
    suspend fun getPlayerResponse(videoId: String): PlayerResponse {
        Timber.d("InnerTubeApi: requesting player data for videoId=%s", videoId)

        // Phase 1: Obtain visitor data (bootstrap session)
        val visitorData = visitorDataManager.getVisitorData(videoId)
        Timber.d("InnerTubeApi: visitorData=%s", visitorData?.take(20) ?: "null")

        var lastResponse: PlayerResponse? = null
        var lastError: Exception? = null

        for (client in InnerTubeConfig.FALLBACK_CLIENTS) {
            try {
                Timber.d("InnerTubeApi: trying client=%s for videoId=%s", client.clientName, videoId)
                val response = fetchWithClient(videoId, client, visitorData)
                val status = response.playabilityStatus?.status

                if (status in InnerTubeConfig.ACCEPTABLE_STATUSES && response.streamingData != null) {
                    Timber.d(
                        "InnerTubeApi: SUCCESS with client=%s status=%s formats=%d adaptive=%d",
                        client.clientName, status,
                        response.streamingData.formats?.size ?: 0,
                        response.streamingData.adaptiveFormats?.size ?: 0
                    )
                    return response
                }

                // If LOGIN_REQUIRED, invalidate visitor data for next attempt
                if (status == "LOGIN_REQUIRED") {
                    Timber.w("InnerTubeApi: LOGIN_REQUIRED from client=%s, invalidating visitor data", client.clientName)
                    visitorDataManager.invalidate()
                }

                Timber.w(
                    "InnerTubeApi: client=%s returned status=%s reason=%s hasStreaming=%b",
                    client.clientName, status,
                    response.playabilityStatus?.reason ?: "none",
                    response.streamingData != null
                )
                lastResponse = response
            } catch (e: Exception) {
                Timber.w(e, "InnerTubeApi: client=%s failed with exception", client.clientName)
                lastError = e
            }
        }

        // All clients exhausted — return best available response or throw
        lastResponse?.let { return it }
        throw lastError ?: IOException("All InnerTube clients failed for videoId=$videoId")
    }

    /**
     * Make a single /player request using the given client profile.
     */
    private fun fetchWithClient(
        videoId: String,
        client: InnerTubeConfig.ClientProfile,
        visitorData: String?
    ): PlayerResponse {
        val requestBody = buildRequestBody(videoId, client)
        val requestJson = json.encodeToString(PlayerRequest.serializer(), requestBody)

        val requestBuilder = Request.Builder()
            .url(InnerTubeConfig.API_URL)
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .addHeader("User-Agent", client.userAgent)
            .addHeader("X-YouTube-Client-Name", client.clientNameId)
            .addHeader("X-YouTube-Client-Version", client.clientVersion)
            .addHeader("Origin", "https://www.youtube.com")
            .addHeader("Content-Type", "application/json")
            .addHeader("Cookie", CONSENT_COOKIES)

        // Critical header: visitor data from webpage bootstrap
        if (visitorData != null) {
            requestBuilder.addHeader("X-Goog-Visitor-Id", visitorData)
        }

        val response = httpClient.newCall(requestBuilder.build()).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "No response body"
            Timber.e("InnerTubeApi: HTTP %d from client=%s: %s", response.code, client.clientName, errorBody.take(200))
            throw IOException("InnerTube API error: HTTP ${response.code} (${client.clientName})")
        }

        val responseBody = response.body?.string()
            ?: throw IOException("Empty response body from InnerTube API (${client.clientName})")

        return json.decodeFromString(PlayerResponse.serializer(), responseBody)
    }

    private fun buildRequestBody(
        videoId: String,
        client: InnerTubeConfig.ClientProfile
    ): PlayerRequest {
        val thirdParty = client.embedUrlTemplate?.let { template ->
            PlayerRequest.ThirdParty(
                embedUrl = template.replace("{VIDEO_ID}", videoId)
            )
        }

        return PlayerRequest(
            context = PlayerRequest.Context(
                client = PlayerRequest.Client(
                    clientName = client.clientName,
                    clientVersion = client.clientVersion,
                    deviceMake = client.deviceMake,
                    deviceModel = client.deviceModel,
                    androidSdkVersion = client.androidSdkVersion,
                    userAgent = client.userAgent,
                    osName = client.osName,
                    osVersion = client.osVersion
                ),
                thirdParty = thirdParty
            ),
            videoId = videoId
        )
    }
}
