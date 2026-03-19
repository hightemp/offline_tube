# InnerTube API Research Report for Kotlin-only YouTube Downloader

> Generated from yt-dlp source code analysis (yt_dlp/extractor/youtube/)

---

## 1. API Endpoints

All InnerTube API requests go through:

```
POST https://{host}/youtubei/v1/{endpoint}?key={api_key}&prettyPrint=false
```

### Endpoints Used

| Endpoint | Purpose |
|----------|---------|
| `youtubei/v1/player` | Get video streaming data (formats, URLs, metadata) |
| `youtubei/v1/browse` | Browse channels, playlists |
| `youtubei/v1/next` | Get initial data (comments, engagement panels, chapters) |
| `youtubei/v1/search` | Search |

### Hosts

| Client | Host |
|--------|------|
| web, android, ios, tv, mweb | `www.youtube.com` |
| web_music | `music.youtube.com` |

**Note**: The API key parameter (`key=`) is optional for most requests now. yt-dlp passes it only if explicitly configured.

---

## 2. Client Identity Configs

### Android Client
```json
{
  "context": {
    "client": {
      "clientName": "ANDROID",
      "clientVersion": "21.02.35",
      "androidSdkVersion": 30,
      "userAgent": "com.google.android.youtube/21.02.35 (Linux; U; Android 11) gzip",
      "osName": "Android",
      "osVersion": "11",
      "hl": "en",
      "timeZone": "UTC",
      "utcOffsetMinutes": 0
    }
  }
}
```
- **INNERTUBE_CONTEXT_CLIENT_NAME**: `3`
- **REQUIRE_JS_PLAYER**: `false` (no signature cipher needed!)
- **PO Token**: Required for HTTPS and DASH streaming; player PO token recommended

