package com.hightemp.offline_tube.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages YouTube visitor data (session bootstrap).
 *
 * YouTube requires a valid `visitorData` token in the `X-Goog-Visitor-Id` header
 * for InnerTube API calls to succeed without authentication.
 * Without it, requests return `LOGIN_REQUIRED` ("Sign in to confirm you're not a bot").
 *
 * The visitor data is obtained by fetching any YouTube page and extracting
 * the `VISITOR_DATA` field from the embedded `ytcfg.set({...})` JSON.
 *
 * The extracted value is cached in memory and reused across requests.
 * yt-dlp uses the same approach.
 */
@Singleton
class VisitorDataManager @Inject constructor(
    private val httpClient: OkHttpClient
) {
    @Volatile
    private var cachedVisitorData: String? = null

    companion object {
        /** User-Agent mimicking Safari on macOS — used for the initial page fetch only. */
        private const val WEB_SAFARI_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"

        /** Regex to extract VISITOR_DATA from ytcfg.set({...}) in the page HTML. */
        private val VISITOR_DATA_REGEX = Regex(""""VISITOR_DATA"\s*:\s*"([^"]+)"""")

        /** Regex to extract visitorData from responseContext or client context. */
        private val VISITOR_DATA_CONTEXT_REGEX = Regex(""""visitorData"\s*:\s*"([^"]+)"""")
    }

    /**
     * Get a valid visitor data token, fetching from YouTube if not cached.
     * @param videoId Optional video ID to fetch a specific page (improves cache locality).
     * @return Visitor data string, or null if extraction fails.
     */
    suspend fun getVisitorData(videoId: String? = null): String? {
        cachedVisitorData?.let { return it }

        return try {
            fetchVisitorData(videoId).also { data ->
                if (data != null) {
                    cachedVisitorData = data
                    Timber.d("VisitorDataManager: obtained visitor data: %s", data.take(20) + "...")
                } else {
                    Timber.w("VisitorDataManager: failed to extract visitor data from page")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "VisitorDataManager: error fetching visitor data")
            null
        }
    }

    /**
     * Clear the cached visitor data (e.g., after a LOGIN_REQUIRED response).
     */
    fun invalidate() {
        cachedVisitorData = null
        Timber.d("VisitorDataManager: cache invalidated")
    }

    /**
     * Fetch a YouTube page and extract visitor data from ytcfg.
     */
    private fun fetchVisitorData(videoId: String?): String? {
        val url = if (videoId != null) {
            "https://www.youtube.com/watch?v=$videoId&bpctr=9999999999&has_verified=1"
        } else {
            "https://www.youtube.com/?bpctr=9999999999&has_verified=1"
        }

        Timber.d("VisitorDataManager: fetching page %s", url)

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", WEB_SAFARI_USER_AGENT)
            .addHeader("Accept-Language", "en-US,en;q=0.9")
            .addHeader("Cookie", "SOCS=CAI; PREF=hl=en&tz=UTC")
            .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            Timber.w("VisitorDataManager: HTTP %d fetching YouTube page", response.code)
            throw IOException("Failed to fetch YouTube page: HTTP ${response.code}")
        }

        val body = response.body?.string() ?: return null

        // Try VISITOR_DATA from ytcfg first
        VISITOR_DATA_REGEX.find(body)?.groupValues?.get(1)?.let { return it }

        // Fallback: try visitorData from embedded JSON
        VISITOR_DATA_CONTEXT_REGEX.find(body)?.groupValues?.get(1)?.let { return it }

        Timber.w("VisitorDataManager: no visitor data found in %d byte page", body.length)
        return null
    }
}
