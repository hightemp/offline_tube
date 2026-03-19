---
name: yt-dlp-android
description: Guide for building an Android app that downloads YouTube videos using yt-dlp as a Python library. Covers yt-dlp architecture, API embedding patterns, format selection, metadata extraction, error handling, and Android integration strategies via Chaquopy or process-based approaches.
metadata:
  author: offline_tube
  version: "1.0"
  category: android-development
---

# yt-dlp Android Integration Skill

## 1. yt-dlp Architecture Overview

yt-dlp is a feature-rich video downloader written in Python. Its architecture follows a pipeline pattern:

```
URL → InfoExtractor → info_dict (metadata + formats) → FormatSelector → Downloader → File
                                                      → PostProcessor → Final File
```

### Core Components

| Component | Location | Role |
|-----------|----------|------|
| `YoutubeDL` | `yt_dlp/YoutubeDL.py` | Main orchestrator class. Manages extractors, downloaders, and post-processors |
| `InfoExtractor` | `yt_dlp/extractor/` | Extracts video metadata and format URLs from websites |
| `YoutubeIE` | `yt_dlp/extractor/youtube/_video.py` | YouTube-specific extractor (11-char video ID matching) |
| `FileDownloader` | `yt_dlp/downloader/` | Downloads video/audio data. Protocol-specific implementations |
| `HttpFD` | `yt_dlp/downloader/http.py` | HTTP/HTTPS downloader with chunked download and resume support |
| `PostProcessor` | `yt_dlp/postprocessor/` | Post-download processing (merge, convert, embed metadata) |
| `FormatSorter` | `yt_dlp/utils/_utils.py` | Sorts and selects formats based on quality criteria |

### Request Flow

1. `YoutubeDL.extract_info(url)` finds the matching `InfoExtractor` via `IE.suitable(url)`
2. The extractor's `_real_extract()` method fetches the webpage/API and builds an `info_dict`
3. `YoutubeDL.process_video_result()` sanitizes metadata, sorts formats, applies format selection
4. If `download=True`, the selected format is passed to the appropriate `FileDownloader`
5. Post-processors run after download (merge audio+video, embed thumbnails, etc.)

### YouTube Extractor Specifics

The YouTube extractor (`YoutubeIE`) uses YouTube's InnerTube API with multiple client identities:
- **`web`** / **`web_safari`**: Standard web clients
- **`android`**: Android app client (SDK 30, version 21.02.35)
- **`ios`**: iOS app client
- **`tv`** / **`tv_embedded`**: TV/embedded clients
- **`web_music`**: YouTube Music client

Each client returns different format sets. The extractor tries multiple clients and merges results.

**PO Token System**: YouTube now requires Proof of Origin tokens for some clients/formats. The extractor handles this via `_PoTokenContext` and `GvsPoTokenPolicy`.

**JS Challenges**: YouTube uses signature ciphering (`s` parameter) and n-parameter throttling challenges that require JavaScript interpretation.

---

## 2. Key API for Embedding

### Basic Usage Pattern

```python
import yt_dlp

# Extract info without downloading
ydl_opts = {}
with yt_dlp.YoutubeDL(ydl_opts) as ydl:
    info = ydl.extract_info(url, download=False)
    # info is a dict-like object with all metadata
    serializable = ydl.sanitize_info(info)
```

### Core API Methods

#### `YoutubeDL.__init__(params)` 
Creates the downloader instance. Key params for Android:

```python
params = {
    # Format selection
    'format': 'bestvideo[height<=720]+bestaudio/best[height<=720]',
    'format_sort': ['res:720', 'ext:mp4:m4a'],
    
    # Output
    'outtmpl': '/storage/downloads/%(title)s.%(ext)s',
    'paths': {'home': '/storage/downloads/', 'temp': '/cache/'},
    'restrictfilenames': True,       # Safe filenames for Android
    'windowsfilenames': True,        # Extra safety for special chars
    
    # Download control
    'noplaylist': True,              # Single video only
    'extract_flat': False,           # Resolve all info
    'ignoreerrors': False,           # Fail on errors (handle in app)
    'quiet': True,                   # Suppress stdout
    'no_warnings': False,
    
    # Network
    'socket_timeout': 30,
    'retries': 3,
    'fragment_retries': 3,
    'http_chunk_size': 10485760,     # 10MB chunks
    'continuedl': True,              # Resume downloads
    
    # Progress tracking
    'progress_hooks': [my_progress_hook],
    'logger': MyLogger(),
    
    # Subtitles
    'writesubtitles': False,
    'writeautomaticsub': False,
    'subtitleslangs': ['en'],
    
    # Thumbnail
    'writethumbnail': True,
    
    # Skip post-processing if no ffmpeg
    'postprocessors': [],
    'prefer_free_formats': True,
}
```

