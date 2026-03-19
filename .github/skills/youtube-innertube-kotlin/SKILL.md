---
name: youtube-innertube-kotlin
description: Guide for implementing YouTube video downloading in Kotlin using the InnerTube API directly. Covers API endpoints, client configs, format extraction, metadata parsing, and download implementation for an Android app without any Python/yt-dlp dependency.
metadata:
  author: offline_tube
  version: "1.0"
  category: android-development
---

# YouTube InnerTube API — Kotlin Implementation Guide

## 1. Overview

YouTube's InnerTube API is the internal API used by all YouTube clients. By mimicking the ANDROID_VR client, we can get direct video download URLs without needing:
- JavaScript player parsing
- Signature cipher decryption
- PO (Proof of Origin) tokens
- Browser automation

### Limitations of ANDROID_VR client
- "Made for kids" videos may not be available
- Some age-restricted content may be blocked
- YouTube may change the API at any time (monitor yt-dlp changelog for updates)

---

## 2. API Endpoint

```
POST https://www.youtube.com/youtubei/v1/player?prettyPrint=false
```

### Required Headers
```
Content-Type: application/json
User-Agent: com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip
X-YouTube-Client-Name: 28
X-YouTube-Client-Version: 1.65.10
Origin: https://www.youtube.com
```

### Request Body
```json
{
  "context": {
    "client": {
      "clientName": "ANDROID_VR",
      "clientVersion": "1.65.10",
      "deviceMake": "Oculus",
      "deviceModel": "Quest 3",
      "androidSdkVersion": 32,
      "userAgent": "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
      "osName": "Android",
      "osVersion": "12L",
      "hl": "en",
      "timeZone": "UTC",
      "utcOffsetMinutes": 0
    }
  },
  "videoId": "VIDEO_ID_HERE",
  "playbackContext": {
    "contentPlaybackContext": {
      "html5Preference": "HTML5_PREF_WANTS"
    }
  },
  "contentCheckOk": true,
  "racyCheckOk": true
}
```

---

## 3. Response Structure

### Key Response Fields

```kotlin
// Playability check
response.playabilityStatus.status  // "OK", "ERROR", "UNPLAYABLE", "LOGIN_REQUIRED"
response.playabilityStatus.reason  // Error message if not OK

// Video metadata
response.videoDetails.videoId
response.videoDetails.title
response.videoDetails.lengthSeconds  // String, convert to Long
response.videoDetails.channelId
response.videoDetails.shortDescription
response.videoDetails.thumbnail.thumbnails  // List of {url, width, height}
response.videoDetails.viewCount  // String
response.videoDetails.isLive  // Boolean
response.videoDetails.keywords  // List<String>

// Streaming data
response.streamingData.formats           // Combined audio+video (360p, 720p)
response.streamingData.adaptiveFormats   // Separate audio/video (higher quality)

// Microformat (extra metadata)
response.microformat.playerMicroformatRenderer.uploadDate
response.microformat.playerMicroformatRenderer.category
```

### Format Object Fields
```kotlin
data class StreamFormat(
    val itag: Int,
    val url: String?,                    // Direct download URL (ANDROID_VR always has this)
    val mimeType: String,                // "video/mp4; codecs=\"avc1.64001F, mp4a.40.2\""
    val bitrate: Long?,
    val averageBitrate: Long?,
    val width: Int?,
    val height: Int?,
    val contentLength: String?,          // File size in bytes (as string)
    val quality: String?,                // "hd720", "medium", "small"
    val qualityLabel: String?,           // "720p", "360p"
    val fps: Int?,
    val audioQuality: String?,           // "AUDIO_QUALITY_MEDIUM"
    val audioSampleRate: String?,
    val audioChannels: Int?,
    val approxDurationMs: String?,
    val projectionType: String?,
    val signatureCipher: String? = null  // Only for web client (not ANDROID_VR)
)
```

---

## 4. Kotlin Implementation Patterns

### Video ID Extraction from URL
```kotlin
fun extractVideoId(url: String): String? {
    val patterns = listOf(
        Regex("""(?:youtube\.com/watch\?.*v=|youtu\.be/|youtube\.com/embed/|youtube\.com/v/|youtube\.com/shorts/)([a-zA-Z0-9_-]{11})"""),
        Regex("""^([a-zA-Z0-9_-]{11})$""")  // Plain video ID
    )
    for (pattern in patterns) {
        pattern.find(url)?.groupValues?.get(1)?.let { return it }
    }
    return null
}
```

### Format Selection Logic
```kotlin
fun selectFormat(
    formats: List<StreamFormat>,
    adaptiveFormats: List<StreamFormat>,
    maxHeight: Int = 720
): StreamFormat? {
    // Prefer combined formats (no need for muxing)
    val combined = formats
        .filter { it.url != null && it.height != null && it.height <= maxHeight }
        .sortedByDescending { it.height }
        .firstOrNull()
    
    if (combined != null) return combined
    
    // Fallback: best adaptive format within limit
    return adaptiveFormats
        .filter { 
            it.url != null && 
            it.height != null && 
            it.height <= maxHeight &&
            it.mimeType.startsWith("video/") &&
            it.audioQuality != null  // Has audio too
        }
        .sortedByDescending { it.height }
        .firstOrNull()
}
```

