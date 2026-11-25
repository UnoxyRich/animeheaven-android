# AnimeHeaven Android (`me.animeheaven.android`)

Native Jetpack Compose + Media3 Android **Phone & Tablet** application for browsing and streaming anime from `animeheaven.me`, sharing the 6-pillar optimized engine with `animeheaven-tv` and tailored for touch, portrait/landscape folding layouts, Picture-in-Picture, and mobile player gestures.

> **Companion Repository**: For the Android TV / Leanback D-pad edition (`me.animeheaven.tv`), see the sibling repository `animeheaven-tv`. Both repositories share the core networking, Room persistence, HTML parsing, predictive preloading, and multi-mirror Media3 booster architecture, while adapting manifests, chrome, search input, and playback controls to their target devices.

## Screenshots

<p align="center">
  <img src="docs/screenshots.jpg" alt="AnimeHeaven Android Screenshots — Home, Popular, Detail, and Settings" width="720" />
</p>
<p align="center"><sub>Home (Midnight Indigo) · Popular Today (Cinema Dark) · Anime Detail (Classic Light) · Settings &amp; 8 Themes</sub></p>

---

## Key Capabilities

- **Phone & Tablet Touch UI**: Edge-to-edge Material 3 layout with `MobileTopBar`, `MobileBottomBar`, `PullToRefreshBox`, adaptive multi-column grids (`BoxWithConstraints`), and native soft-keyboard (`BasicTextField` + `ImeAction.Search`) instant search.
- **Gesture-Driven Mobile Player**:
  - Auto-landscape fullscreen toggle (`autoLandscapePlayer`) + manual orientation switcher, restoring `SCREEN_ORIENTATION_UNSPECIFIED` on exit
  - **Picture-in-Picture (`API 26+`)** via `onUserLeaveHint()` and manual PiP button
  - **Double-tap left/right** seek (`< 38%` / `> 62%` width, configurable `5 / 10 / 15s`, center double-tap toggles play/pause)
  - **Vertical drag gestures** on left half (`window.attributes.screenBrightness`) & right half (`AudioManager.STREAM_MUSIC`)
  - **Interactive Canvas scrubber** (tap & horizontal drag) + speed selector (`0.75x`..`2.0x`), resize mode (`Fit`/`Fill`/`Zoom`), and **screen lock** toggle
- **Multi-Mirror Parallel Streaming & Caching**: Shared byte-length episode cache keys (`ep:<episodeKey>:<contentLength>`), throughput-ranked mirror failover (`MirrorHealthTracker`), 512 KB parallel HTTP range boosting (`StreamingChunkBooster`), and non-faststart MP4 `moov` tail warming.
- **Instant Cold Start**: Off-main-thread lazy subsystem initialization (`AppContainer`) with fast `home_snapshot.v1.json` hydration before Room or network requests finish.
- **AniSkip & AniList Integration**: Coalesced AniList `idMal` resolution + AniSkip OP/ED timestamp overlays with configurable fallback manual skip (`default 85s`) and Up Next countdown.
- **Compact Binary (~2.32 MB Release APK)**: R8 full mode, `material-icons-core` + extracted vectors in [`AhIcons.kt`](app/src/main/java/me/animeheaven/android/ui/components/AhIcons.kt), English-only resource config, and a targeted Baseline Profile.

---

## Tech Stack

| Layer | Technology / Library | Version |
| :--- | :--- | :--- |
| Language / Build | Kotlin, AGP, KSP | `2.0.21` / `8.7.3` / `2.0.21-1.0.28` |
| Android SDK | `minSdk 24`, `targetSdk 35`, `compileSdk 35`, Java 17 | — |
| UI | Jetpack Compose (`composeBom`), Material 3, Navigation Compose | `2025.02.00` / `2.8.6` |
| Media Playback | Media3 (`exoplayer`, `ui`, `datasource-okhttp`) | `1.5.1` |
| Networking & Parsing | OkHttp, Jsoup | `4.12.0` / `1.18.3` |
| Images | Coil 3 (`coil-compose`, `coil-network-okhttp`) | `3.0.4` |
| Persistence | Room (`room-runtime`, `room-ktx`, `room-compiler`) + `SharedPreferences` | `2.6.1` |
| Concurrency & Startup | Kotlinx Coroutines, ProfileInstaller | `1.9.0` / `1.4.1` |
| Testing | JUnit 4, `kotlinx-coroutines-test` | `4.13.2` / `1.9.0` |

