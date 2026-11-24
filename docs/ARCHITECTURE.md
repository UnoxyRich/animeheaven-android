# Architecture & Mobile UI Guide

This document describes the runtime wiring, state flow, navigation, Picture-in-Picture support, and mobile touch UI subsystems in `animeheaven-android`.

---

## 1. Startup & Dependency Injection

`animeheaven-android` uses a lightweight manual container ([`AppContainer.kt`](../app/src/main/java/me/animeheaven/android/AppContainer.kt)) instead of a reflection-based DI framework so cold start stays minimal.

### Startup Sequence
1. [`AnimeHeavenApplication.onCreate()`](../app/src/main/java/me/animeheaven/android/AnimeHeavenApplication.kt) builds `AppContainer` and triggers `container.warmUpOnStartup()` onto an `IO` `CoroutineScope`.
2. **Lazy Heavy Subsystems**: `okHttpClient`, `client`, `database`, `networkPolicy`, `repository`, `skipTimesClient`, `videoCacheManager`, `imageLoader`, and `preloader` are initialized via `lazy(LazyThreadSafetyMode.SYNCHRONIZED)`. Only lightweight `AppSettingsStore` reads synchronously during container construction.
3. **Async Warm-Up (`warmUpOnStartup`)**:
   - Preconnects TLS sockets asynchronously via [`NetworkPreconnector.preconnectAsync`](../app/src/main/java/me/animeheaven/android/data/network/NetworkOptimizer.kt).
   - Calls `repository.hydrateHomeFastSync()`, parsing `home_snapshot.v1.json` via zero-AST streaming JSON so the Home feed hydrates immediately before Room finishes opening.
   - Preloads the first 2 `HERO_BANNER` images and first 6 `POPULAR_CARD` covers at exact composable target dimensions.
   - Opens Room via `repository.warmUpFromDisk()` and schedules `preloadHomeEcosystem(...)`.
   - Delays `800 ms` after launch to initialize ExoPlayer's `SimpleCache` & `MirrorHealthTracker` off the main thread after Frame 1 renders.
   - Delays `9,000 ms` before pruning expired `web_cache` rows (> 14 days).
4. **Memory Pressure Handling**: [`AnimeHeavenApplication.onTrimMemory()`](../app/src/main/java/me/animeheaven/android/AnimeHeavenApplication.kt) forwards trim events to `AppContainer.onTrimMemory()`, trimming Coil's memory cache (half at `TRIM_MEMORY_RUNNING_LOW`, clear at `RUNNING_CRITICAL`/`UI_HIDDEN`) and clearing LRU maps / booster buffers when necessary.

### Edge-to-Edge, Theme Scoping & PiP in `MainActivity`
[`MainActivity.kt`](../app/src/main/java/me/animeheaven/android/MainActivity.kt):
- Calls `enableEdgeToEdge(...)` and updates light/dark system bar icons reactively when `palette.isLight` changes (`LIGHT` vs dark themes).
- Observes **only** `container.settingsStore.themeId.collectAsStateWithLifecycle()` at the root so toggling non-theme settings never recomposes the root navigation tree.
- Clears the redundant window background (`window.setBackgroundDrawable(null)`) because `AnimeHeavenTheme` paints the full-screen canvas.
- Implements **Picture-in-Picture (API 26+)**:
  - `pipEligibilityCheck` is registered/cleared dynamically by `PlayerScreen` (`exoPlayer.isPlaying || exoPlayer.playWhenReady`).
  - `onUserLeaveHint()` automatically invokes `tryEnterPipMode()` (`16:9` aspect ratio `PictureInPictureParams`) when playback is active.
  - `onPictureInPictureModeChanged(...)` updates `isInPipMode: StateFlow<Boolean>`, hiding HUD chrome, touch overlays, and bottom bars cleanly while in PiP.

---

## 2. Data & UI State Flow

