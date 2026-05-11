package com.hightemp.offline_tube.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds session bootstrap data extracted from a YouTube page.
 */
data class SessionData(
    val visitorData: String?,
    val signatureTimestamp: Int?
)

/**
 * Manages YouTube session bootstrap data: visitorData and signatureTimestamp (STS).
 *
 * YouTube requires a valid `visitorData` token in the `X-Goog-Visitor-Id` header.
 * Without it, requests return `LOGIN_REQUIRED` ("Sign in to confirm you're not a bot").
 *
 * Since early 2026, YouTube also requires `signatureTimestamp` (STS from ytcfg) in
 * `playbackContext.contentPlaybackContext.signatureTimestamp`. Without it, the android_vr
 * and tv clients return LOGIN_REQUIRED even with a valid visitor token.
 *
 * Both values are obtained by fetching any YouTube page and extracting from ytcfg.set({...}).
 * yt-dlp uses the same approach (_extract_visitor_data + STS from ytcfg['STS']).
 */
@Singleton
class VisitorDataManager @Inject constructor(
    private val httpClient: OkHttpClient
) {
    @Volatile
    private var cachedSession: SessionData? = null

    companion object {
        /** User-Agent mimicking Safari on macOS — used for the initial page fetch only. */
        private const val WEB_SAFARI_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"

        /** Regex to extract VISITOR_DATA from ytcfg.set({...}) in the page HTML. */
        private val VISITOR_DATA_REGEX = Regex(""""VISITOR_DATA"\s*:\s*"([^"]+)"""")

        /** Regex to extract visitorData from responseContext or client context. */
        private val VISITOR_DATA_CONTEXT_REGEX = Regex(""""visitorData"\s*:\s*"([^"]+)"""")

        /** Regex to extract STS (signatureTimestamp) from ytcfg — required in playbackContext. */
        private val STS_REGEX = Regex(""""STS"\s*:\s*(\d+)""")
    }

    /**
     * Get session bootstrap data (visitorData + signatureTimestamp), fetching from YouTube if not cached.
     * @param videoId Optional video ID to fetch a specific page.
     * @return SessionData with visitor data and STS (either may be null if extraction fails).
     */
    suspend fun getSessionData(videoId: String? = null): SessionData {
        cachedSession?.let { return it }

        return try {
            fetchSessionData(videoId).also { session ->
                cachedSession = session
                Timber.d(
                    "VisitorDataManager: visitorData=%s STS=%s",
                    session.visitorData?.take(20) ?: "null",
                    session.signatureTimestamp
                )
                if (session.visitorData == null) {
                    Timber.w("VisitorDataManager: failed to extract visitor data from page")
                }
                if (session.signatureTimestamp == null) {
                    Timber.w("VisitorDataManager: failed to extract STS from page")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "VisitorDataManager: error fetching session data")
            SessionData(null, null)
        }
    }

    /**
     * Clear the cached session (e.g., after a LOGIN_REQUIRED response).
     */
    fun invalidate() {
        cachedSession = null
        Timber.d("VisitorDataManager: cache invalidated")
    }

    /**
     * Fetch a YouTube page and extract visitor data and STS from ytcfg.
     */
    private fun fetchSessionData(videoId: String?): SessionData {
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

        val body = response.body?.string() ?: return SessionData(null, null)

        // Extract VISITOR_DATA from ytcfg first, fallback to visitorData from embedded JSON
        val visitorData = VISITOR_DATA_REGEX.find(body)?.groupValues?.get(1)
            ?: VISITOR_DATA_CONTEXT_REGEX.find(body)?.groupValues?.get(1)

        if (visitorData == null) {
            Timber.w("VisitorDataManager: no visitor data found in %d byte page", body.length)
        }

        // Extract STS (signatureTimestamp) — required in playbackContext since early 2026
        val sts = STS_REGEX.find(body)?.groupValues?.get(1)?.toIntOrNull()

        return SessionData(visitorData, sts)
    }
}
