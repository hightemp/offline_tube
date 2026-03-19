# Implementation Plan: Offline Tube

## Context
- **Branch:** main
- **Testing:** No test tasks — tests later
- **Logging:** Timber, configurable LOG_LEVEL

## Tasks

- [ ] Task 1: Setup Gradle dependencies (Hilt, Room, ExoPlayer, OkHttp, Navigation, DataStore, Coil, Timber, KSP, Kotlin Serialization)
- [ ] Task 2: Create domain models (Video, DownloadTask, PlaybackPosition, VideoQuality, DownloadStatus, YouTubeError)
- [ ] Task 3: Create repository interfaces (VideoRepository, DownloadRepository, SettingsRepository)
- [ ] Task 4: Create use cases (ExtractVideoInfo, DownloadVideo, GetPlaylist, SavePlaybackPosition)
- [ ] Task 5: Create Room entities & DAOs (VideoEntity/Dao, DownloadEntity/Dao, PlaybackPositionEntity/Dao)
- [ ] Task 6: Create Room AppDatabase
- [ ] Task 7: Create InnerTube API layer (InnerTubeConfig, DTOs, InnerTubeApi client)
- [ ] Task 8: Create data mappers (VideoMapper, DownloadMapper)
- [ ] Task 9: Create repository implementations (VideoRepositoryImpl, DownloadRepositoryImpl, SettingsRepositoryImpl)
- [ ] Task 10: Create Hilt DI modules (AppModule, RepositoryModule)
- [ ] Task 11: Create DownloadWorker (WorkManager background download)
- [ ] Task 12: Create Application class (OfflineTubeApp with Hilt + Timber)
- [ ] Task 13: Create UI theme (Color, Type, Theme — Material 3)
- [ ] Task 14: Create reusable UI components (VideoCard, DownloadQueueItem, QualitySelector)
- [ ] Task 15: Create navigation (Screen routes, AppNavigation with BottomBar)
- [ ] Task 16: Create DownloadScreen + DownloadViewModel
- [ ] Task 17: Create PlaylistScreen + PlaylistViewModel
- [ ] Task 18: Create PlayerScreen + PlayerViewModel (ExoPlayer + position save)
- [ ] Task 19: Create SettingsScreen + SettingsViewModel
- [ ] Task 20: Update MainActivity (Hilt, navigation host) + AndroidManifest (permissions, share intent)

## Commit Checkpoints
- After Task 1 (gradle setup)
- After Task 6 (domain + data models complete)
- After Task 11 (data layer complete)
- After Task 20 (all done)