Dependency versions are centralized in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

---

## Quick Start

### Prerequisites
- JDK 17
- Android SDK Platform 35 + Build Tools (configured via `local.properties` or `ANDROID_HOME`)

### Common Commands

```bash
# Run all JVM unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Build R8-minified release APK + run lintVitalRelease
./gradlew assembleRelease

# Build release APK + generate Compose compiler stability & skippability reports
./gradlew testDebugUnitTest lintDebug compileDebugAndroidTestKotlin assembleRelease -PcomposeCompilerReports=true

# Install an R8-optimized, debug-signed build with a separate application ID
./gradlew installSideload
```

Release APKs are unsigned unless all of `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` are set as environment variables or Gradle properties. Use `-PversionCode=… -PversionName=…` to set release versions. Auto Backup is disabled; local settings and watch history are not restored after reinstall.

Compose compiler reports are generated under `app/build/compose_compiler/` when `-PcomposeCompilerReports=true` is passed.

---

## Repository Structure

```text
app/
├── compose_stability.conf                  # Explicit stability contracts for library references
├── proguard-rules.pro                      # R8 full-mode optimization & selective keep rules
└── src/
    ├── main/
    │   ├── AndroidManifest.xml             # Resizable, PiP-enabled portrait/landscape activity
    │   ├── baseline-prof.txt               # Targeted AOT Baseline Profile rules
    │   ├── res/                            # Adaptive launcher icons, strings, Material theme
    │   └── java/me/animeheaven/android/
    │       ├── AnimeHeavenApplication.kt   # Application startup, Coil singleton, trimMemory hook
    │       ├── AppContainer.kt             # Lazy DI container & async warm-up coordinator
    │       ├── MainActivity.kt             # Edge-to-edge bars, PiP lifecycle & theme-scoped root
    │       ├── data/
    │       │   ├── cache/                  # VideoCacheManager, StreamingChunkBooster, AnimePreloader, Codecs, LRU
    │       │   ├── local/                  # Room database (v3) & SharedPreferences settings store
    │       │   ├── model/                  # @Immutable UI/domain models & theme/preload enums
    │       │   ├── network/                # OkHttp client, DNS cache, NetworkPolicy, AniSkip/AniList
    │       │   ├── parser/                 # Pre-compiled Jsoup HTML selectors & parsers
    │       │   └── repository/             # Offline-first SWR repository & RequestCoalescer
    │       └── ui/
    │           ├── components/             # MobileChrome (Top/Bottom bar), AnimeCards, AhIcons
    │           ├── navigation/             # NavHost routes, ViewModel factories, bottom-bar gating
    │           ├── screens/                # Home, Catalog, Bookmarks, Detail, Search, Player, Settings
    │           └── theme/                  # Static cached palettes & Typography
    └── test/java/me/animeheaven/android/   # 8 JVM unit test suites (no device required)
```

---

## Documentation Index

- **[Architecture & Mobile UI Guide](docs/ARCHITECTURE.md)** — Startup wiring, edge-to-edge & PiP activity hooks, navigation routes, ViewModels, adaptive grids, IME search, and player touch gestures.
- **[Video Playback, Caching & Preloading](docs/VIDEO_AND_CACHING.md)** — 3-tier caching, Room schema, streaming JSON snapshot codec, `VideoCacheManager`, `StreamingChunkBooster`, `MirrorHealthTracker`, and 3-band `AnimePreloader`.
- **[Performance, Build & Contributor Guide](docs/PERFORMANCE_AND_BUILD.md)** — Compose skippability rules, R8 full-mode configuration, unit tests, and invariants to preserve when making changes.
