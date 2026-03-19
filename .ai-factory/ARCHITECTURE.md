# Architecture: Clean Architecture (MVVM)

## Overview

Offline Tube follows Clean Architecture with MVVM presentation pattern, adapted for Android with Jetpack Compose. The architecture separates the app into three main layers: **domain** (business logic), **data** (repositories, API, database), and **presentation** (UI + ViewModels). Dependencies point inward — domain has zero dependencies on Android or external libraries.

This architecture was chosen because:
- The app has moderate business logic (download queue management, format selection, playback tracking)
- Clean separation makes the YouTube API integration easy to test and update when YouTube changes their API
- MVVM with Compose provides reactive UI updates for download progress, queue status, etc.

## Decision Rationale
- **Project type:** Offline video downloader with queue management
- **Tech stack:** Kotlin, Jetpack Compose, Room, ExoPlayer, OkHttp
- **Key factor:** YouTube's InnerTube API may change — isolating it behind interfaces enables quick updates without affecting the rest of the app

## Folder Structure
```
app/src/main/java/com/hightemp/offline_tube/
├── OfflineTubeApp.kt                    # Application class (@HiltAndroidApp)
├── MainActivity.kt                      # Single Activity, hosts Compose navigation
├── domain/                              # Pure Kotlin — no Android imports
│   ├── model/                           # Domain entities
│   │   ├── Video.kt                     # Video metadata (id, title, duration, thumbnail)
│   │   ├── DownloadTask.kt              # Download queue item (status, progress, error)
│   │   ├── PlaybackPosition.kt          # Video playback position tracking
│   │   └── VideoQuality.kt              # Quality enum (360p, 480p, 720p, 1080p)
│   ├── repository/                      # Repository interfaces
│   │   ├── VideoRepository.kt
│   │   ├── DownloadRepository.kt
│   │   └── SettingsRepository.kt
│   └── usecase/                         # Business logic
│       ├── ExtractVideoInfoUseCase.kt   # Extract metadata from YouTube URL
│       ├── DownloadVideoUseCase.kt      # Start download, manage queue
│       ├── GetPlaylistUseCase.kt        # Get downloaded videos list
│       └── SavePlaybackPositionUseCase.kt
├── data/                                # Data layer — implements domain interfaces
│   ├── local/                           # Room database
│   │   ├── AppDatabase.kt              # Room database definition
│   │   ├── dao/
│   │   │   ├── VideoDao.kt
│   │   │   ├── DownloadDao.kt
│   │   │   └── PlaybackPositionDao.kt
│   │   └── entity/                      # Room entities (DB models)
│   │       ├── VideoEntity.kt
│   │       ├── DownloadEntity.kt
│   │       └── PlaybackPositionEntity.kt
│   ├── remote/                          # YouTube InnerTube API
│   │   ├── InnerTubeApi.kt             # API client (OkHttp + Kotlin Serialization)
│   │   ├── dto/                         # API request/response DTOs
│   │   │   ├── PlayerRequest.kt
│   │   │   └── PlayerResponse.kt
│   │   └── InnerTubeConfig.kt          # API constants (client identity, URLs)
│   ├── repository/                      # Repository implementations
│   │   ├── VideoRepositoryImpl.kt
│   │   ├── DownloadRepositoryImpl.kt
│   │   └── SettingsRepositoryImpl.kt
│   ├── mapper/                          # Entity ↔ Domain model mappers
│   │   ├── VideoMapper.kt
│   │   └── DownloadMapper.kt
│   └── worker/                          # WorkManager workers
│       └── DownloadWorker.kt            # Background download execution
├── di/                                  # Hilt dependency injection modules
│   ├── AppModule.kt                     # Singletons: DB, OkHttp, DataStore
│   ├── RepositoryModule.kt             # Binds repository implementations
│   └── UseCaseModule.kt                # Provides use cases
└── ui/                                  # Presentation layer (Compose + ViewModels)
    ├── navigation/
    │   ├── AppNavigation.kt             # NavHost + Bottom Navigation Bar
    │   └── Screen.kt                    # Route definitions
    ├── download/                        # Download screen (paste URL, queue)
    │   ├── DownloadScreen.kt
    │   └── DownloadViewModel.kt
    ├── playlist/                        # Playlist screen (downloaded videos)
    │   ├── PlaylistScreen.kt
    │   └── PlaylistViewModel.kt
    ├── player/                          # Video player screen
    │   ├── PlayerScreen.kt
    │   └── PlayerViewModel.kt
    ├── settings/                        # Settings screen
    │   ├── SettingsScreen.kt
    │   └── SettingsViewModel.kt
    ├── components/                      # Reusable Compose components
    │   ├── VideoCard.kt                 # Video thumbnail + title card
    │   ├── DownloadQueueItem.kt         # Queue item with progress bar
    │   └── QualitySelector.kt           # Quality picker dialog
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

## Dependency Rules

Dependencies point **inward** — outer layers depend on inner, never the reverse.

```
Presentation (UI)  →  Domain (Use Cases + Interfaces)  ←  Data (Implementations)
     ↓                        ↑                                    ↑
  ViewModels            Pure Kotlin                          Room, OkHttp
  Compose UI           No Android deps                    WorkManager, DataStore