### Android VR Client (used as default jsless client)
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
  }
}
```
- **INNERTUBE_CONTEXT_CLIENT_NAME**: `28`
- **REQUIRE_JS_PLAYER**: `false`
- **PO Token**: Not required (no GVS or Player PO token policies set)
- **Caveat**: "Made for kids" videos aren't available with this client

### Web Client
```json
{
  "context": {
    "client": {
      "clientName": "WEB",
      "clientVersion": "2.20260114.08.00",
      "hl": "en",
      "timeZone": "UTC",
      "utcOffsetMinutes": 0
    }
  }
}
```
- **INNERTUBE_CONTEXT_CLIENT_NAME**: `1`
- **REQUIRE_JS_PLAYER**: `true` (needs signature deciphering & n-param challenge!)
- **PO Token**: Required for HTTPS and DASH
- **SUPPORTS_COOKIES**: `true`

### Web Safari Client
```json
{
  "context": {
    "client": {
      "clientName": "WEB",
      "clientVersion": "2.20260114.08.00",
      "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15,gzip(gfe)",
      "hl": "en",
      "timeZone": "UTC",
      "utcOffsetMinutes": 0
    }
  }
}
```
- Same as web but with Safari UA. Returns pre-merged HLS formats at 144p/240p/360p/720p/1080p.

### iOS Client
```json
{
  "context": {
    "client": {
      "clientName": "IOS",
      "clientVersion": "21.02.3",
      "deviceMake": "Apple",
      "deviceModel": "iPhone16,2",
      "userAgent": "com.google.ios.youtube/21.02.3 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
      "osName": "iPhone",
      "osVersion": "18.3.2.22D82",
      "hl": "en",
      "timeZone": "UTC",
      "utcOffsetMinutes": 0
    }
  }
}
```
- **INNERTUBE_CONTEXT_CLIENT_NAME**: `5`
- **REQUIRE_JS_PLAYER**: `false`

### TV Client
```json
{
  "context": {
    "client": {
      "clientName": "TVHTML5",
      "clientVersion": "7.20260114.12.00",
      "userAgent": "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/25.lts.30.1034943-gold (unlike Gecko), Unknown_TV_Unknown_0/Unknown (Unknown, Unknown)",
      "hl": "en",
      "timeZone": "UTC",
      "utcOffsetMinutes": 0
    }
  }
}
```
- **INNERTUBE_CONTEXT_CLIENT_NAME**: `7`
- **TV Downgraded** (clientVersion `5.20260114`): Requires auth but works well for premium users

---

## 3. Request Structure

### Player API Request Body

```json
{
  "context": {
    "client": {
      "clientName": "ANDROID",
      "clientVersion": "21.02.35",
      "androidSdkVersion": 30,
      "userAgent": "com.google.android.youtube/21.02.35 (Linux; U; Android 11) gzip",
      "osName": "Android",
      "osVersion": "11",
      "hl": "en",
      "timeZone": "UTC",
      "utcOffsetMinutes": 0
    }
  },
  "videoId": "dQw4w9WgXcQ",
  "playbackContext": {
    "contentPlaybackContext": {
      "html5Preference": "HTML5_PREF_WANTS",
      "signatureTimestamp": 20321
    }
  },
  "contentCheckOk": true,
  "racyCheckOk": true
}
```

Optional fields:
- `"params"`: Player params string (base64 encoded, client-specific)
- `"serviceIntegrityDimensions": { "poToken": "..." }`: PO (Proof of Origin) token

### Required Headers

```
Content-Type: application/json
User-Agent: <matching the client's userAgent field>
X-YouTube-Client-Name: <INNERTUBE_CONTEXT_CLIENT_NAME number, e.g. "3" for android>
X-YouTube-Client-Version: <client version string>
Origin: https://www.youtube.com
```

For authenticated requests, additional headers:
```
Authorization: SAPISIDHASH <timestamp>_<sha1_hash>
X-Origin: https://www.youtube.com
X-Goog-Visitor-Id: <visitor_data>
X-Goog-PageId: <delegated_session_id>
X-Goog-AuthUser: <session_index>
```

---

## 4. Player Response Structure

The player API returns JSON with this structure:

```json
{
  "playabilityStatus": {
    "status": "OK",
    "playableInEmbed": true
  },
  "videoDetails": {
    "videoId": "dQw4w9WgXcQ",
    "title": "Video Title",
    "lengthSeconds": "212",
    "channelId": "UCuAXFkgsw1L7xaCfnd5JJOw",
    "shortDescription": "...",
    "thumbnail": {
      "thumbnails": [
        { "url": "https://i.ytimg.com/vi/dQw4w9WgXcQ/default.jpg", "width": 120, "height": 90 },
        { "url": "https://i.ytimg.com/vi/dQw4w9WgXcQ/mqdefault.jpg", "width": 320, "height": 180 },
        { "url": "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg", "width": 480, "height": 360 }
      ]
    },
    "viewCount": "1234567890",
    "isLive": false,
    "isLiveContent": false,
    "keywords": ["tag1", "tag2"],
    "averageRating": 4.5
  },
  "streamingData": {
    "formats": [ /* combined audio+video */ ],
    "adaptiveFormats": [ /* separate audio or video streams */ ],
    "hlsManifestUrl": "https://manifest.googlevideo.com/api/manifest/hls_variant/...",
    "dashManifestUrl": "https://manifest.googlevideo.com/api/manifest/dash/..."
  },
  "microformat": {
    "playerMicroformatRenderer": {
      "title": { "simpleText": "Video Title" },
      "description": { "simpleText": "..." },
      "uploadDate": "2009-10-25T06:57:33-07:00",
      "category": "Music",
      "isFamilySafe": true,
      "availableCountries": ["US", "GB", ...]
    }
  },
  "captions": {
    "playerCaptionsTracklistRenderer": {
      "captionTracks": [
        { "baseUrl": "...", "name": { "simpleText": "English" }, "languageCode": "en" }
      ]
    }
  },
  "storyboards": {
    "playerStoryboardSpecRenderer": {
      "spec": "https://i.ytimg.com/sb/VIDEO_ID/storyboard3_L$L/$N.jpg?..."
    }
  }
}
```

---

## 5. Format/Stream URL Extraction

### streamingData.formats (combined audio+video)

These are pre-muxed (audio+video combined) streams, typically at lower resolutions (360p, 720p).

### streamingData.adaptiveFormats (separate streams)

These are either video-only or audio-only streams at higher quality. Need to be muxed together post-download.

### Format Object Structure

Each format entry looks like:

```json
{
  "itag": 22,
  "url": "https://rr4---sn-...-googlevideo.com/videoplayback?expire=...&ei=...&ip=...&id=...&itag=22&...",
  "mimeType": "video/mp4; codecs=\"avc1.64001F, mp4a.40.2\"",
  "bitrate": 1500000,
  "averageBitrate": 1200000,
  "width": 1280,
  "height": 720,
  "contentLength": "31415926",
  "quality": "hd720",
  "qualityLabel": "720p",
  "fps": 30,
  "audioQuality": "AUDIO_QUALITY_MEDIUM",
  "audioSampleRate": "44100",
  "audioChannels": 2,
  "approxDurationMs": "212000",
  "projectionType": "RECTANGULAR"
}
```

### Two URL delivery mechanisms:

#### 1. Direct URL (preferred, used by android/ios clients)
The format has a `"url"` field with the direct download URL.

#### 2. Signature Cipher (used by web clients)
Instead of `"url"`, the format has:
```json
{
  "signatureCipher": "s=ENCRYPTED_SIG&sp=sig&url=https%3A%2F%2F..."
}
```

The `signatureCipher` is URL-encoded with these params:
- `s`: The encrypted signature that must be deciphered
- `sp`: The query parameter name to append the deciphered signature to (usually `sig` or `signature`)
- `url`: The base video URL (URL-encoded)

To get the final URL: `url + "&" + sp + "=" + decipher(s)`

---

## 6. Signature Cipher Challenge

### How it works

1. YouTube's player JavaScript contains a cipher function that transforms signatures
2. The cipher function is a sequence of string manipulations (reverse, splice, swap characters)
3. yt-dlp extracts this function from the player JS and evaluates it

### Player JS URL discovery

From the webpage's `ytcfg` config:
```javascript
ytcfg.set({ "PLAYER_JS_URL": "/s/player/PLAYER_ID/player_ias.vflset/en_US/base.js" })
```

Or via iframe API:
```
GET https://www.youtube.com/iframe_api
```
Extract player version (8-char hex ID) from: `player\\/{PLAYER_ID}\\/`

Full player URL: `https://www.youtube.com/s/player/{PLAYER_ID}/player_ias.vflset/en_US/base.js`