```text
  AnimeHeavenClient (OkHttp + CachedDns)
           │
           ▼
  AnimeHeavenParser (Pre-compiled Jsoup Evaluators)
           │
           ▼
  AnimeHeavenRepository (RequestCoalescer + LruCacheMap + Stale-While-Revalidate)
     ├── Fast Home Snapshot: home_snapshot.v1.json (CacheSnapshotCodec)
     └── Durable Cache/User Data: AppDatabase (Room v3, WAL)
           │
           ▼
  ViewModels (Home / Catalog / Bookmarks / Detail / Search / Player)
     └── Flow.combine(..., SharingStarted.WhileSubscribed(5_000L)) ──▶ @Immutable UiState
           │
           ▼
  Jetpack Compose Mobile Screens (Edge-to-edge + PullToRefresh + Touch Player)
```

### Why `SharingStarted.WhileSubscribed(5_000L)` Matters
Screen ViewModels (`HomeViewModel`, `CatalogViewModel`, `BookmarksViewModel`, `DetailViewModel`) automatically pause Room collection `5s` after navigating away or entering fullscreen `PlayerScreen`, eliminating redundant background Coil and stream preloading during playback.

---

## 3. Navigation Graph & Chrome

Routes are defined in [`AppNavGraph.kt`](../app/src/main/java/me/animeheaven/android/ui/navigation/AppNavGraph.kt):

| Route | Screen | Bottom Bar Visible? | Notes |
| :--- | :--- | :---: | :--- |
| `home` | `HomeScreen` | Yes (`Schedule`) | Pull-to-refresh + Hero pager + Continue Watching + Popular + Schedule |
| `new` | `CatalogScreen` | Yes (`New`) | `CatalogKind.NEW` (`new.php`) with pull-to-refresh |
| `popular` | `CatalogScreen` | Yes (`Popular`) | `CatalogKind.POPULAR` (`popular.php`) with pull-to-refresh |
| `bookmarks` | `BookmarksScreen` | Yes (`Bookmarks`) | Saved bookmarks + Continue Watching + Clear Watch History |
| `seasonal/{path}/{label}` | `CatalogScreen` | No | URI-encoded season path & label from top-bar season pill |
| `tag/{tag}` | `CatalogScreen` | No | URI-encoded tag catalog |
| `search` | `SearchScreen` | No | Native IME search + recent queries + live fast/full grid results |
| `detail/{animeId}` | `AnimeDetailScreen` | No | Synopsis, tags, bookmark toggle, episode tiles, related/similar shows |
| `player/{episodeKey}/{animeId}/{animeTitle}/{coverUrl}` | `PlayerScreen` | No | Fullscreen / PiP video player; blank placeholders encoded as `"_"` / `"AnimeHeaven"` |
| `settings` | `SettingsScreen` | No | Themes, view mode, player/gesture/orientation, skip/preload controls, cache stats/clear |

Top-level tabs navigate via `popUpTo(Routes.HOME) { saveState = true }`, `launchSingleTop = true`, and `restoreState = true`.

---

## 4. Mobile & Tablet UI Details

### Manifest Configuration ([`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml))
- Standard phone/tablet `LAUNCHER` entry without Leanback TV requirements.
- `android:resizeableActivity="true"` and `android:supportsPictureInPicture="true"`.
- `android:windowSoftInputMode="adjustResize"` so Search results resize cleanly above the soft keyboard.
- `android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboard|keyboardHidden|navigation"` to allow seamless orientation transitions and PiP without recreating `MainActivity` or tearing down `ExoPlayer`.

### Mobile Chrome & Adaptive Grids
- [`MobileChrome.kt`](../app/src/main/java/me/animeheaven/android/ui/components/MobileChrome.kt) provides `MobileTopBar` (status-bar inset-aware header with AH emblem refresh, seasonal chip, Search, Random, Settings) and `MobileBottomBar` (`Schedule`, `New`, `Popular`, `Bookmarks` with navigation-bar inset padding).
- Feed screens use `PullToRefreshBox` (Material 3) and `BoxWithConstraints` (`toGridRows`) to adapt grid column counts dynamically across portrait phones, landscape phones, and tablets.

