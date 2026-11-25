# Video Playback, Caching & Preloading Engine

This document covers the networking, HTML parsing, multi-tier caching, Wi-Fi/cellular predictive preloading, and adaptive multi-mirror Media3 playback stack in `animeheaven-android`.

---

## 1. Networking & HTML Parsing

### OkHttp & Connection Reuse ([`AnimeHeavenClient.kt`](../app/src/main/java/me/animeheaven/android/data/network/AnimeHeavenClient.kt))
- Base URL: `https://animeheaven.me`
- Dispatcher limits: `maxRequests = 32`, `maxRequestsPerHost = 12`
- ConnectionPool: `12` idle connections, `5 min` keep-alive
- HTML/API caching is owned by Room and bounded in-memory caches; OkHttp has no on-disk HTTP cache. Coil owns its image cache separately.
- Cancellable requests via `OkHttpClient.awaitResponse(request)` using `suspendCancellableCoroutine` + `cont.invokeOnCancellation { call.cancel() }`, cancelling underlying sockets immediately on rapid typing or back navigation.
- [`CachedDns`](../app/src/main/java/me/animeheaven/android/data/network/NetworkOptimizer.kt): 5-minute in-memory DNS TTL cache with stale IP fallback on `UnknownHostException`.

### Network Policy & Preconnection ([`NetworkOptimizer.kt`](../app/src/main/java/me/animeheaven/android/data/network/NetworkOptimizer.kt))
- `NetworkPolicy` tracks unmetered Wi-Fi vs. metered cellular connections reactively via `ConnectivityManager.NetworkCallback` and `isUnmeteredFlow: StateFlow<Boolean>`, strictly enforcing `DATA_SAVER` (`0 B` background video preloading) on metered cellular data and resuming 4-wave preloading automatically when Wi-Fi returns.
- `NetworkPreconnector` runs async head/DNS warming for AnimeHeaven and learned top-3 healthy video CDN hosts from `MirrorHealthTracker`.

### HTML Parsing ([`AnimeHeavenParser.kt`](../app/src/main/java/me/animeheaven/android/data/parser/AnimeHeavenParser.kt))
All CSS selectors are compiled **once** as Jsoup `Evaluator`s (`QueryParser.parse(...)`). Parsers handle:
- Home feed (`index.php` -> banners, schedule, popular strip, current season path/label)
- Catalog pages (`new.php`, `popular.php`, seasonal `*.php`, `tags.php?tag=...`)
- Fast search (`fastsearch.php?xhr=1&s=...`) & full search (`search.php?s=...`)
- Anime detail (`anime.php?...` -> metadata, tags, episodes, related/similar cards)
- Episode mirror gate extraction (`gate.php` -> episode stream & `<source>` mirror URLs)

---

## 2. Three-Tier Data Caching & Persistence

### Tier 1 — Bounded In-Memory LRU ([`LruCacheMap.kt`](../app/src/main/java/me/animeheaven/android/data/cache/LruCacheMap.kt), [`AnimeHeavenRepository.kt`](../app/src/main/java/me/animeheaven/android/data/repository/AnimeHeavenRepository.kt))
Synchronized access-ordered `LinkedHashMap` caches bound heap usage:
- `cachedCharts`: max `24`
- `cachedGrids`: max `24`
- `cachedDetails`: max `128`
- `cachedStreams`: max `128`
- `cachedFastSearch`: max `100`
- In-memory watch-progress maps (`progressByEpisode`, `latestProgressByAnime`) hydrated at startup alongside recent `gate:*` streams (`48`), `detail:*` entries (`24`), and `vmeta:*` MP4 seek indices (`128`) so warm episodes open in 0 ms without DB or network blocking.

Soft TTLs drive Stale-While-Revalidate (SWR) background refreshes:
- `TTL_HOME_MS` = `5 min`
- `TTL_CHART_MS` = `5 min`
- `TTL_SEASONAL_MS` = `30 min`
- `TTL_SEARCH_MS` = `30 min`
- `TTL_DETAIL_MS` = `15 min`
- `TTL_GATE_MS` = `15 min`

### Request Coalescing (`RequestCoalescer`)
Concurrent requests for the same key (e.g., foreground detail load + background preloader) share a single `Deferred<String>` running in `repositoryScope` (`SupervisorJob + Dispatchers.IO`), so cancelling a background caller never aborts the foreground fetch.