### Signature deciphering logic

yt-dlp solves this by:
1. Downloading the player JS
2. Sending it to a JavaScript runtime (deno, node, bun, quickjs) along with a "challenge solver" script
3. The solver script parses the player JS, finds the cipher function, and evaluates it
4. Result is a **spec** (array of integer indices) that maps input chars to output chars

The actual deciphering is remarkably simple once you have the spec:
```python
def solve_sig(s, spec):
    return ''.join(s[i] for i in spec)
```

### Complexity for Kotlin implementation

**HIGH COMPLEXITY**. The cipher function changes every time YouTube updates their player JS (every few weeks). You'd need to either:
- Embed a JavaScript interpreter (e.g., QuickJS via JNI, or Mozilla Rhino)
- Implement the cipher function parser/interpreter in Kotlin
- Use a server-side solution

The cipher operations themselves are simple (array manipulations), but **parsing the obfuscated JS to find them is the hard part**.

---

## 7. N-Parameter Throttling Challenge

### What it is

YouTube adds an `n` parameter to streaming URLs. If you don't transform this parameter correctly, download speeds are **severely throttled** (~50KB/s instead of full speed).

### How yt-dlp handles it

1. Extract the n-challenge value from the URL query parameter `n`
2. Send the player JS + n-challenge to a JavaScript runtime
3. The JS runtime runs the n-transform function from the player JS
4. Replace the `n` parameter in the URL with the transformed value

