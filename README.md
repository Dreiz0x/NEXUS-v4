# NEXUS — Personal Document Intelligence System

```
 ███╗   ██╗███████╗██╗  ██╗██╗   ██╗███████╗
 ████╗  ██║██╔════╝╚██╗██╔╝██║   ██║██╔════╝
 ██╔██╗ ██║█████╗   ╚███╔╝ ██║   ██║███████╗
 ██║╚██╗██║██╔══╝   ██╔██╗ ██║   ██║╚════██║
 ██║ ╚████║███████╗██╔╝ ╚██╗╚██████╔╝███████║
 ╚═╝  ╚═══╝╚══════╝╚═╝   ╚═╝ ╚═════╝ ╚══════╝
 PERSONAL DOCUMENT INTELLIGENCE SYSTEM
```

**100% Local | Zero Telemetry | Zero External Servers**

---

## Overview

NEXUS is a native Android application that automatically indexes all documents on your device and enables intelligent search through natural language queries. It connects to a local OpenAI-compatible API for semantic search capabilities, ensuring all your data stays on your device.

### Key Features

- **Automatic Document Indexing**: Scans and indexes PDF, Word, Excel, PowerPoint, images (OCR), TXT, and CSV files
- **Full-Text Search**: Instant text-based search across all indexed documents
- **Semantic Search**: Natural language queries powered by a local AI API
- **OCR Support**: Extracts text from images using Tesseract
- **File Monitoring**: Detects new or modified files and updates the index automatically
- **Local Network Sharing**: Access documents from other devices on the same WiFi
- **HUD Interface**: Futuristic military-inspired interface with animated elements
- **Document Preview**: Opens documents directly with the system's default app
- **Zero Telemetry**: No data ever leaves your device

---

## Screenshots

The interface features a futuristic HUD design with:
- Deep black background with cyan grid overlay
- Animated scan lines and radar pulse
- Holographic document cards with floating metadata
- Color-coded document type badges
- Real-time indexing progress bars

---

## Architecture

```
nexus-app/
├── app/src/main/java/com/nexus/intelligence/
│   ├── NexusApplication.kt          # Hilt Application
│   ├── MainActivity.kt              # Entry point + permissions
│   ├── data/
│   │   ├── local/
│   │   │   ├── entity/              # Room entities
│   │   │   ├── dao/                 # Room DAOs
│   │   │   └── database/            # Room database
│   │   ├── parser/                  # Document parsers (PDF, Office, OCR)
│   │   ├── embeddings/              # Embedding service (local API)
│   │   └── repository/              # Repository implementation
│   ├── domain/
│   │   ├── model/                   # Domain models
│   │   ├── repository/              # Repository interface
│   │   └── usecase/                 # Business logic use cases
│   ├── service/
│   │   ├── indexing/                # Background indexing + FileObserver
│   │   └── network/                 # NSD discovery + Ktor HTTP server
│   ├── di/                          # Hilt dependency injection
│   └── ui/
│       ├── theme/                   # HUD theme (colors, typography)
│       ├── components/              # Reusable HUD components
│       ├── screens/                 # Dashboard, Search, FileMap, Settings
│       ├── viewmodel/               # ViewModels for each screen
│       └── navigation/              # Navigation graph
├── .github/workflows/
│   └── android-build.yml            # GitHub Actions CI/CD
└── gradle/                          # Gradle wrapper
```

### Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt (Dagger) |
| Database | Room (SQLite) |
| PDF Parsing | PdfBox-Android |
| Office Parsing | Apache POI |
| OCR | Tesseract4Android |
| Embeddings | OkHttp + OpenAI-compatible API |
| Network Server | Ktor (Netty) |
| Service Discovery | Android NSD |
| Navigation | Navigation Compose |

---

## Prerequisites

### For Building

- **JDK 17** or higher
- **Android SDK** with API level 34
- **Android Build Tools** 34.0.0

### For Running