### Tier 2 — Fast Home Snapshot ([`CacheSnapshotCodec.kt`](../app/src/main/java/me/animeheaven/android/data/cache/CacheSnapshotCodec.kt))
- Home feed persists atomically as `home_snapshot.v1.json` in `filesDir`.
- **Zero-AST Streaming Reader**: `StreamingJsonReader` reads JSON tokens directly into `HomeFeed` models without allocating intermediate `Map`/`List` AST objects.
- When `web_cache.parsedJson` is present, `AnimeHeavenRepository` skips redundant GZIP+Base64 HTML payload writes and persists off the caller path.

### Tier 3 — Room Database v3 ([`AppDatabase.kt`](../app/src/main/java/me/animeheaven/android/data/local/AppDatabase.kt))
Database file: `animeheaven_tv.db` (kept compatible in name/schema v3), opened with `JournalMode.WRITE_AHEAD_LOGGING` and `PRAGMA synchronous = NORMAL`, `temp_store = MEMORY`, `cache_size = -4000`.

| Table | Purpose | Key Indexes / Notes |
| :--- | :--- | :--- |
| `bookmarks` | User saved anime (`addedAt DESC`) | PrimaryKey `animeId` |
| `watch_progress` | Per-episode position, duration, `isWatched`, `nextEpisodeKey` | Indexed on `(animeId, updatedAt)` and `(updatedAt)`. `observeLatestPerAnime()` returns 1 row per anime (`LIMIT 30`). |
| `search_history` | Recent search queries (`LIMIT 10`) | PrimaryKey `query` |
| `web_cache` | Parsed JSON + optional compressed HTML (`MAX_DISK_RETENTION_MS = 14 days`) | Indexed on `fetchedAt`; stores page caches and AniSkip/AniList responses |

Migrations `MIGRATION_1_2` and `MIGRATION_2_3` preserve local progress and bookmarks cleanly.

### Watch Progress Thresholds (`AnimeHeavenRepository.kt`)
- `MIN_RESUME_THRESHOLD_MS` = `10,000 ms`
- `WATCHED_RATIO` = `0.85f` (episodes >= 85% watched promote Continue Watching to `nextEpisodeKey`)
- `MAX_RESUME_RATIO` = `0.92f` (re-opening past 92% restarts from `0 ms`)

---

## 3. Image Preloading & Memory Cache Parity

Coil 3 keys `MemoryCache` entries by `(url, widthPx, heightPx)`. Preloading and composables (`AhCoverImage`) therefore use identical canonical dimensions from [`ImageTargetSize`](../app/src/main/java/me/animeheaven/android/data/cache/ImageTargetSize.kt):
- `HERO_BANNER`: `1080 × 460`
- `DETAIL_POSTER`: `360 × 540`
- `CONTINUE_CARD`: `480 × 270`
- `POPULAR_CARD`: `320 × 320`
- `STANDARD_CARD`: `420 × 236`
- `SEARCH_THUMB`: `256 × 144`

Coil memory allocation in `AppContainer`: `18%` of memory class on low-RAM devices (`isLowRamDevice` or `memoryClass <= 192 MB`), `25%` on standard phones/tablets, plus a `250 MB` `image_cache` disk cache and `RGB_565` bitmap decoding.

---

## 4. 4-Wave Startup & Interactive Preloading ([`AnimePreloader.kt`](../app/src/main/java/me/animeheaven/android/data/cache/AnimePreloader.kt))