From the URL: `&n=ORIGINAL_VALUE` → `&n=TRANSFORMED_VALUE`

For manifest URLs, the pattern is `/n/ORIGINAL_VALUE/` in the path.

### Complexity for Kotlin implementation

**VERY HIGH COMPLEXITY**. The n-transform function is a complex, obfuscated JavaScript function that:
- Changes with every player update
- Uses complex control flow (arrays of function references)
- Cannot be replicated with simple string manipulation
- **Requires a JavaScript interpreter** to evaluate

This is the single hardest challenge for a Kotlin-only implementation.

---

## 8. PO Token (Proof of Origin)

### What it is

YouTube now requires Proof of Origin tokens for many clients/streaming protocols to prevent automated access.

### Policies per client

| Client | HTTPS Streaming | DASH Streaming | HLS Streaming | Player Token |
|--------|----------------|----------------|---------------|--------------|
| web | Required | Required | Not required | Not required |
| android | Required (waived w/ player token) | Required (waived w/ player token) | Not required | Recommended |
| ios | Required (waived w/ player token) | N/A | Required (waived w/ player token) | Recommended |
| android_vr | Not required | Not required | Not required | Not required |
| tv | Not required | Not required | Not required | Not required |

### How PO tokens are obtained

PO tokens are generated via a separate plugin/provider system. The token is appended to URLs as `&pot=TOKEN_VALUE` or included in the player request as:
```json
{
  "serviceIntegrityDimensions": {
    "poToken": "TOKEN_VALUE"
  }
}
```

### Complexity

**VERY HIGH**. PO token generation involves Google's BotGuard/attestation system, which is extremely hard to replicate without running actual browser JavaScript.

---

## 9. Thumbnail URLs

### From API Response

Thumbnails come from `videoDetails.thumbnail.thumbnails[]` array with `url`, `width`, `height`.

### Constructed URLs (known patterns)

```
https://i.ytimg.com/vi/{video_id}/{name}.jpg
https://i.ytimg.com/vi_webp/{video_id}/{name}.webp
```

For live streams:
```
https://i.ytimg.com/vi/{video_id}/{name}_live.jpg
https://i.ytimg.com/vi_webp/{video_id}/{name}_live.webp
```

### Thumbnail name variants (highest to lowest quality):

| Name | Typical Size |
|------|-------------|
| `maxresdefault` | 1920×1080 |
| `hq720` | 1280×720 |
| `sddefault` | 640×480 |
| `hqdefault` | 480×360 |
| `0` | 480×360 |
| `mqdefault` | 320×180 |
| `default` | 120×90 |

---

## 10. Rate Limiting Considerations

From yt-dlp's code:
- YouTube returns HTTP 403/429 for rate-limited requests
- Error message: "This content isn't available, try again later"
- Rate limiting can last up to **1 hour**
- Recommended: Add delays between video requests (`-t sleep`)
- YouTube may also show CAPTCHA challenges via `playerCaptchaViewModel`
- Session rotation (cookies being invalidated) can happen

---

## 11. Recommended Kotlin Implementation Approach

### Tier 1: Feasible (Medium Effort)

1. **Android VR Client** - Best starting point:
   - No JS player required (`REQUIRE_JS_PLAYER: false`)
   - No PO token required
   - Returns direct URLs (no signature cipher)
   - URLs don't need n-parameter transformation
   - **Limitation**: "Made for kids" videos don't work
   - **Limitation**: May have limited format selection

2. **API Request Construction** - Straightforward:
   - Build JSON body with context + videoId
   - POST to `https://www.youtube.com/youtubei/v1/player`
   - Set correct User-Agent and headers
   - Parse JSON response

