# AGENTS.md

> Project map for AI agents. Keep this file up-to-date as the project evolves.

## Project Overview
Android application for offline YouTube video viewing. Downloads videos via YouTube's InnerTube API (Kotlin-only, no Python dependency), manages a download queue, and provides a playlist with playback position memory.

## Tech Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose + Material 3
- **Database:** Room (SQLite)
- **Video Player:** ExoPlayer (Media3)
- **Network:** OkHttp + Kotlin Serialization
- **Background Work:** WorkManager
- **DI:** Hilt
- **Architecture:** Clean Architecture (MVVM)

## Project Structure
```
offline_tube/
├── app/                              # Android application module
│   ├── build.gradle.kts              # App dependencies and config
│   ├── proguard-rules.pro            # ProGuard rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # App manifest
│       │   ├── java/com/hightemp/offline_tube/
│       │   │   ├── domain/           # Business logic (pure Kotlin)
│       │   │   ├── data/             # Data layer (Room, API, repos)
│       │   │   ├── di/               # Hilt DI modules
│       │   │   └── ui/               # Compose screens + ViewModels
│       │   └── res/                  # Android resources
│       ├── test/                     # Unit tests
│       └── androidTest/              # Instrumentation tests
├── build.gradle.kts                  # Project-level build config
├── settings.gradle.kts               # Module definitions
├── gradle/
│   └── libs.versions.toml            # Version catalog
├── .ai-factory/                      # AI context files
│   ├── DESCRIPTION.md                # Project specification
│   ├── ARCHITECTURE.md               # Architecture decisions
│   └── INNERTUBE_API_RESEARCH.md     # YouTube API research
├── .github/skills/                   # Custom skills
│   ├── youtube-innertube-kotlin/     # InnerTube API implementation guide
│   └── yt-dlp-android/              # yt-dlp reference (research)
├── .agents/skills/                   # Installed external skills
│   └── android-kotlin/               # Android Kotlin best practices
└── tmp/
    └── yt-dlp/                       # yt-dlp source code (reference only)
```

## Key Entry Points
| File | Purpose |
|------|---------|
| app/src/main/java/com/hightemp/offline_tube/MainActivity.kt | Single Activity, hosts Compose navigation |
| app/src/main/AndroidManifest.xml | App manifest with permissions and activities |
| app/build.gradle.kts | App module dependencies and build configuration |
| gradle/libs.versions.toml | Centralized version catalog |

## Documentation
| Document | Path | Description |
|----------|------|-------------|
| README | README.md | Project landing page (to be created) |
| InnerTube API | .ai-factory/INNERTUBE_API_RESEARCH.md | YouTube InnerTube API technical research |

## AI Context Files
| File | Purpose |
|------|---------|
| AGENTS.md | This file — project structure map |
| .ai-factory/DESCRIPTION.md | Project specification and tech stack |
| .ai-factory/ARCHITECTURE.md | Architecture decisions and guidelines |
| .github/skills/youtube-innertube-kotlin/SKILL.md | YouTube InnerTube API Kotlin implementation guide |
| .agents/skills/android-kotlin/SKILL.md | Android Kotlin development best practices |