### Native IME Search ([`SearchScreen.kt`](../app/src/main/java/me/animeheaven/android/ui/screens/search/SearchScreen.kt))
- Uses `BasicTextField` + `FocusRequester` + `LocalSoftwareKeyboardController` (`ImeAction.Search`) + `imePadding()` instead of a custom TV keyboard.
- Displays recent search history chips when idle, live `fastsearch.php` suggestions while typing, and full grid results on IME submit.

### Mobile Player Gestures & Orientation ([`PlayerScreen.kt`](../app/src/main/java/me/animeheaven/android/ui/screens/player/PlayerScreen.kt))
- **Orientation & Immersive Bars**: Enters `SCREEN_ORIENTATION_SENSOR_LANDSCAPE` automatically when `settings.autoLandscapePlayer == true` (with a manual HUD orientation toggle), hides system bars with `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`, sets `FLAG_KEEP_SCREEN_ON`, and restores `SCREEN_ORIENTATION_UNSPECIFIED` + system bars on dispose.
- **Tap & Double-Tap (`detectTapGestures`)**:
  - Single tap: toggles HUD visibility (or flashes the unlock hint when locked).
  - Double-tap `< 38%` screen width: seeks `-doubleTapSeekSeconds`; `> 62%`: seeks `+doubleTapSeekSeconds`; center zone: toggles play/pause.
- **Vertical Drag (`detectVerticalDragGestures`)**:
  - Left half: adjusts `activity.window.attributes.screenBrightness` (`3%`–`100%`) with an on-screen percentage indicator.
  - Right half: adjusts `AudioManager.STREAM_MUSIC` volume (`0%`–`100%`).
- **Interactive Scrubber**: `PlayerInteractiveSeekSection` renders buffered/played tracks in `Canvas` and supports both tap-to-seek and horizontal drag scrubbing (`detectHorizontalDragGestures`).
- **HUD Controls & Screen Lock**: Includes Play/Pause, Play Speed (`0.75x`, `1.0x`, `1.25x`, `1.5x`, `2.0x`), Resize Mode (`Fit`, `Fill`, `Zoom`), Mirror switcher, Skip OP/ED chip, PiP button, Orientation toggle, and Gesture Lock button. All touch overlays hide when `isInPipMode == true`.
- **Recomposition Isolation**: Polling updates an isolated `@Stable PlaybackProgressState` every `500 ms` inside leaf HUD composables, while `SkipPromptOverlay` animates countdown progress via `Animatable` + `graphicsLayer { scaleX = ... }`.

---

## 5. User Settings (`AppSettingsStore.kt`)

Persisted in `SharedPreferences` (`animeheaven_android_settings`):

| Setting | Default | Purpose |
| :--- | :--- | :--- |
| `themeId` | `AhThemeId.DARK` | Active palette (`dark`, `dark2`..`dark7`, `light`) |
| `defaultViewMode` | `FeedViewMode.LIST` | Default layout for Home/Catalog grids (`LIST` / `GRID`) |
| `autoPlayNext` | `true` | Shows 5-second Up Next countdown at episode end |
| `skipOpSeconds` | `85` | Fallback OP skip jump when AniSkip timestamps are unavailable |
| `skipPromptEnabled` | `true` | Enables contextual `Skip OP` / `Skip ED` / `Next Ep` overlay prompts |
| `doubleTapSeekSeconds` | `10` | Double-tap left/right seek step (`5s` / `10s` / `15s`) |
| `autoLandscapePlayer` | `true` | Automatically switches player to sensor-landscape on open |
| `preloadMode` | `PreloadMode.AGGRESSIVE` | Controls background preloading (`AGGRESSIVE` Wi-Fi / `BALANCED` / `DATA_SAVER`) |