#### `YoutubeDL.extract_info(url, download=True, ie_key=None, process=True)`
Main extraction method. Returns `info_dict`.

- `download=False`: Extract metadata only (no file download)
- `download=True`: Extract and download
- `ie_key='Youtube'`: Force specific extractor
- `process=True`: Must be True for download to work

#### `YoutubeDL.download(url_list)`
Download a list of URLs. Calls `extract_info` internally.

```python
with yt_dlp.YoutubeDL(opts) as ydl:
    retcode = ydl.download(['https://youtube.com/watch?v=VIDEO_ID'])
    # retcode: 0 = success, 1 = error
```

#### `YoutubeDL.sanitize_info(info_dict)`
Makes `info_dict` JSON-serializable. **Always use this before serializing.**

```python
import json
info = ydl.extract_info(url, download=False)
safe_info = ydl.sanitize_info(info)
json_str = json.dumps(safe_info)
```

#### `YoutubeDL.prepare_filename(info_dict)`
Returns the final output filename based on `outtmpl`.

### Progress Hook Pattern

```python
def progress_hook(d):
    """Called during download with status updates."""
    status = d['status']  # 'downloading', 'finished', 'error'
    
    if status == 'downloading':
        downloaded = d.get('downloaded_bytes', 0)
        total = d.get('total_bytes') or d.get('total_bytes_estimate')
        speed = d.get('speed')          # bytes/sec or None
        eta = d.get('eta')              # seconds or None
        elapsed = d.get('elapsed')      # seconds since start
        filename = d.get('filename')
        tmpfilename = d.get('tmpfilename')
        fragment_index = d.get('fragment_index')
        fragment_count = d.get('fragment_count')
        
    elif status == 'finished':
        filename = d['filename']
        downloaded = d.get('downloaded_bytes', 0)
        total = d.get('total_bytes')
        elapsed = d.get('elapsed')
        
    elif status == 'error':
        pass  # Handle error
```

### Logger Pattern

```python
class AppLogger:
    """Custom logger for yt-dlp integration."""
    
    def debug(self, msg):
        # Both debug and info go through debug()
        if msg.startswith('[debug] '):
            # Actual debug messages
            log_debug(msg)
        else:
            # Info-level messages
            log_info(msg)
    
    def info(self, msg):
        log_info(msg)
    
    def warning(self, msg):
        log_warning(msg)
    
    def error(self, msg):
        log_error(msg)
```

---

## 3. Format Selection Mechanism

### Format Specification Syntax

The `format` parameter accepts a selector expression:

| Selector | Meaning |
|----------|---------|
| `best` | Best quality with both video+audio in one file |
| `bestvideo+bestaudio` | Best separate video + audio (requires merge/ffmpeg) |
| `bestvideo*+bestaudio/best` | Best video (may include audio) + best audio, fallback to best combined |
| `bestvideo[height<=720]+bestaudio` | 720p max video + best audio |
| `best[height<=480]` | Best combined format, max 480p |
| `worst` | Worst quality (for bandwidth-limited scenarios) |
| `ba` / `bestaudio` | Best audio-only |
| `bv` / `bestvideo` | Best video-only |

### Filter Operators

```
[height<=720]           # Max 720p
[ext=mp4]               # MP4 container only
[filesize<50M]          # Under 50MB
[vcodec^=avc]           # H.264 codec (starts with "avc")
[acodec=opus]           # Opus audio codec
[fps<=30]               # Max 30fps
[protocol=https]        # HTTPS only (no DASH/HLS)
```

### Format Sort (`format_sort` parameter)

Controls how "best" is determined. Default order:
```
lang, quality, res, fps, hdr:12, vcodec, channels, acodec, size, br, asr, proto, ext, hasaud, source, id
```

