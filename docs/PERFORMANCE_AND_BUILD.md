# Performance, Build & Contributor Guide

This guide covers Compose compiler stability, R8 full-mode shrinking, JVM unit testing, verified mobile build metrics, and invariants to preserve during future work in `animeheaven-android`.

---

## 1. Compose Compiler & Runtime Performance

Configuration in [`app/build.gradle.kts`](../app/build.gradle.kts):
- **Compose BOM**: `2025.02.00` (Strong Skipping + Intrinsic Remember enabled in Kotlin `2.0.21`)
- **`OptimizeNonSkippingGroups`**: Enabled via `composeCompiler.featureFlags`
- **Stability Config**: [`app/compose_stability.conf`](../app/compose_stability.conf) marks `AppContainer`, `AppSettingsStore`, `AnimeHeavenRepository`, `VideoCacheManager`, `AnimePreloader`, `SkipTimesClient`, all 6 screen `ViewModel`s, `OkHttpClient`, and `ImageLoader` as reference-stable.
- **Model Annotations**: UI models, Room display entities, skip timestamps, `AppSettings`, and `AhThemePalette` carry `@Immutable`.

### Recomposition & Scroll Hygiene Patterns
1. **Isolate ticking state to leaves**:
   - In [`PlayerScreen.kt`](../app/src/main/java/me/animeheaven/android/ui/screens/player/PlayerScreen.kt), `PlaybackProgressState` updates every `500 ms`; only `PlayerInteractiveSeekSection` (Canvas track) and time readouts observe it so fullscreen HUD/video playback stays free of root recompositions.
   - `SkipPromptOverlay` animates countdown progress via `Animatable` + `graphicsLayer { scaleX = ... }`.
2. **Avoid `String.format` in hot loops**: Use [`formatDurationMs`](../app/src/main/java/me/animeheaven/android/ui/components/AnimeCards.kt) (`buildString`).
3. **Keep `@OptIn(ExperimentalFoundationApi::class)`** on composables using `LazyListPrefetchStrategy(nestedPrefetchItemCount = ...)` (required in Compose BOM `2025.02.00`).
4. **Precomputed Grid Rows**: Screens render adaptive grid rows via `toGridRows` + stable keys instead of nested grids.
5. **Lifecycle-gated Auto-Rotation**: `HeroBannerPager` pauses auto-advance whenever the lifecycle drops below `Lifecycle.State.RESUMED`.

### Verifying Compose Metrics
```bash
./gradlew assembleRelease -PcomposeCompilerReports=true
```
Verified baseline on `app_release`:
- **37 / 37 named `@Composable` functions** reported `skippable=1, restartable=1` in `app_release-composables.csv` (**0 non-skippable composables**).
- **3,004 / 3,060 (98.2%) known stable Compose arguments** in `app_release-module.json`.
- **0 UI models, UI states, or ViewModels unstable** in `app_release-classes.txt`.

---

## 2. Build Optimization & R8 Full Mode

### Gradle Flags ([`gradle.properties`](../gradle.properties))
- `org.gradle.parallel=true`, `org.gradle.caching=true`, `org.gradle.configuration-cache=true`
- `android.nonTransitiveRClass=true`
- `android.enableR8.fullMode=true`

### Verified Release Output (`assembleRelease`)
- **Release APK (`app/build/outputs/apk/release/app-release.apk`)**: `2,433,574 B` (`2.32 MB`)
- **`classes.dex`**: `4,167,692 B` raw / `2,132,179 B` compressed
- **Compiled Baseline Profile (`assets/dexopt/baseline.prof`)**: `9,892 B`

### APK Size Controls ([`app/build.gradle.kts`](../app/build.gradle.kts), [`proguard-rules.pro`](../app/proguard-rules.pro))
1. **Lean Iconography ([`AhIcons.kt`](../app/src/main/java/me/animeheaven/android/ui/components/AhIcons.kt))**: Uses `material-icons-core` + 34 extracted Material Rounded `ImageVector`s instead of `material-icons-extended`.
2. **Strict ProGuard Rules**: `-repackageclasses 'o'`, `-allowaccessmodification`, `-optimizationpasses 5`, stripping `Log.v`/`d`/`i`, and relying on Media3/Coil/OkHttp consumer rules instead of broad wildcard keeps.
3. **Resource & Packaging Trimming**: `resourceConfigurations += listOf("en")`, plus stripping `DebugProbesKt.bin`, `kotlin-tooling-metadata.json`, `*.kotlin_module`, and duplicate licenses.
4. **Precision Baseline Profile ([`app/src/main/baseline-prof.txt`](../app/src/main/baseline-prof.txt))**: Lists hot startup, UI component, OkHttp/Jsoup, Repository, Codec, and ExoPlayer classes.

> **Release Signing Note**: `buildTypes.release` in [`app/build.gradle.kts`](../app/build.gradle.kts) currently uses `signingConfigs.getByName("debug")` for sideload convenience. Configure a production release keystore before Play Store / public distribution.

---

## 3. Unit Testing

All JVM unit tests run locally without an emulator or network access:

```bash
./gradlew testDebugUnitTest
```

Verified result: **42 / 42 passed** (`0` failures, `0` errors, `0` skipped):

| Test Class | Count | Coverage |
| :--- | :---: | :--- |
| `CacheAndMirrorHealthTest` | 15 | `MirrorHealthTracker` EWMA ranking, stub demotion, shared `ep:<key>:<len>` key, MP4 `moov` detection, `LruCacheMap` |
| `CacheSnapshotCodecTest` | 6 | `StreamingJsonReader` round-trips, unicode escapes, forward-compatible fields |
| `AnimeHeavenParserTest` | 6 | Home, FastSearch, Detail, Episode gate, URL/ID extraction |
| `WatchProgressLogicTest` | 5 | Resume thresholds, `>= 85%` watched, `>= 92%` reset, Continue Watching progression |
| `SkipPromptLogicTest` | 5 | OP/ED/fallback/next-episode skip evaluation windows |
| `MirrorFailoverTest` | 3 | `MirrorQueueState` ordering, rotation, exhaustion |
| `RepositoryOptimizationTest` | 1 | `RequestCoalescer` deduplication & cancellation safety |
| `SearchViewModelTest` | 1 | Instant & debounced IME search state transitions via fake `SearchGateway` |

---

## 4. Invariants Not to Break

- **Keep `applicationId = "me.animeheaven.android"`** distinct from `me.animeheaven.tv` so both apps can stay installed side-by-side.
- **Do NOT add `material-icons-extended`** — extract needed icon vectors into `AhIcons.kt`.
- **Do NOT enable OkHttp disk caching on Coil's fetcher** — keep `okHttpClient.newBuilder().cache(null).build()` in `AppContainer`.
- **Keep `ImageTargetSize` aligned** between `AnimePreloader` and UI cards (`AhCoverImage`).
- **Preserve PiP safety in `PlayerScreen`** — always gate touch overlays, HUD, and `ON_STOP` pauses with `!isInPipMode` and restore `SCREEN_ORIENTATION_UNSPECIFIED` when disposing the player.
- **Observe `themeId` separately at the root** (`MainActivity`) so changing player/preload preferences never recomposes the app root.