```

- ✅ `ui/` depends on `domain/` (ViewModels call use cases)
- ✅ `data/` depends on `domain/` (implements repository interfaces)
- ✅ `di/` depends on all layers (wiring only)
- ❌ `domain/` MUST NOT depend on `data/` or `ui/`
- ❌ `domain/` MUST NOT import Android classes
- ❌ `ui/` MUST NOT access `data/` directly (always through use cases/repositories)
- ❌ ViewModels MUST NOT reference Compose or Activity classes

## Layer/Module Communication

- **UI → Domain**: ViewModels inject use cases via Hilt constructor injection
- **Domain → Data**: Use cases call repository interfaces; Hilt provides implementations
- **Data → External**: Repository implementations use OkHttp (InnerTube API), Room (database), DataStore (settings)
- **Background Work**: `DownloadWorker` (WorkManager) calls repository methods directly — it's infrastructure code
- **Reactive Data Flow**: Room DAOs return `Flow<>`, which flows through repositories → use cases → ViewModels → Compose UI via `collectAsStateWithLifecycle()`

## Key Principles

1. **Single Source of Truth**: Room database is the SSOT for all video metadata, download status, and playback positions. UI always reads from DB, never directly from API responses.

2. **Unidirectional Data Flow**: User action → ViewModel → UseCase → Repository → DB update → Flow emission → UI update

3. **Offline-First**: All UI state reads from local DB. Network calls only happen when user initiates a download or extracts video info.

4. **Background Safety**: Downloads run in WorkManager workers, surviving app kills and device restarts. Never use plain coroutines for long downloads.

5. **Error Isolation**: YouTube API errors are caught at the data layer, mapped to domain error types, and presented with user-friendly messages at the UI layer.

## Code Examples

### Domain Layer — Use Case
```kotlin
class ExtractVideoInfoUseCase @Inject constructor(
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke(url: String): Result<Video> {
        val videoId = VideoUrlParser.extractVideoId(url)
            ?: return Result.failure(YouTubeError.InvalidUrl(url))
        return videoRepository.extractVideoInfo(videoId)
    }
}
```

### Data Layer — Repository
```kotlin
class VideoRepositoryImpl @Inject constructor(
    private val innerTubeApi: InnerTubeApi,
    private val videoDao: VideoDao,
    private val videoMapper: VideoMapper
) : VideoRepository {

    override suspend fun extractVideoInfo(videoId: String): Result<Video> {
        return try {
            val response = innerTubeApi.getPlayerResponse(videoId)
            if (response.playabilityStatus.status != "OK") {
                Result.failure(YouTubeError.VideoUnavailable(
                    response.playabilityStatus.reason ?: "Unknown error"
                ))
            } else {
                val video = videoMapper.fromResponse(response)
                videoDao.insert(videoMapper.toEntity(video))
                Result.success(video)
            }
        } catch (e: IOException) {
            Result.failure(YouTubeError.NetworkError(e))
        }
    }
}
```

### Presentation Layer — ViewModel
```kotlin
@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val extractVideoInfoUseCase: ExtractVideoInfoUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    val downloadQueue: StateFlow<List<DownloadTask>> =
        downloadRepository.observeQueue()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun extractVideoInfo(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            extractVideoInfoUseCase(url)
                .onSuccess { video ->
                    _uiState.update { it.copy(isLoading = false, videoInfo = video) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
```

## Anti-Patterns

- ❌ **Direct API calls from ViewModel** — Always go through use cases/repositories
- ❌ **Downloading in a regular coroutine** — Use WorkManager for reliability
- ❌ **Storing video files paths as hardcoded strings** — Use `Context.getExternalFilesDir()`
- ❌ **Blocking the main thread for DB operations** — Room + suspend functions handle this
- ❌ **Exposing MutableStateFlow to UI** — Always expose read-only `StateFlow`
- ❌ **Not handling URL expiration** — YouTube download URLs expire; always extract fresh before downloading
- ❌ **GlobalScope for downloads** — Use structured concurrency (viewModelScope or WorkManager)
- ❌ **Skipping error mapping** — Never show raw HTTP/API errors to users; map to user-friendly messages