Key sort fields:
- `res` — Resolution (higher preferred)
- `ext` — Container (`mp4` > `mov` > `webm` > `flv`)
- `vcodec` — Video codec (`av01` > `vp9.2` > `vp9` > `h265` > `h264`)
- `acodec` — Audio codec (`flac/alac` > `wav` > `opus` > `vorbis` > `aac` > `mp3`)
- `size` — File size
- `proto` — Protocol (`https` > `http` > `m3u8` > `dash`)

**Android-optimized format selection:**
```python
# For devices without ffmpeg (no merge capability):
'format': 'best[height<=720]/best'  # Pre-merged only

# For devices with ffmpeg:
'format': 'bestvideo[height<=1080][ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best'

# Audio only:
'format': 'm4a/bestaudio/best'

# Small file (mobile data):
'format': 'best[height<=480][filesize<100M]/worst'
```

### Format Dict Fields (per format entry)

Each format in `info_dict['formats']` contains:

```python
{
    'format_id': '137',               # Unique format identifier
    'ext': 'mp4',                     # File extension
    'width': 1920,                    # Video width in pixels
    'height': 1080,                   # Video height in pixels
    'fps': 30,                        # Frames per second
    'vcodec': 'avc1.640028',          # Video codec
    'acodec': 'mp4a.40.2',           # Audio codec ('none' if video-only)
    'abr': 128,                       # Audio bitrate kbps
    'vbr': 4000,                      # Video bitrate kbps
    'tbr': 4128,                      # Total bitrate kbps
    'filesize': 50000000,             # Exact file size in bytes (if known)
    'filesize_approx': 48000000,      # Approximate file size
    'url': 'https://...',             # Direct download URL
    'protocol': 'https',             # Download protocol
    'format_note': '1080p',           # Human-readable quality note
    'dynamic_range': 'SDR',           # SDR/HDR10/HDR10+/HLG/DV
    'resolution': '1920x1080',        # Resolution string
    'aspect_ratio': 1.78,             # Width/height ratio
    'audio_channels': 2,              # Number of audio channels
    'language': 'en',                 # Audio language
    'has_drm': False,                 # DRM protection flag
    'container': 'mp4_dash',          # Container format
    'asr': 44100,                     # Audio sample rate
}
```

---

## 4. Available Metadata Fields (info_dict)

The `info_dict` returned by `extract_info()` for YouTube contains:

### Core Fields
```python
{
    'id': 'dQw4w9WgXcQ',                     # Video ID (11 chars)
    'title': 'Video Title',                    # Video title
    'description': 'Full description...',      # Video description
    'duration': 212,                           # Duration in seconds
    'thumbnail': 'https://i.ytimg.com/...',    # Best thumbnail URL
    'thumbnails': [{                           # All available thumbnails
        'url': 'https://i.ytimg.com/vi/ID/maxresdefault.jpg',
        'width': 1280, 'height': 720,
        'preference': -1,
    }],
    'webpage_url': 'https://www.youtube.com/watch?v=...',
    'ext': 'mp4',                              # Final extension
    'formats': [...],                          # List of all available formats
}
```

### Channel/Uploader Fields
```python
{
    'channel': 'Channel Name',
    'channel_id': 'UCxxxxxxxxxxxxxxxxxx',      # Channel ID
    'channel_url': 'https://www.youtube.com/channel/UCxxx',
    'channel_follower_count': 1000000,
    'channel_is_verified': True,
    'uploader': 'Channel Name',               # Same as channel
    'uploader_id': '@handle',                  # Channel handle
    'uploader_url': 'https://www.youtube.com/@handle',
}
```

### Engagement Fields
```python
{
    'view_count': 1000000,
    'like_count': 50000,
    'comment_count': 5000,
    'average_rating': None,                    # Deprecated by YouTube
}
```

### Date/Time Fields
```python
{
    'upload_date': '20230115',                 # YYYYMMDD format
    'timestamp': 1673784000,                   # Unix timestamp (if timezone available)
    'release_timestamp': None,                 # For premieres/livestreams
}
```

### Classification Fields
```python
{
    'categories': ['Music'],
    'tags': ['tag1', 'tag2'],
    'age_limit': 0,                            # 0 or 18
    'availability': 'public',                  # public/unlisted/private/premium/subscription
    'live_status': 'not_live',                 # not_live/is_live/was_live/is_upcoming/post_live
    'media_type': 'video',                     # video/short/livestream
    'playable_in_embed': True,
}
```

### Music-Specific Fields (auto-generated for music videos)
```python
{
    'track': 'Song Name',
    'artists': ['Artist Name'],
    'album': 'Album Name',
    'release_year': 2023,
    'release_date': '20230115',
}
```

