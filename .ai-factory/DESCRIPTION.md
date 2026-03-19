# Project: Offline Tube

## Overview
Android application for offline viewing of YouTube videos. Users can paste a YouTube link, the app extracts video metadata via YouTube's InnerTube API (Kotlin-only, no Python dependency), downloads the video to local storage, and adds it to a playlist. The app supports download queues with error handling, playback position memory (resume from where you stopped), and configurable video quality settings.

## Core Features
- **Video Download**: Paste YouTube URL → extract metadata → select quality → download
- **Download Queue**: Multiple downloads managed in a queue with pause/resume/retry/cancel
- **Playlist**: Browse downloaded videos with thumbnails, titles, duration
- **Video Player**: Built-in player with playback position persistence (resume from last stop)
- **Settings**: Video quality selection (360p/480p/720p/1080p), storage path, download over WiFi only, etc.
- **Error Handling**: Network errors, invalid URLs, geo-blocked content, storage full — all handled gracefully
- **Share Intent**: Accept YouTube URLs shared from other apps

## Tech Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose + Material 3
- **Navigation:** Bottom Navigation Bar (3 tabs: Download, Playlist, Settings)
- **Database:** Room (SQLite) — video metadata, download queue, playback positions
- **Video Player:** ExoPlayer (Media3)
- **Network:** OkHttp + Kotlin Serialization for InnerTube API
- **Background Work:** WorkManager for download queue
- **DI:** Hilt
- **Architecture:** MVVM + Clean Architecture
- **YouTube Integration:** Direct InnerTube API calls (ANDROID_VR client — no JS player, no PO token, direct URLs)

## Architecture Notes
- InnerTube API uses ANDROID_VR client identity to get direct download URLs without signature cipher or PO tokens
- Downloads managed via WorkManager for reliability (survives app kill, supports constraints like WiFi-only)
- Room database stores: video metadata, download status/progress, playback position per video
- ExoPlayer (Media3) for video playback with built-in position save/restore
- Kotlin Coroutines + Flow for reactive data flow
- Repository pattern for data access abstraction

## Non-Functional Requirements
- Logging: Configurable via LOG_LEVEL, Timber for structured logging
- Error handling: Structured error responses with user-friendly messages
- Security: No credentials stored, HTTPS-only API calls
- Storage: App-specific external storage (no special permissions needed on Android 10+)
- Performance: Chunked downloads, background processing, lazy loading of thumbnails
- Offline-first: All UI works without network, downloads resume on reconnect

## Architecture
See `.ai-factory/ARCHITECTURE.md` for detailed architecture guidelines.
Pattern: Clean Architecture (MVVM)