### Download with Progress
```kotlin
suspend fun downloadVideo(
    url: String,
    outputFile: File,
    onProgress: (downloaded: Long, total: Long) -> Unit
) {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val request = Request.Builder()
        .url(url)
        .header("User-Agent", ANDROID_VR_USER_AGENT)
        .build()
    
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Download failed: ${response.code}")
        
        val body = response.body ?: throw IOException("Empty response body")
        val totalBytes = body.contentLength()
        
        var downloadedBytes = 0L
        val buffer = ByteArray(8192)
        
        outputFile.outputStream().use { output ->
            body.byteStream().use { input ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    onProgress(downloadedBytes, totalBytes)
                }
            }
        }
    }
}
```

### Chunked Download with Resume Support
```kotlin
suspend fun downloadWithResume(
    url: String,
    outputFile: File,
    onProgress: (downloaded: Long, total: Long) -> Unit
) {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val existingBytes = if (outputFile.exists()) outputFile.length() else 0L
    
    val requestBuilder = Request.Builder()
        .url(url)
        .header("User-Agent", ANDROID_VR_USER_AGENT)
    
    if (existingBytes > 0) {
        requestBuilder.header("Range", "bytes=$existingBytes-")
    }
    
    client.newCall(requestBuilder.build()).execute().use { response ->
        val isResumed = response.code == 206
        val totalBytes = if (isResumed) {
            existingBytes + (response.body?.contentLength() ?: 0L)
        } else {
            response.body?.contentLength() ?: 0L
        }
        
        var downloadedBytes = if (isResumed) existingBytes else 0L
        val buffer = ByteArray(8192)
        
        val fileMode = if (isResumed) true else false
        FileOutputStream(outputFile, isResumed).use { output ->
            response.body?.byteStream()?.use { input ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    onProgress(downloadedBytes, totalBytes)
                }
            }
        }
    }
}
```

---

## 5. Thumbnail URL Patterns

Standard YouTube thumbnail URLs:
```
https://i.ytimg.com/vi/{VIDEO_ID}/default.jpg        (120x90)
https://i.ytimg.com/vi/{VIDEO_ID}/mqdefault.jpg       (320x180)
https://i.ytimg.com/vi/{VIDEO_ID}/hqdefault.jpg       (480x360)
https://i.ytimg.com/vi/{VIDEO_ID}/sddefault.jpg       (640x480)
https://i.ytimg.com/vi/{VIDEO_ID}/maxresdefault.jpg   (1280x720, may not exist)
```

These URLs work without API calls. Use `hqdefault.jpg` as default.

---

## 6. Error Handling

### Playability Status Codes
| Status | Meaning | Action |
|--------|---------|--------|
| `OK` | Video is available | Proceed with download |
| `ERROR` | General error | Show error reason to user |
| `UNPLAYABLE` | Video not available (geo-block, etc.) | Show reason, suggest VPN |
| `LOGIN_REQUIRED` | Age-restricted or private | Inform user |
| `CONTENT_CHECK_REQUIRED` | Needs content check | Set contentCheckOk=true (already done) |
| `LIVE_STREAM_OFFLINE` | Ended live stream | Try again or show error |

### Common Error Patterns
```kotlin
sealed class YouTubeError : Exception() {
    data class VideoUnavailable(val reason: String) : YouTubeError()
    data class NetworkError(val cause: Throwable) : YouTubeError()
    data class NoFormatsAvailable(val videoId: String) : YouTubeError()
    data class DownloadFailed(val httpCode: Int) : YouTubeError()
    data class InvalidUrl(val url: String) : YouTubeError()
    data class StorageFull(val requiredBytes: Long) : YouTubeError()
}
```

---

## 7. Rate Limiting & Best Practices

- **Don't hammer the API**: Add delays between requests (1-2 seconds)
- **Cache video info**: Store extracted metadata in Room DB to avoid re-fetching
- **URL expiration**: Download URLs expire after ~6 hours. Extract fresh URLs before each download.
- **User-Agent consistency**: Always use the same User-Agent in headers and request body
- **Error retry**: Use exponential backoff (1s, 2s, 4s, max 30s) for network errors
- **Respect `contentLength`**: Check available storage before starting download

---

## 8. Constants

```kotlin
object InnerTubeConfig {
    const val API_URL = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
    const val CLIENT_NAME = "ANDROID_VR"
    const val CLIENT_VERSION = "1.65.10"
    const val CLIENT_NAME_ID = "28"
    const val DEVICE_MAKE = "Oculus"
    const val DEVICE_MODEL = "Quest 3"
    const val ANDROID_SDK_VERSION = 32
    const val OS_VERSION = "12L"
    const val USER_AGENT = "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
}
```