### Subtitle Fields
```python
{
    'subtitles': {                              # Manual subtitles
        'en': [{'ext': 'vtt', 'url': '...'}],
    },
    'automatic_captions': {                    # Auto-generated captions
        'en': [{'ext': 'vtt', 'url': '...', 'name': 'English'}],
        'en-orig': [{'ext': 'vtt', 'url': '...'}],
    },
    'requested_subtitles': {...},              # Selected subtitles based on params
}
```

### Other Fields
```python
{
    'chapters': [{'start_time': 0, 'end_time': 60, 'title': 'Intro'}],
    'heatmap': [...],                          # Engagement heatmap data
    'location': None,                          # Geolocation if tagged
    'series': None,                            # TV series name
    'season_number': None,
    'episode_number': None,
    'license': None,                           # Content license
    'playlist': None,                          # Playlist title if part of one
    'playlist_index': None,
}
```

### Thumbnail URL Patterns
YouTube provides thumbnails at known URLs:
```
https://i.ytimg.com/vi/{video_id}/maxresdefault.jpg     # 1280x720 (may not exist)
https://i.ytimg.com/vi/{video_id}/hq720.jpg             # 1280x720
https://i.ytimg.com/vi/{video_id}/sddefault.jpg         # 640x480
https://i.ytimg.com/vi/{video_id}/hqdefault.jpg         # 480x360
https://i.ytimg.com/vi/{video_id}/mqdefault.jpg         # 320x180
https://i.ytimg.com/vi/{video_id}/default.jpg           # 120x90
https://i.ytimg.com/vi_webp/{video_id}/maxresdefault.webp  # WebP variants
```

---

## 5. Error Handling Patterns

### Exception Hierarchy

```
YoutubeDLError
├── DownloadError           # General download failure
├── ExtractorError          # Extraction failed
│   └── GeoRestrictedError  # Geographic restriction
├── PostProcessingError     # FFmpeg/post-processing failure
├── SameFileError           # Multiple downloads to same file
├── ContentTooShortError    # Incomplete download
├── UnavailableVideoError   # Video not available
├── MaxDownloadsReached     # Download limit hit
├── DownloadCancelled       # User cancelled
│   ├── ExistingVideoReached
│   └── RejectedVideoReached
└── UserNotLive             # Streamer not live
```

### Error Handling in Android App

```python
import yt_dlp
from yt_dlp.utils import (
    DownloadError,
    ExtractorError,
    GeoRestrictedError,
    UnavailableVideoError,
)

def extract_video_info(url):
    """Extract video info with proper error handling."""
    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
        'ignoreerrors': False,
    }
    
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            if info is None:
                return {'error': 'No info extracted'}
            return ydl.sanitize_info(info)
            
    except GeoRestrictedError as e:
        return {'error': 'geo_restricted', 'message': str(e), 'countries': e.countries}
    except UnavailableVideoError:
        return {'error': 'unavailable', 'message': 'Video is unavailable'}
    except ExtractorError as e:
        msg = str(e)
        if 'sign in' in msg.lower():
            return {'error': 'auth_required', 'message': msg}
        elif 'private' in msg.lower():
            return {'error': 'private_video', 'message': msg}
        elif 'removed' in msg.lower() or 'deleted' in msg.lower():
            return {'error': 'removed', 'message': msg}
        return {'error': 'extraction_failed', 'message': msg}
    except DownloadError as e:
        return {'error': 'download_failed', 'message': str(e)}
    except Exception as e:
        return {'error': 'unknown', 'message': str(e)}
```

### Common YouTube-Specific Errors

| Error | Cause | Handling |
|-------|-------|----------|
| Sign-in required | Age-restricted or private | Provide cookies or authentication |
| Rate limited | Too many requests | Add sleep interval between requests |
| PO Token required | Bot detection | Configure PO token provider |
| Video unavailable | Deleted/private/region-blocked | Show user-friendly message |
| DRM protected | Premium/protected content | Skip with explanation |
| No formats found | Extraction failure | Retry with different client |

---

## 6. Dependencies

### Core (no external dependencies required)
yt-dlp has **zero required dependencies** — all are optional:

