<p align="center">
  <img src="icons/icon_2.jpeg" width="128" height="128" alt="Offline Tube"/>
</p>

<h1 align="center">Offline Tube</h1>

<p align="center">
  Android-приложение для офлайн-просмотра YouTube видео
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?logo=android" alt="Platform"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue" alt="Compose"/>
  <img src="https://img.shields.io/badge/MinSDK-24-orange" alt="MinSDK"/>
</p>

---

## Описание

**Offline Tube** — приложение для загрузки и офлайн-просмотра YouTube видео. Вставьте ссылку, выберите качество, скачайте — и смотрите без интернета.

Работает без Python, без yt-dlp — чистый Kotlin с прямым обращением к InnerTube API.

## Возможности

- 🔗 **Загрузка видео** — вставьте ссылку YouTube → получите информацию → скачайте
- 📋 **Очередь загрузки** — несколько загрузок одновременно с паузой/отменой/повтором
- 🎬 **Плейлист** — просмотр загруженных видео с обложками и длительностью
- ▶️ **Встроенный плеер** — запоминает позицию воспроизведения (продолжение с места остановки)
- ⚙️ **Настройки** — выбор качества (360p / 480p / 720p / 1080p), путь сохранения, загрузка только по Wi-Fi
- 📤 **Share Intent** — принимает ссылки YouTube из других приложений
- 🔄 **Фоновая загрузка** — работает даже после закрытия приложения

## Скриншоты

| Загрузка | Плейлист | Плеер |
|----------|----------|-------|
| *скоро*  | *скоро*  | *скоро* |

## Технологии

| Компонент | Технология |
|-----------|-----------|
| Язык | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Навигация | Bottom Navigation (3 вкладки) |
| База данных | Room (SQLite) |
| Плеер | ExoPlayer (Media3) |
| Сеть | OkHttp + Kotlin Serialization |
| Фоновая работа | WorkManager |
| DI | Hilt |
| Архитектура | MVVM + Clean Architecture |
| YouTube API | InnerTube API (ANDROID_VR клиент) |
| Логирование | Timber |
| Изображения | Coil |

## Сборка

### Требования

- Android Studio Hedgehog+ или JDK 11+
- Android SDK 34
- Устройство или эмулятор с Android 7.0+ (API 24)

### Команды

```bash
# Сборка и установка debug-версии
make install

# Запуск на устройстве
make run

# Только сборка
make debug

# Release сборка
make release

# Очистка
make clean

# Все доступные команды
make help
```

### Релиз

```bash
# Отредактируйте VERSION, затем:
make release-push
```

Это создаст тег `v<версия>`, запушит его в репозиторий, и GitHub Actions автоматически соберёт APK и создаст релиз.

## Архитектура

```
app/src/main/java/com/hightemp/offline_tube/
├── domain/          # Бизнес-логика (чистый Kotlin)
│   ├── model/       # Модели данных (Video, DownloadTask, ...)
│   ├── repository/  # Интерфейсы репозиториев
│   └── usecase/     # Use cases (ExtractVideoInfo, DownloadVideo)
├── data/            # Реализация данных
│   ├── local/       # Room (DAO, Entity, Database)
│   ├── remote/      # InnerTube API, VisitorDataManager
│   ├── repository/  # Реализации репозиториев
│   └── worker/      # WorkManager (DownloadWorker)
├── di/              # Hilt модули
└── ui/              # Jetpack Compose UI
    └── screens/
        ├── download/  # Экран загрузки
        ├── playlist/  # Экран плейлиста
        ├── player/    # Экран плеера
        └── settings/  # Экран настроек
```

## Как это работает

1. Пользователь вставляет ссылку YouTube
2. Приложение извлекает `videoId` из URL
3. Запрашивает `VisitorData` с YouTube для авторизации
4. Обращается к InnerTube `/player` API (клиент ANDROID_VR) для получения метаданных и ссылок на потоки
5. Выбирает лучший формат с аудио и видео в рамках указанного качества
6. WorkManager скачивает файл в фоне с прогрессом и уведомлениями
7. Видео сохраняется в локальное хранилище и добавляется в плейлист

## Лицензия

MIT

![](https://asdertasd.site/counter/offline_tube)