3. **Format Selection** - Extract from `streamingData`:
   - `formats[]` for pre-muxed streams
   - `adaptiveFormats[]` for separate audio/video
   - Direct `url` field available for android/ios/android_vr clients
   - Parse `mimeType` for codec info

4. **Metadata Extraction** - Simple JSON traversal:
   - Title, description, duration from `videoDetails`
   - Thumbnails from `videoDetails.thumbnail.thumbnails[]`
   - Upload date from `microformat.playerMicroformatRenderer`

5. **Download** - HTTP range requests:
   - Use `contentLength` to know total size
   - Download with `Range: bytes=start-end` headers
   - Chunk size ~10MB recommended

### Tier 2: Hard (Significant Effort)

1. **Web Client Support** - Requires:
   - Embedding a JS runtime (QuickJS via JNI, or Rhino)
   - Downloading and parsing player JS
   - Solving signature cipher (character reordering)
   - Solving n-parameter throttle challenge
   - Must update when YouTube changes player JS

2. **PO Token Generation** - Requires:
   - Google BotGuard/attestation integration
   - Either use WebView to run attestation JS, or use external token provider
   - Without PO tokens, android/web clients will fail or be throttled

3. **Audio/Video Muxing** - For adaptive formats:
   - Separate audio + video downloads
   - Need FFmpeg or MediaMuxer to combine
   - Android's MediaMuxer API can handle MP4 muxing

### Tier 3: Very Hard (Not Recommended Without Server)

1. **Full signature cipher in pure Kotlin** - The cipher function changes regularly
2. **N-parameter transform in pure Kotlin** - Requires JS execution
3. **BotGuard PO token generation** - Requires Google's proprietary JS
4. **SABR streaming** - YouTube's new streaming protocol (being rolled out)

### Recommended Architecture

```
┌─────────────────────────────┐
│     Android App (Kotlin)     │
├─────────────────────────────┤
│  InnerTubeClient            │ ← Build API requests
│  ├─ AndroidVRClient         │ ← Primary (no JS needed)
│  ├─ AndroidClient           │ ← Needs PO token  
│  └─ WebClient               │ ← Needs JS runtime + PO token
├─────────────────────────────┤
│  VideoInfoParser            │ ← Parse player response JSON
│  FormatSelector             │ ← Pick best format
│  StreamDownloader           │ ← HTTP range download with resume
│  MediaMuxer                 │ ← Merge audio+video (adaptive)
├─────────────────────────────┤
│  Optional: QuickJS JNI      │ ← For sig/n-param challenges
│  Optional: WebView PO Token │ ← For BotGuard attestation
└─────────────────────────────┘
```

### Minimum Viable Implementation

Use **android_vr** client:
1. POST to `/youtubei/v1/player` with android_vr context
2. Parse `streamingData.formats` for pre-muxed URL
3. Parse `streamingData.adaptiveFormats` for best quality
4. Download via direct URL with HTTP range requests
5. Use Android MediaMuxer to combine audio + video

This avoids ALL JavaScript challenges and PO token requirements.

### Key Risks

1. **YouTube can break any client at any time** - They may start requiring PO tokens for android_vr
2. **Format availability varies by client** - android_vr may not return all formats
3. **IP-based throttling** - Too many requests will get rate-limited
4. **Made for kids restriction** - android_vr doesn't work for these videos
5. **Age-restricted content** - May require authentication
6. **SABR-only streaming** - YouTube is experimenting with removing direct URLs for web clients

---

## Appendix: Complete Client Name → Number Mapping

| Client Name | clientName String | Number |
|-------------|-------------------|--------|
| web | WEB | 1 |
| mweb | MWEB | 2 |
| android | ANDROID | 3 |
| ios | IOS | 5 |
| tv | TVHTML5 | 7 |
| android_vr | ANDROID_VR | 28 |
| web_embedded | WEB_EMBEDDED_PLAYER | 56 |
| web_creator | WEB_CREATOR | 62 |
| web_music | WEB_REMIX | 67 |
| tv_simply | TVHTML5_SIMPLY | 75 |