- Android device or emulator running **Android 8.0 (API 26)** or higher
- For semantic search: a local OpenAI-compatible API running on `127.0.0.1:8080`
  - Recommended: [llama.cpp server](https://github.com/ggerganov/llama.cpp), [Ollama](https://ollama.ai), or [LocalAI](https://localai.io)

### OCR Setup (Optional)

To enable OCR for images, you need to include Tesseract trained data:

1. Download `eng.traineddata` from [tessdata](https://github.com/tesseract-ocr/tessdata)
2. Place it in `app/src/main/assets/tessdata/eng.traineddata`
3. For additional languages, add the corresponding `.traineddata` files

---

## Building

### Option 1: GitHub Actions (Recommended)

This is the easiest method. Simply push the code to GitHub:

```bash
# Initialize git repository
git init
git add .
git commit -m "Initial commit: NEXUS v1.0.0"

# Add your GitHub repository as remote
git remote add origin https://github.com/YOUR_USERNAME/nexus-app.git
git push -u origin main
```

The GitHub Actions workflow will automatically:
1. Set up JDK 17 and Android SDK
2. Build the debug APK
3. Upload the APK as a build artifact

Download the APK from the **Actions** tab > latest workflow run > **Artifacts**.

### Option 2: Local Build (Android Studio)

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Select `Build > Build Bundle(s) / APK(s) > Build APK(s)`
4. Find the APK in `app/build/outputs/apk/debug/`

### Option 3: Command Line

```bash
# Make gradlew executable (if needed)
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# The APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## Installation

1. Transfer the APK to your Android device
2. Enable "Install from unknown sources" in Settings
3. Open the APK file to install
4. Grant storage permissions when prompted

---

## Configuration

### API Endpoint

NEXUS connects to a local OpenAI-compatible API for semantic search. Configure the endpoint in **Settings > API ENDPOINT**.

Default: `http://127.0.0.1:8080`

The API must support:
- `GET /v1/models` — Health check
- `POST /v1/embeddings` — Generate text embeddings
- `POST /v1/chat/completions` — Chat completion for semantic search

### Monitored Folders

Add folders to monitor in **Settings > MONITORED FOLDERS**. By default, NEXUS scans the entire external storage.

### Network Sharing

Enable the local server in **Settings > NETWORK SERVER** to share your document index with other NEXUS instances on the same WiFi network.

---

## Supported File Types

| Type | Extensions | Parser |
|------|-----------|--------|
| PDF | `.pdf` | PdfBox-Android |
| Word | `.doc`, `.docx` | Apache POI |
| Excel | `.xls`, `.xlsx` | Apache POI |
| PowerPoint | `.ppt`, `.pptx` | Apache POI |
| Images (OCR) | `.jpg`, `.jpeg`, `.png`, `.bmp`, `.tiff`, `.webp` | Tesseract4Android |
| Text | `.txt`, `.md`, `.log`, `.json`, `.xml`, `.html` | Native |
| CSV | `.csv` | Native |

---

## Privacy & Security

- **Zero telemetry**: No analytics, no tracking, no data collection
- **Zero external servers**: All processing happens on-device
- **No internet required**: Works completely offline (semantic search requires local API)
- **No Play Store**: Distributed as a direct APK install
- **Open architecture**: All code is transparent and auditable

---

## Troubleshooting

### Build fails with "Out of Memory"

Add to `gradle.properties`:
```
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
```

### OCR not working

Ensure `eng.traineddata` is placed in `app/src/main/assets/tessdata/`.

### Semantic search not working

1. Verify your local API is running: `curl http://127.0.0.1:8080/v1/models`
2. Check the endpoint in Settings
3. Use the [TEST] button to verify connectivity

### Storage permission denied

On Android 11+, NEXUS requires "All Files Access" permission. Go to:
`Settings > Apps > NEXUS > Permissions > Files and media > Allow management of all files`

---

## License

This project is provided as-is for personal use. All rights reserved.

---

```
NEXUS v1.0.0 // LOCAL MODE // ZERO TELEMETRY
Built with Kotlin + Jetpack Compose
```