Preloading starts immediately on app launch (zero startup delay on unmetered Wi-Fi, strictly disabled on metered cellular) across four waves with `originHtmlGate(2)`, `150 ms` Wave-2 pacing, and automatic HTTP `429`/`503` exponential backoff:
- **Wave 0 (`0 ms`)**: Top 3 Continue Watching shows (`8 MB` + pre-demuxed `PreloadMediaSource` for #1, `6 MB` for #2–#3) + Hero Banner #1 (`6 MB`), targeting exact resume offsets or opening + post-OP.
- **Wave 1 (`+350 ms`)**: Continue Watching #4–#15 (`5 MB` each), Hero Banners #2–#5 (`5 MB` each), and Bookmarks #1–#15 (`4.5 MB` each).
- **Wave 2 (Paced)**: Every show in Home Popular (`12`) and Home Schedule (`24`) warmed with `4 MB` (~15–20s) of start/resume bytes, plus background catalog tabs (`New`, `Popular`, seasonal feed).
- **Wave 3 (Interactive & Watch Next)**: Detail primary episode (`8 MB` + follow-up `4 MB`), visible catalog scroll warming, and Watch Next (`6 MB` on playback start, escalating to `20 MB` once the active episode has `>= 15s` buffered ahead).

Foreground work calls `beginUserPriorityWork()` / `endUserPriorityWork()` around `foregroundUserWorkCount: MutableStateFlow<Int>`, pausing background waves reactively via `yieldIfUserActive()`.

---

## 5. Zero-Buffering Video Engine, MP4 Seek Index & Gapless Watch Next

### Exact MP4 Keyframe Indexing ([`Mp4MoovSeekParser.kt`](../app/src/main/java/me/animeheaven/android/data/cache/Mp4MoovSeekParser.kt))
- Pure-Kotlin ISO-BMFF parser inspects `ftyp`/`moov`/`mdat` boxes to locate faststart or multi-chunk tail `moov` atoms and extracts `EpisodeVideoIndex` keyframe tables (`mvhd`/`mdhd`/`hdlr`/`stts`/`stss`/`stsc`/`stsz`/`stco`/`co64`), persisted in Room `web_cache` (`"vmeta:<episodeKey>"`, zero schema migration).
- Enables exact VBR keyframe resume & post-OP byte targeting plus 0 ms cold-start cache key lookup.

### Tiered 2 GB Cache & Eviction Protection ([`PinnedPriorityCacheEvictor.kt`](../app/src/main/java/me/animeheaven/android/data/cache/PinnedPriorityCacheEvictor.kt), [`VideoCacheManager.kt`](../app/src/main/java/me/animeheaven/android/data/cache/VideoCacheManager.kt))
- Shared episode cache keys (`"ep:$episodeKey:$contentLength"`) reuse cached chunks across verified mirrors at **0 ms**.
- Disk budget scales up to `2 GB` (`512 MB` `< 4 GB` free, `1 GB` `< 12 GB` free, `2 GB` `>= 12 GB` free, capped at `<= 25%` of usable storage).
- `PinnedPriorityCacheEvictor` protects **Tier A** hot-start, Continue Watching resume, `moov`, and Watch Next windows (up to `40%` budget) and evicts **Tier B** watched trailing chunks first (`> 3 min` behind playhead auto-demoted).

### First-Valid-Mirror Probe & `StreamingChunkBooster` ([`StreamingChunkBooster.kt`](../app/src/main/java/me/animeheaven/android/data/cache/StreamingChunkBooster.kt))
- Warm disk hits bypass blocking mirror probes completely (`PlayerViewModel.buildWarmStateIfAvailable`) and start on Frame 0 while `probeMirrorsInBackground()` refreshes mirror stats asynchronously.
- Cold starts race `bytes=0-0` probes (`maxWaitMs = 900L`) and unblock on the first valid full-length (`>= 1 MB`) mirror.
- `StreamingChunkBooster` maintains a **48 MB** sliding ahead window (`24 MB` low-RAM) from the exact playhead frontier (`alignChunkStart(estByte)`), `SINGLE_HOST_WORKERS = 3`, instant `Channel.CONFLATED` seek wakeups, and post-OP priority.

### `PlayerWarmupPool` & Gapless ExoPlayer Playlist ([`PlayerWarmupPool.kt`](../app/src/main/java/me/animeheaven/android/data/cache/PlayerWarmupPool.kt))
- Shares a playback `HandlerThread` (`playbackLooper`) and uses Media3 `PreloadMediaSource.Factory` to pre-demux 6s of RAM samples (max 2 entries, zero extra `MediaCodec` decoders).
- `PlayerScreen` binds ExoPlayer to `warmupPool.playbackLooper`, enables `pauseAtEndOfMediaItems = true` + `ExoPlayer.PreloadConfiguration(15_000_000L)`, appends the pre-warmed Watch Next item into the live playlist, and performs **0 ms instant switches** via `seekToNextMediaItem()`.
- `buildFastLoadControl()`: `200 ms` start buffer, `500 ms` rebuffer threshold, `20s–30s` low-RAM / `40s–60s` standard buffers, plus a `220 ms` anti-flicker spinner debounce when `isDiskCacheWarm`.

---

## 6. Skip OP / ED Pipeline ([`SkipTimesClient.kt`](../app/src/main/java/me/animeheaven/android/data/network/SkipTimesClient.kt), [`SkipPromptLogic.kt`](../app/src/main/java/me/animeheaven/android/ui/screens/player/SkipPromptLogic.kt))

1. Parses episode integer via `SkipTimesParser.parseEpisodeNumber`.
2. Resolves anime title -> MyAnimeList ID via AniList GraphQL (`https://graphql.anilist.co`), cached in memory + Room `web_cache` (`30 days`).
3. Fetches `https://api.aniskip.com/v2/skip-times/{malId}/{episodeNum}?types=op&types=ed&episodeLength=0`, cached in `web_cache` (`7 days`).
4. `SkipPromptLogic` drives contextual `Skip OP`, `Skip ED`, fallback OP (`skipOpSeconds`), and `Next Episode` prompts inside the 500 ms player poll loop.