```toml
dependencies = []  # Empty! Pure Python stdlib works

[project.optional-dependencies]
default = [
    "brotli",              # Brotli compression support
    "certifi",             # SSL certificates
    "mutagen",             # Audio metadata for thumbnail embedding
    "pycryptodomex",       # AES decryption for some formats
    "requests>=2.32.2",    # HTTP library (fallback: urllib)
    "urllib3>=2.0.2",      # HTTP library
    "websockets>=13.0",    # WebSocket support
    "yt-dlp-ejs==0.8.0",  # External JS component resolution
]
```

### For Android Integration (Chaquopy)

**Definitely needed:**
- `pycryptodomex` — Required for decrypting some YouTube format URLs
- `certifi` — SSL certificate verification on Android
- `brotli` or `brotlicffi` — Brotli content-encoding (common in YouTube responses)

**Recommended:**
- `mutagen` — For embedding thumbnails into audio files
- `requests` + `urllib3` — More reliable HTTP than stdlib on Android
- `websockets` — Only if supporting live chat/live streams

**NOT needed on Android:**
- `curl-cffi` — C library, complex to build for Android
- `secretstorage` — Linux desktop keyring
- `pyinstaller` — Binary packaging
- `yt-dlp-ejs` — External JS components (Deno/Node not available on Android)

### FFmpeg Consideration

Many yt-dlp features require FFmpeg:
- Merging separate video+audio streams
- Converting formats (e.g., WebM → MP4)
- Extracting audio from video
- HLS/DASH native downloading

**Android options:**
1. Bundle `ffmpeg-kit` (mobile FFmpeg) — adds ~15-30MB but enables all features
2. Use pre-merged formats only (`format: 'best'`) — no FFmpeg needed but lower quality ceiling
3. Use `mobile-ffmpeg` via JNI — most Android YouTube downloaders do this

---

## 7. Android Integration Approaches

### Approach A: Chaquopy (Embedded Python) — RECOMMENDED

[Chaquopy](https://chaquo.com/chaquopy/) embeds CPython into Android apps via Gradle plugin.

**Pros:**
- Full yt-dlp Python API available
- Easy updates (pip install)
- Progress hooks, format selection, metadata extraction all work
- No subprocess overhead

**Cons:**
- Adds ~20-30MB to APK (Python runtime)
- Chaquopy is free for open-source, paid for commercial
- Some C extensions may not compile for all ABIs

**Setup in `build.gradle.kts`:**
```kotlin
plugins {
    id("com.chaquo.python") version "16.0.0"
}

android {
    defaultConfig {
        python {
            pip {
                install("yt-dlp")
                install("pycryptodomex")
                install("certifi")
                install("brotli")
                install("mutagen")
                install("requests")
                install("urllib3")
            }
        }
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }
}
```

**Kotlin/Java usage:**
```kotlin
// In your ViewModel or Worker
import com.chaquo.python.Python
import com.chaquo.python.PyObject

class VideoDownloadManager {
    private val py = Python.getInstance()
    private val ytDlpModule = py.getModule("yt_dlp")
    
    fun extractInfo(url: String): Map<String, Any?> {
        val opts = py.builtins.callAttr("dict").apply {
            callAttr("__setitem__", "quiet", true)
            callAttr("__setitem__", "no_warnings", true)
            callAttr("__setitem__", "noplaylist", true)
        }
        
        val ydl = ytDlpModule.callAttr("YoutubeDL", opts)
        val info = ydl.callAttr("extract_info", url, 
            py.builtins.callAttr("bool", false))  // download=False
        val sanitized = ydl.callAttr("sanitize_info", info)
        
        return sanitized.toJava(Map::class.java) as Map<String, Any?>
    }
    
    fun download(url: String, outputDir: String, 
                 onProgress: (Float, Long, Long?) -> Unit) {
        // Create Python progress hook callable
        val progressHook = py.getModule("__main__").callAttr(
            "eval", """
            def hook(d):
                status = d.get('status')
                if status == 'downloading':
                    downloaded = d.get('downloaded_bytes', 0)
                    total = d.get('total_bytes') or d.get('total_bytes_estimate', 0)
                    callback(downloaded, total)
            hook
            """.trimIndent()
        )
        
        val opts = createDownloadOpts(outputDir, progressHook)
        val ydl = ytDlpModule.callAttr("YoutubeDL", opts)
        ydl.callAttr("download", py.builtins.callAttr("list", listOf(url)))
    }
}
```

**Recommended wrapper module** — Create a Python bridge file:
```python
# app/src/main/python/ytdlp_bridge.py
"""Bridge module between Android/Kotlin and yt-dlp."""
import json
import yt_dlp
from yt_dlp.utils import DownloadError, ExtractorError

_progress_callback = None
_log_callback = None


def set_callbacks(progress_cb=None, log_cb=None):
    global _progress_callback, _log_callback
    _progress_callback = progress_cb
    _log_callback = log_cb


class AndroidLogger:
    def debug(self, msg):
        if _log_callback and not msg.startswith('[debug] '):
            _log_callback('INFO', msg)
    
    def info(self, msg):
        if _log_callback:
            _log_callback('INFO', msg)
    
    def warning(self, msg):
        if _log_callback:
            _log_callback('WARN', msg)
    
    def error(self, msg):
        if _log_callback:
            _log_callback('ERROR', msg)


def _progress_hook(d):
    if _progress_callback is None:
        return
    status = d.get('status', '')
    if status == 'downloading':
        downloaded = d.get('downloaded_bytes', 0)
        total = d.get('total_bytes') or d.get('total_bytes_estimate', 0)
        speed = d.get('speed', 0) or 0
        eta = d.get('eta', -1) or -1
        _progress_callback(status, downloaded, total, speed, eta, '')
    elif status == 'finished':
        _progress_callback(status, 0, 0, 0, 0, d.get('filename', ''))
    elif status == 'error':
        _progress_callback(status, 0, 0, 0, 0, '')


def extract_info(url, format_spec='best', cookies_file=None):
    """Extract video info without downloading.
    
    Returns JSON string with video metadata.
    """
    opts = {
        'format': format_spec,
        'quiet': True,
        'no_warnings': True,
        'noplaylist': True,
        'logger': AndroidLogger(),
    }
    if cookies_file:
        opts['cookiefile'] = cookies_file
    
    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=False)
            if info is None:
                return json.dumps({'error': 'no_info', 'message': 'Could not extract info'})
            
            safe = ydl.sanitize_info(info)
            # Extract only the fields we need for the Android UI
            result = {
                'id': safe.get('id'),
                'title': safe.get('title'),
                'description': safe.get('description', ''),
                'duration': safe.get('duration'),
                'thumbnail': safe.get('thumbnail'),
                'channel': safe.get('channel'),
                'channel_url': safe.get('channel_url'),
                'view_count': safe.get('view_count'),
                'like_count': safe.get('like_count'),
                'upload_date': safe.get('upload_date'),
                'categories': safe.get('categories'),
                'tags': safe.get('tags', []),
                'age_limit': safe.get('age_limit', 0),
                'live_status': safe.get('live_status'),
                'webpage_url': safe.get('webpage_url'),
                'formats': [
                    {
                        'format_id': f.get('format_id'),
                        'ext': f.get('ext'),
                        'width': f.get('width'),
                        'height': f.get('height'),
                        'fps': f.get('fps'),
                        'vcodec': f.get('vcodec'),
                        'acodec': f.get('acodec'),
                        'filesize': f.get('filesize') or f.get('filesize_approx'),
                        'tbr': f.get('tbr'),
                        'format_note': f.get('format_note'),
                        'resolution': f.get('resolution'),
                        'has_video': f.get('vcodec', 'none') != 'none',
                        'has_audio': f.get('acodec', 'none') != 'none',
                    }
                    for f in safe.get('formats', [])
                ],
                'subtitles': list(safe.get('subtitles', {}).keys()),
                'automatic_captions': list(safe.get('automatic_captions', {}).keys()),
            }
            return json.dumps(result)
    except ExtractorError as e:
        return json.dumps({'error': 'extractor', 'message': str(e)})
    except DownloadError as e:
        return json.dumps({'error': 'download', 'message': str(e)})
    except Exception as e:
        return json.dumps({'error': 'unknown', 'message': str(e)})


def download_video(url, output_dir, format_spec='best', 
                   cookies_file=None, ffmpeg_path=None):
    """Download a video.
    
    Args:
        url: YouTube URL
        output_dir: Directory to save the file
        format_spec: yt-dlp format string
        cookies_file: Optional path to cookies.txt
        ffmpeg_path: Optional path to ffmpeg binary
    
    Returns: JSON string with result.
    """
    opts = {
        'format': format_spec,
        'outtmpl': f'{output_dir}/%(title)s.%(ext)s',
        'quiet': True,
        'no_warnings': True,
        'noplaylist': True,
        'restrictfilenames': True,
        'continuedl': True,
        'retries': 3,
        'fragment_retries': 3,
        'logger': AndroidLogger(),
        'progress_hooks': [_progress_hook],
    }
    if cookies_file:
        opts['cookiefile'] = cookies_file
    if ffmpeg_path:
        opts['ffmpeg_location'] = ffmpeg_path
    
    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=True)
            if info is None:
                return json.dumps({'error': 'no_info'})
            
            filename = ydl.prepare_filename(info)
            return json.dumps({
                'success': True,
                'filename': filename,
                'title': info.get('title'),
                'id': info.get('id'),
            })
    except Exception as e:
        return json.dumps({'error': type(e).__name__, 'message': str(e)})


def get_available_qualities(url):
    """Get list of available quality options for quick UI display."""
    opts = {
        'quiet': True,
        'no_warnings': True,
        'noplaylist': True,
        'logger': AndroidLogger(),
    }
    
    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=False)
            if not info:
                return json.dumps([])
            
            qualities = set()
            for f in info.get('formats', []):
                h = f.get('height')
                if h and f.get('vcodec', 'none') != 'none':
                    qualities.add(h)
            
            return json.dumps(sorted(qualities, reverse=True))
    except Exception:
        return json.dumps([])
```

### Approach B: Process-Based (yt-dlp Binary)

Bundle yt-dlp as a standalone binary (via PyInstaller or zipapp) and invoke it via `ProcessBuilder`.

**Pros:**
- Simpler integration
- Process isolation (crashes don't affect app)
- Can use `--print-json` for structured output

**Cons:**
- Slower startup per operation
- Harder to track progress in real-time
- Larger binary size
- No in-process format selection API

```kotlin
// Invoke yt-dlp as a process
fun extractInfo(url: String): String {
    val process = ProcessBuilder(
        listOf(ytdlpBinaryPath, "--dump-json", "--no-download", url)
    ).redirectErrorStream(true).start()
    
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor(60, TimeUnit.SECONDS)
    return output  // JSON string
}
```

### Approach C: JNI + libcurl + Custom Extraction

Reimplement extraction logic in Kotlin/Java using YouTube's InnerTube API directly.

**Pros:**
- No Python dependency
- Smallest APK size
- Full native performance

**Cons:**
- Must maintain YouTube API compatibility yourself (YouTube changes frequently)
- yt-dlp has 500+ contributors fixing YouTube breakage — you'd be alone
- Very complex (PO tokens, JS challenges, cipher decryption)

**NOT RECOMMENDED** unless you have a dedicated team to maintain YouTube compatibility.

---

## 8. Android-Specific Considerations

### Storage and Permissions

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<!-- For API < 29 (Q) -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

For API 29+, use `MediaStore` or app-specific directory:
```kotlin
val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
    ?: context.filesDir
```

### Background Downloads (WorkManager)

Always run downloads in a foreground service or WorkManager:

```kotlin
class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString("url") ?: return@withContext Result.failure()
        val outputDir = inputData.getString("output_dir") ?: return@withContext Result.failure()
        val format = inputData.getString("format") ?: "best"
        
        setForeground(createForegroundInfo("Downloading..."))
        
        try {
            val py = Python.getInstance()
            val bridge = py.getModule("ytdlp_bridge")
            bridge.callAttr("set_callbacks", progressCallback, logCallback)
            
            val resultJson = bridge.callAttr(
                "download_video", url, outputDir, format
            ).toString()
            
            val result = JSONObject(resultJson)
            if (result.optBoolean("success", false)) {
                Result.success(workDataOf("filename" to result.getString("filename")))
            } else {
                Result.failure(workDataOf("error" to result.optString("message")))
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

### Network Considerations

```python
# Mobile-optimized options
opts = {
    'socket_timeout': 30,
    'retries': 5,
    'fragment_retries': 5,
    'http_chunk_size': 5242880,    # 5MB chunks (smaller for mobile)
    'continuedl': True,            # CRITICAL: resume on network change
    'noprogress': True,
    'sleep_interval_requests': 1,  # Be nice to YouTube
}
```

### Updating yt-dlp

YouTube frequently changes its API. yt-dlp updates are essential:

```kotlin
// With Chaquopy, update via pip
fun updateYtDlp() {
    val py = Python.getInstance()
    py.getModule("pip").callAttr("main", listOf("install", "--upgrade", "yt-dlp"))
}
```

Or bundle a specific version and update the app.

### Recommended Architecture

```
┌─────────────────────────────────────────────────┐
│                Android App (Kotlin)              │
│  ┌────────────┐  ┌──────────┐  ┌─────────────┐ │
│  │   UI Layer  │  │ ViewModel│  │  Repository  │ │
│  │  (Compose)  │←→│          │←→│              │ │
│  └────────────┘  └──────────┘  └──────┬──────┘ │
│                                       │         │
│  ┌───────────────────────────────────┐│         │
│  │        WorkManager / Service      ││         │
│  │  ┌─────────────────────────────┐  ││         │
│  │  │    Chaquopy Python Runtime  │  ││         │
│  │  │  ┌───────────────────────┐  │  ││         │
│  │  │  │   ytdlp_bridge.py    │  │  ││         │
│  │  │  │  ┌─────────────────┐  │  │  ││         │
│  │  │  │  │     yt-dlp      │  │  │  ││         │
│  │  │  │  └─────────────────┘  │  │  ││         │
│  │  │  └───────────────────────┘  │  ││         │
│  │  └─────────────────────────────┘  ││         │
│  └───────────────────────────────────┘│         │
│                                       │         │
│  ┌──────────────┐  ┌───────────────┐  │         │
│  │   Room DB    │  │  FFmpeg-kit   │  │         │
│  │  (metadata)  │  │  (optional)   │  │         │
│  └──────────────┘  └───────────────┘  │         │
└─────────────────────────────────────────────────┘
```

---

## 9. Quick Reference: Common Operations

### Extract info only
```python
with yt_dlp.YoutubeDL({'quiet': True}) as ydl:
    info = ydl.extract_info(url, download=False)
```

### Download best quality
```python
with yt_dlp.YoutubeDL({'format': 'best', 'outtmpl': '%(title)s.%(ext)s'}) as ydl:
    ydl.download([url])
```

### Download audio only
```python
with yt_dlp.YoutubeDL({'format': 'm4a/bestaudio/best', 'postprocessors': [{'key': 'FFmpegExtractAudio', 'preferredcodec': 'm4a'}]}) as ydl:
    ydl.download([url])
```

### Get specific format by ID
```python
with yt_dlp.YoutubeDL({'format': '137+140'}) as ydl:  # 1080p video + audio
    ydl.download([url])
```

### Limit resolution
```python
with yt_dlp.YoutubeDL({'format': 'best[height<=720]'}) as ydl:
    ydl.download([url])
```

### Download with cookies (for age-restricted/private)
```python
with yt_dlp.YoutubeDL({'cookiefile': '/path/to/cookies.txt'}) as ydl:
    ydl.download([url])
```

### Custom format selector function
```python
def format_selector(ctx):
    formats = ctx.get('formats')[::-1]  # sorted worst to best, reverse
    # Find best MP4 with both audio+video
    for f in formats:
        if f['ext'] == 'mp4' and f.get('acodec') != 'none' and f.get('vcodec') != 'none':
            yield f
            return
    # Fallback: best anything
    yield formats[0]

with yt_dlp.YoutubeDL({'format': format_selector}) as ydl:
    ydl.download([url])
```

---

## 10. Important Notes

1. **yt-dlp requires Python >= 3.10**. Ensure Chaquopy bundles a compatible Python version.

2. **YouTube changes its API frequently**. Pin a yt-dlp version in production and update regularly.

3. **The `extract_info` return value is NOT guaranteed to be JSON-serializable**. Always pass through `sanitize_info()` before serialization.

4. **Format URLs expire**. Don't cache format URLs — they typically expire within hours. Cache `info_dict` metadata instead and re-extract URLs when needed.

5. **Rate limiting**: YouTube rate-limits aggressive requests. Use `sleep_interval_requests: 1` or higher for batch operations.

6. **Legal considerations**: Downloading copyrighted content may violate YouTube's Terms of Service and local laws. Ensure your app includes appropriate disclaimers.

7. **The `format` parameter can be a function**, not just a string. This allows programmatic format selection based on device capabilities (screen resolution, storage space, network type).

8. **For pre-merged formats** (no FFmpeg needed), use format `best` (not `bestvideo+bestaudio`). Pre-merged formats may cap at 720p on YouTube.
