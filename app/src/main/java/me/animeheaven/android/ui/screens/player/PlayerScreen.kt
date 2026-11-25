package me.animeheaven.android.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.media.AudioManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import me.animeheaven.android.MainActivity
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import me.animeheaven.android.data.cache.VideoCacheManager
import me.animeheaven.android.data.network.SkipSegment
import me.animeheaven.android.data.network.SkipSegmentType
import me.animeheaven.android.ui.components.AhErrorBox
import me.animeheaven.android.ui.components.AhIcons
import me.animeheaven.android.ui.components.AhShape10
import me.animeheaven.android.ui.components.AhShape20
import me.animeheaven.android.ui.components.AhShape4
import me.animeheaven.android.ui.components.AhShape8
import me.animeheaven.android.ui.components.formatDurationMs
import me.animeheaven.android.ui.theme.LocalAhPalette

@OptIn(UnstableApi::class)
private enum class ResizeOption(val label: String, val mode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
}

private enum class GestureType { BRIGHTNESS, VOLUME }

private data class GestureIndicator(
    val type: GestureType,
    val percent: Int
)

private val PLAYBACK_SPEEDS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
private val HUD_TOP_SCRIM = Brush.verticalGradient(listOf(Color(0xE6000000), Color.Transparent))
private val HUD_BOTTOM_SCRIM = Brush.verticalGradient(listOf(Color.Transparent, Color(0xEC000000)))
private val HUD_TEXT_SECONDARY = Color(0xFFD2D2D2)
private val HUD_SECONDARY_BUTTON_BG = Color(0xFF262626)
private val SEEK_BADGE_SHAPE = RoundedCornerShape(8.dp)
private val SEEK_TRACK_BG = Color(0x55FFFFFF)
private val SEEK_BUFFER_COLOR = Color(0x77BDBDBD)
private val SKIP_PROMPT_TRANSFORM_ORIGIN = TransformOrigin(0f, 0.5f)

@Stable
private class PlaybackProgressState {
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)
    var bufferedMs by mutableLongStateOf(0L)
}

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    videoCacheManager: VideoCacheManager,
    isInPipMode: Boolean,
    onRegisterPipEligibility: ((() -> Boolean)?) -> Unit,
    onRequestPip: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsStore.settings.collectAsStateWithLifecycle()
    val palette = LocalAhPalette.current

    val context = LocalContext.current
    val activity = context as? Activity
    val pipActivity = activity as? MainActivity
    val view = LocalView.current
    val compactHud = LocalConfiguration.current.screenWidthDp < 550
    val lifecycleOwner = LocalLifecycleOwner.current

    val progressState = remember { PlaybackProgressState() }

    var isPlaying by remember { mutableStateOf(false) }
    var wantsPlayback by remember { mutableStateOf(false) }
    var pipSource by remember { mutableStateOf<Rect?>(null) }
    var isBuffering by remember { mutableStateOf(true) }

    var hudVisible by remember { mutableStateOf(true) }
    var hudInteractionNonce by remember { mutableIntStateOf(0) }
    var isLocked by rememberSaveable { mutableStateOf(false) }
    var showUnlockHintNonce by remember { mutableIntStateOf(0) }

    var seekFeedback by remember { mutableStateOf<String?>(null) }
    var seekFeedbackNonce by remember { mutableIntStateOf(0) }
    var gestureIndicator by remember { mutableStateOf<GestureIndicator?>(null) }

    var resizeOption by rememberSaveable { mutableStateOf(ResizeOption.FIT) }
    var playbackSpeed by rememberSaveable { mutableFloatStateOf(1.0f) }
    var isLandscape by rememberSaveable { mutableStateOf(settings.autoLandscapePlayer) }

    var upNextSecondsLeft by remember { mutableIntStateOf(0) }
    var showUpNextOverlay by remember { mutableStateOf(false) }

    var activeSkipSegment by remember { mutableStateOf<SkipSegment?>(null) }
    val handledSkipTypes = remember(state.stream?.episodeKey) { mutableSetOf<SkipSegmentType>() }

    val latestState by rememberUpdatedState(state)
    val latestSettings by rememberUpdatedState(settings)
    val latestHudVisible by rememberUpdatedState(hudVisible)
    val latestShowUpNext by rememberUpdatedState(showUpNextOverlay)
    val latestIsBuffering by rememberUpdatedState(isBuffering)
    val latestIsInPip by rememberUpdatedState(isInPipMode)
    val latestIsLocked by rememberUpdatedState(isLocked)

    val audioManager = remember(context) {
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    val exoPlayer = remember(context, videoCacheManager) {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .forceEnableMediaCodecAsynchronousQueueing()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setPlaybackLooper(videoCacheManager.warmupPool.playbackLooper)
            .setLoadControl(videoCacheManager.buildFastLoadControl())
            .setMediaSourceFactory(videoCacheManager.buildProgressiveMediaSourceFactory())
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                pauseAtEndOfMediaItems = true
                setPreloadConfiguration(ExoPlayer.PreloadConfiguration(15_000_000L))
            }
    }
    var resumeOnStart by remember(exoPlayer) { mutableStateOf(false) }

    var showBufferingSpinner by remember { mutableStateOf(state.isLoading && !state.isDiskCacheWarm) }
    LaunchedEffect(state.isLoading, isBuffering, state.errorMessage, state.isDiskCacheWarm) {
        val rawBusy = state.errorMessage == null && (state.isLoading || isBuffering)
        if (!rawBusy) {
            showBufferingSpinner = false
        } else if (state.isDiskCacheWarm && !state.isLoading) {
            delay(220L)
            showBufferingSpinner = true
        } else {
            showBufferingSpinner = true
        }
    }

    // Register PiP eligibility when playing and clean up on exit
    DisposableEffect(exoPlayer) {
        onRegisterPipEligibility { exoPlayer.isPlaying || exoPlayer.playWhenReady }
        onDispose {
            onRegisterPipEligibility(null)
        }
    }

    LaunchedEffect(pipActivity, wantsPlayback, isPlaying, pipSource, state.errorMessage, showUpNextOverlay) {
        pipActivity?.updatePlayerPip(wantsPlayback && state.errorMessage == null && !showUpNextOverlay,
            wantsPlayback, pipSource) { command ->
            if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@updatePlayerPip
            when (command) {
                "toggle" -> if (exoPlayer.playWhenReady) exoPlayer.pause() else exoPlayer.play()
                "back" -> exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                "forward" -> exoPlayer.seekTo((exoPlayer.currentPosition + 10_000L)
                    .coerceAtMost(exoPlayer.duration.takeIf { it > 0L } ?: Long.MAX_VALUE))
            }
        }
    }

    DisposableEffect(pipActivity, exoPlayer) {
        onDispose { pipActivity?.updatePlayerPip(false, false, null, null) }
    }

    DisposableEffect(view, activity) {
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(activity, isLandscape) {
        activity?.requestedOrientation = if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle(
        minActiveState = Lifecycle.State.CREATED
    )
    val keepAwake = lifecycleState.isAtLeast(Lifecycle.State.STARTED) &&
        state.errorMessage == null && (isPlaying || isBuffering && wantsPlayback)
    DisposableEffect(view, keepAwake) {
        view.keepScreenOn = keepAwake
        onDispose { view.keepScreenOn = false }
    }

    DisposableEffect(activity) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                window.attributes = window.attributes.apply {
                    screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
        }
    }

    fun saveCurrentProgress() {
        val item = exoPlayer.currentMediaItem ?: return
        if (item.mediaId != latestState.stream?.episodeKey) return
        viewModel.persistProgress(exoPlayer.currentPosition.coerceAtLeast(0L),
            exoPlayer.duration.coerceAtLeast(progressState.durationMs))
    }

    fun handleEpisodeEnded() {
        viewModel.setPlaybackBufferingActive(false)
        val finalDur = exoPlayer.duration.coerceAtLeast(progressState.durationMs)
        viewModel.persistProgress(
            positionMs = finalDur,
            durationMs = finalDur,
            forceWatched = true
        )
        val nextKey = latestState.stream?.nextEpisodeKey
        if (latestSettings.autoPlayNext && !nextKey.isNullOrBlank() && !latestIsInPip) {
            upNextSecondsLeft = 5
            showUpNextOverlay = true
            hudVisible = false
        } else {
            hudVisible = true
        }
    }

    fun switchToEpisodeInstant(nextKey: String) {
        showUpNextOverlay = false
        activeSkipSegment = null
        val curIndex = exoPlayer.currentMediaItemIndex
        val nextIndex = curIndex + 1
        val hasPreparedInPlaylist = nextIndex < exoPlayer.mediaItemCount &&
            exoPlayer.getMediaItemAt(nextIndex).mediaId == nextKey
        if (hasPreparedInPlaylist) {
            val promoted = viewModel.promotePreparedNextEpisode()
            if (promoted != null) {
                exoPlayer.seekTo(nextIndex, 0L)
                if (curIndex >= 0 && curIndex < nextIndex) {
                    exoPlayer.removeMediaItem(curIndex)
                }
                exoPlayer.setPlaybackSpeed(playbackSpeed)
                exoPlayer.playWhenReady = true
                exoPlayer.play()
                progressState.positionMs = 0L
                val dur = exoPlayer.duration.coerceAtLeast(promoted.durationMs)
                progressState.durationMs = dur
                progressState.bufferedMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING
                promoted.primaryUrl?.let { promotedUrl ->
                    viewModel.startStreamingBoost(
                        activeUrl = promotedUrl,
                        activeCacheKey = promoted.activeCacheKey,
                        initialPositionMs = 0L,
                        initialDurationMs = dur
                    )
                }
                viewModel.onPlaybackStarted()
                return
            }
        }
        viewModel.loadEpisode(nextKey)
    }

    DisposableEffect(exoPlayer) {
        val playerListener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                wantsPlayback = playWhenReady
                if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                    handleEpisodeEnded()
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing) {
                    saveCurrentProgress()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                when (playbackState) {
                    Player.STATE_BUFFERING -> viewModel.setPlaybackBufferingActive(true)
                    Player.STATE_READY -> {
                        val dur = exoPlayer.duration.coerceAtLeast(0L)
                        if (dur > 0L) progressState.durationMs = dur
                        viewModel.updateBoosterProgress(
                            exoPlayer.currentPosition.coerceAtLeast(0L),
                            progressState.durationMs
                        )
                        viewModel.onPlaybackStarted()
                    }
                    Player.STATE_IDLE -> viewModel.setPlaybackBufferingActive(false)
                    Player.STATE_ENDED -> handleEpisodeEnded()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                viewModel.onMirrorPlaybackError(exoPlayer.currentPosition.coerceAtLeast(0L))
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                val curDur = exoPlayer.duration.coerceAtLeast(progressState.durationMs)
                viewModel.updateBoosterProgress(
                    newPosition.positionMs.coerceAtLeast(0L),
                    curDur
                )
            }
        }
        exoPlayer.addListener(playerListener)
        onDispose {
            exoPlayer.removeListener(playerListener)
            saveCurrentProgress()
            viewModel.stopStreamingBoost()
            exoPlayer.release()
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                resumeOnStart = exoPlayer.playWhenReady
                viewModel.stopStreamingBoost()
                saveCurrentProgress()
                exoPlayer.pause()
            } else if (event == Lifecycle.Event.ON_START && resumeOnStart) {
                resumeOnStart = false
                val snapshot = latestState
                val url = snapshot.mirrorState.currentUrl
                if (url != null && snapshot.errorMessage == null &&
                    exoPlayer.currentMediaItem?.mediaId == snapshot.stream?.episodeKey
                ) {
                    exoPlayer.play()
                    viewModel.startStreamingBoost(url, snapshot.activeCacheKey ?: url,
                        exoPlayer.currentPosition, exoPlayer.duration)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose { lifecycleOwner.lifecycle.removeObserver(lifecycleObserver) }
    }

    // Load active mirror URL into ExoPlayer with shared episode cache key, PreloadMediaSource handoff & direct resume
    val activeUrl = state.mirrorState.currentUrl
    val activeCacheKey = state.activeCacheKey ?: activeUrl
    val episodeKey = state.stream?.episodeKey
    LaunchedEffect(exoPlayer, lifecycleOwner, activeUrl, episodeKey, state.errorMessage) {
        if (state.errorMessage != null || activeUrl.isNullOrBlank() || activeCacheKey.isNullOrBlank() || episodeKey == null) {
            exoPlayer.pause()
            exoPlayer.clearMediaItems()
        } else {
            videoCacheManager.cancelPreloadsExcept(setOfNotNull(episodeKey, state.stream?.nextEpisodeKey))
            val currentItem = exoPlayer.currentMediaItem
            if (currentItem?.mediaId == episodeKey &&
                currentItem.localConfiguration?.uri?.toString() == activeUrl
            ) {
                return@LaunchedEffect
            }
            showUpNextOverlay = false
            activeSkipSegment = null
            isBuffering = true
            val resumeMs = state.initialResumePositionMs.coerceAtLeast(0L)
            progressState.positionMs = resumeMs
            progressState.durationMs = state.initialDurationMs
            progressState.bufferedMs = resumeMs

            val preloadedSource = videoCacheManager.warmupPool.takePreparedSource(
                episodeKey = episodeKey,
                cacheKey = activeCacheKey,
                resumePositionMs = resumeMs
            )
            if (preloadedSource != null) {
                exoPlayer.setMediaSource(preloadedSource, resumeMs)
            } else {
                val mediaItem = MediaItem.Builder()
                    .setUri(activeUrl)
                    .setMediaId(episodeKey)
                    .setCustomCacheKey(activeCacheKey)
                    .build()
                exoPlayer.setMediaItem(mediaItem, resumeMs)
            }
            exoPlayer.prepare()
            exoPlayer.setPlaybackSpeed(playbackSpeed)
            val started = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            exoPlayer.playWhenReady = started
            resumeOnStart = !started
            if (started) viewModel.startStreamingBoost(
                activeUrl = activeUrl,
                activeCacheKey = activeCacheKey,
                initialPositionMs = resumeMs,
                initialDurationMs = state.initialDurationMs
            )
            hudVisible = true
            hudInteractionNonce++
        }
    }

    val preparedNext = state.preparedNextEpisode
    LaunchedEffect(exoPlayer, episodeKey, preparedNext?.episodeKey, preparedNext?.primaryUrl) {
        val curKey = episodeKey ?: return@LaunchedEffect
        val nextUrl = preparedNext?.primaryUrl ?: return@LaunchedEffect
        if (exoPlayer.currentMediaItem?.mediaId != curKey) return@LaunchedEffect
        val nextIndex = exoPlayer.currentMediaItemIndex + 1
        if (nextIndex < exoPlayer.mediaItemCount &&
            exoPlayer.getMediaItemAt(nextIndex).mediaId == preparedNext.episodeKey
        ) {
            return@LaunchedEffect
        }
        val nextMediaItem = MediaItem.Builder()
            .setUri(nextUrl)
            .setMediaId(preparedNext.episodeKey)
            .setCustomCacheKey(preparedNext.activeCacheKey)
            .build()
        if (nextIndex < exoPlayer.mediaItemCount) {
            exoPlayer.removeMediaItems(nextIndex, exoPlayer.mediaItemCount)
        }
        exoPlayer.addMediaItem(nextIndex, nextMediaItem)
    }

    // 500ms playback polling loop (0 root recompositions when HUD is hidden)
    LaunchedEffect(exoPlayer, episodeKey) {
        var tick = 0
        var continuousBufferingMs = 0L
        while (true) {
            delay(500L)
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration
            val resolvedDur = if (dur > 0L) dur else progressState.durationMs
            val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)

            progressState.positionMs = pos
            if (dur > 0L) progressState.durationMs = dur
            progressState.bufferedMs = buf

            viewModel.updateBoosterProgress(pos, resolvedDur, buf)
            val playingNow = exoPlayer.isPlaying
            if (playingNow && (buf - pos) >= 15_000L) {
                viewModel.onPlaybackHealthyBufferAhead()
            }
            if (latestIsBuffering && exoPlayer.playWhenReady && !latestShowUpNext && (buf - pos) < 1_500L) {
                continuousBufferingMs += 500L
                if (continuousBufferingMs >= 15_000L) {
                    continuousBufferingMs = 0L
                    viewModel.onBufferingStallDetected(pos)
                }
            } else {
                continuousBufferingMs = 0L
            }

            val curSegment = activeSkipSegment
            if (curSegment != null) {
                if (latestHudVisible ||
                    latestShowUpNext ||
                    latestIsInPip ||
                    latestIsLocked ||
                    !SkipPromptLogic.isStillInSegmentWindow(pos, resolvedDur, curSegment)
                ) {
                    activeSkipSegment = null
                }
            } else {
                val curState = latestState
                val curSettings = latestSettings
                if (curSettings.skipPromptEnabled &&
                    curState.skipTimesResolved &&
                    playingNow &&
                    !latestHudVisible &&
                    !latestShowUpNext &&
                    !latestIsInPip &&
                    !latestIsLocked &&
                    resolvedDur > 0L
                ) {
                    val allowFallbackOp = curState.initialResumePositionMs < FALLBACK_OP_TRIGGER_END_MS
                    val candidates = SkipPromptLogic.resolveCandidateSegments(
                        exactSkipTimes = curState.skipTimes,
                        durationMs = resolvedDur,
                        skipOpFallbackSeconds = curSettings.skipOpSeconds,
                        allowFallbackOp = allowFallbackOp
                    )
                    val triggered = SkipPromptLogic.findTriggerableSegment(
                        positionMs = pos,
                        durationMs = resolvedDur,
                        segments = candidates,
                        handledTypes = handledSkipTypes
                    )
                    if (triggered != null) {
                        handledSkipTypes.add(triggered.type)
                        activeSkipSegment = triggered
                    }
                }
            }

            tick++
            if (tick % 30 == 0 && playingNow && resolvedDur > 0L) {
                viewModel.persistProgress(pos, resolvedDur)
            }
        }
    }

    // Auto-hide HUD after 4.0s while playing
    LaunchedEffect(hudVisible, isPlaying, hudInteractionNonce, showUpNextOverlay) {
        if (hudVisible && isPlaying && !showUpNextOverlay) {
            delay(4000L)
            hudVisible = false
        }
    }

    LaunchedEffect(hudVisible) {
        if (hudVisible) activeSkipSegment = null
    }

    LaunchedEffect(showUpNextOverlay, lifecycleOwner) {
        if (showUpNextOverlay) {
            activeSkipSegment = null
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (upNextSecondsLeft > 0 && showUpNextOverlay) {
                    delay(1000L)
                    upNextSecondsLeft--
                }
                if (showUpNextOverlay && upNextSecondsLeft <= 0) {
                    showUpNextOverlay = false
                    latestState.stream?.nextEpisodeKey?.let { switchToEpisodeInstant(it) }
                }
            }
        }
    }

    LaunchedEffect(seekFeedbackNonce) {
        if (seekFeedback != null) {
            delay(1100L)
            seekFeedback = null
        }
    }

    var lockOverlayVisible by remember { mutableStateOf(false) }
    LaunchedEffect(showUnlockHintNonce, isLocked) {
        if (isLocked && showUnlockHintNonce > 0) {
            lockOverlayVisible = true
            delay(2500L)
            lockOverlayVisible = false
        } else {
            lockOverlayVisible = false
        }
    }

    fun triggerSeek(deltaMs: Long, label: String) {
        val dur = exoPlayer.duration.coerceAtLeast(progressState.durationMs)
        val target = (exoPlayer.currentPosition + deltaMs).coerceAtLeast(0L).let {
            if (dur > 0L) it.coerceAtMost(dur) else it
        }
        exoPlayer.seekTo(target)
        progressState.positionMs = target
        seekFeedback = label
        seekFeedbackNonce++
    }

    fun executeSkipAction(action: SkipAction) {
        val dur = exoPlayer.duration.coerceAtLeast(progressState.durationMs)
        if (action.kind == SkipActionKind.NEXT_EPISODE) {
            val nextKey = latestState.stream?.nextEpisodeKey
            if (!nextKey.isNullOrBlank()) {
                viewModel.persistProgress(
                    positionMs = dur,
                    durationMs = dur,
                    forceWatched = true
                )
                seekFeedback = action.feedbackLabel
                seekFeedbackNonce++
                switchToEpisodeInstant(nextKey)
                return
            }
        }
        val target = action.targetPositionMs.coerceAtLeast(0L).let {
            if (dur > 0L) it.coerceAtMost(dur) else it
        }
        exoPlayer.seekTo(target)
        progressState.positionMs = target
        seekFeedback = action.feedbackLabel
        seekFeedbackNonce++
        hudInteractionNonce++
    }

    fun exitPlayer() {
        viewModel.persistProgress(
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
            durationMs = exoPlayer.duration.coerceAtLeast(progressState.durationMs)
        )
        onBack()
    }

    BackHandler {
        when {
            isLocked -> showUnlockHintNonce++
            showUpNextOverlay -> showUpNextOverlay = false
            hudVisible && state.errorMessage == null && !state.isLoading -> hudVisible = false
            else -> exitPlayer()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setKeepContentOnPlayerReset(true)
                    resizeMode = resizeOption.mode
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { playerView ->
                if (playerView.resizeMode != resizeOption.mode) {
                    playerView.resizeMode = resizeOption.mode
                }
            },
            onRelease = { it.player = null },
            modifier = Modifier.fillMaxSize().onGloballyPositioned {
                val bounds = it.boundsInWindow()
                pipSource = Rect(bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt())
            }
        )

        // Touch Gesture Layer (disabled in PiP)
        if (!isInPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isLocked, settings.doubleTapSeekSeconds) {
                        detectTapGestures(
                            onTap = {
                                if (isLocked) {
                                    showUnlockHintNonce++
                                } else {
                                    hudVisible = !hudVisible
                                    if (hudVisible) hudInteractionNonce++
                                }
                            },
                            onDoubleTap = { offset ->
                                if (isLocked) {
                                    showUnlockHintNonce++
                                    return@detectTapGestures
                                }
                                val stepSec = latestSettings.doubleTapSeekSeconds
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                when {
                                    offset.x < w * 0.38f -> triggerSeek(-stepSec * 1000L, "-${stepSec}s")
                                    offset.x > w * 0.62f -> triggerSeek(+stepSec * 1000L, "+${stepSec}s")
                                    else -> {
                                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                        hudVisible = true
                                        hudInteractionNonce++
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(isLocked) {
                        if (isLocked) return@pointerInput
                        var activeType: GestureType? = null
                        var accumBrightness = 0.5f
                        var accumVolume = 0f
                        var maxVol = 15

                        detectVerticalDragGestures(
                            onDragStart = { startOffset ->
                                val isLeft = startOffset.x < size.width / 2f
                                activeType = if (isLeft) GestureType.BRIGHTNESS else GestureType.VOLUME
                                if (isLeft) {
                                    val lp = activity?.window?.attributes
                                    val currentB = lp?.screenBrightness ?: -1f
                                    accumBrightness = if (currentB in 0.01f..1f) currentB else 0.5f
                                } else if (audioManager != null) {
                                    maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        .coerceAtLeast(1)
                                    accumVolume = audioManager
                                        .getStreamVolume(AudioManager.STREAM_MUSIC)
                                        .toFloat()
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val heightPx = size.height.toFloat().coerceAtLeast(300f)
                                val deltaFraction = -dragAmount / (heightPx * 0.7f)
                                when (activeType) {
                                    GestureType.BRIGHTNESS -> {
                                        accumBrightness = (accumBrightness + deltaFraction).coerceIn(0.03f, 1f)
                                        val lp = activity?.window?.attributes
                                        if (lp != null) {
                                            lp.screenBrightness = accumBrightness
                                            activity.window?.attributes = lp
                                        }
                                        gestureIndicator = GestureIndicator(
                                            GestureType.BRIGHTNESS,
                                            (accumBrightness * 100f).roundToInt()
                                        )
                                    }
                                    GestureType.VOLUME -> {
                                        if (audioManager != null) {
                                            accumVolume = (accumVolume + deltaFraction * maxVol)
                                                .coerceIn(0f, maxVol.toFloat())
                                            val targetVol = accumVolume.roundToInt()
                                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                            gestureIndicator = GestureIndicator(
                                                GestureType.VOLUME,
                                                ((targetVol.toFloat() / maxVol.toFloat()) * 100f).roundToInt()
                                            )
                                        }
                                    }
                                    null -> Unit
                                }
                            },
                            onDragEnd = {
                                activeType = null
                                gestureIndicator = null
                            },
                            onDragCancel = {
                                activeType = null
                                gestureIndicator = null
                            }
                        )
                    }
            )
        }

        // Loading / Buffering Spinner (220ms anti-flicker debounce when disk cache is warm)
        if (!isInPipMode && showBufferingSpinner) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = palette.crimson)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (state.isLoading) {
                        "Resolving stream..."
                    } else {
                        "Buffering Mirror ${state.mirrorState.displayIndex}/${state.mirrorState.totalCount}..."
                    },
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        // Brightness / Volume Swipe Indicator
        val gest = gestureIndicator
        if (!isInPipMode && gest != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(SEEK_BADGE_SHAPE)
                    .background(Color(0xD9121212))
                    .border(1.5.dp, palette.crimson, SEEK_BADGE_SHAPE)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (gest.type == GestureType.BRIGHTNESS) {
                        AhIcons.Brightness6
                    } else {
                        AhIcons.VolumeUp
                    },
                    contentDescription = null,
                    tint = palette.crimson,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "${if (gest.type == GestureType.BRIGHTNESS) "Brightness" else "Volume"} ${gest.percent}%",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (!isInPipMode && seekFeedback != null && !hudVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(SEEK_BADGE_SHAPE)
                    .background(Color(0xCC121212))
                    .border(1.5.dp, palette.crimson, SEEK_BADGE_SHAPE)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = seekFeedback.orEmpty(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Stream Error Overlay
        if (!isInPipMode && state.errorMessage != null) {
            AhErrorBox(
                errorMessage = state.errorMessage ?: "Playback failed",
                onRetry = viewModel::retryCurrentEpisode
            )
        }

        // Lock-Controls Unlock Pill
        AnimatedVisibility(
            visible = !isInPipMode && isLocked && lockOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(AhShape20)
                    .background(Color(0xDD121212))
                    .border(1.5.dp, palette.crimson, AhShape20)
                    .clickable { isLocked = false }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = AhIcons.LockOpen,
                    contentDescription = "Unlock",
                    tint = palette.crimson,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Screen Locked • Tap to Unlock",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Mobile Touch HUD
        AnimatedVisibility(
            visible = !isInPipMode && !isLocked && hudVisible && !showUpNextOverlay && state.errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Scrim Bar
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(HUD_TOP_SCRIM)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = if (compactHud) Modifier.fillMaxWidth() else Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HudIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            onClick = { exitPlayer() }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.animeTitle.ifBlank { "AnimeHeaven" },
                                color = palette.crimson,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = state.stream?.episodeLabel ?: "Loading...",
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Row(
                        modifier = if (compactHud) Modifier.horizontalScroll(rememberScrollState()) else Modifier,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.mirrorState.totalCount > 0) {
                            HudPillChip(
                                icon = AhIcons.SwapHoriz,
                                label = "M${state.mirrorState.displayIndex}/${state.mirrorState.totalCount}",
                                onClick = {
                                    viewModel.switchMirrorManually(exoPlayer.currentPosition.coerceAtLeast(0L))
                                    hudInteractionNonce++
                                }
                            )
                        }
                        HudPillChip(
                            icon = AhIcons.Speed,
                            label = "${playbackSpeed}x",
                            onClick = {
                                val nextIdx = (PLAYBACK_SPEEDS.indexOf(playbackSpeed) + 1) % PLAYBACK_SPEEDS.size
                                playbackSpeed = PLAYBACK_SPEEDS[nextIdx]
                                exoPlayer.setPlaybackSpeed(playbackSpeed)
                                hudInteractionNonce++
                            }
                        )
                        HudIconButton(
                            icon = AhIcons.AspectRatio,
                            contentDescription = resizeOption.label,
                            onClick = {
                                val entries = ResizeOption.entries
                                resizeOption = entries[(resizeOption.ordinal + 1) % entries.size]
                                seekFeedback = "Zoom: ${resizeOption.label}"
                                seekFeedbackNonce++
                                hudInteractionNonce++
                            }
                        )
                        if (pipActivity?.supportsPip == true) {
                            HudIconButton(
                                icon = AhIcons.PictureInPictureAlt,
                                contentDescription = "Picture in Picture",
                                onClick = {
                                    hudVisible = false
                                    onRequestPip()
                                }
                            )
                        }
                        HudIconButton(
                            icon = AhIcons.Lock,
                            contentDescription = "Lock Screen",
                            onClick = {
                                isLocked = true
                                hudVisible = false
                                showUnlockHintNonce++
                            }
                        )
                        HudIconButton(
                            icon = if (isLandscape) AhIcons.FullscreenExit else AhIcons.Fullscreen,
                            contentDescription = "Toggle Orientation",
                            onClick = {
                                isLandscape = !isLandscape
                                hudInteractionNonce++
                            }
                        )
                    }
                }

                // Center Play/Pause + Prev/Next + Seek Cluster
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    val stream = state.stream
                    CenterTransportButton(
                        icon = AhIcons.SkipPrevious,
                        contentDescription = "Prev Episode",
                        enabled = stream?.prevEpisodeKey != null,
                        sizeDp = 44,
                        onClick = {
                            stream?.prevEpisodeKey?.let { key ->
                                viewModel.persistProgress(
                                    exoPlayer.currentPosition.coerceAtLeast(0L),
                                    exoPlayer.duration.coerceAtLeast(progressState.durationMs)
                                )
                                viewModel.loadEpisode(key)
                            }
                        }
                    )
                    CenterTransportButton(
                        icon = AhIcons.Replay10,
                        contentDescription = "-10s",
                        enabled = true,
                        sizeDp = 46,
                        onClick = {
                            triggerSeek(-10_000L, "-10s")
                            hudInteractionNonce++
                        }
                    )
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(palette.crimson)
                            .clickable {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                hudInteractionNonce++
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) AhIcons.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    CenterTransportButton(
                        icon = AhIcons.FastForward,
                        contentDescription = "+10s",
                        enabled = true,
                        sizeDp = 46,
                        onClick = {
                            triggerSeek(+10_000L, "+10s")
                            hudInteractionNonce++
                        }
                    )
                    CenterTransportButton(
                        icon = AhIcons.SkipNext,
                        contentDescription = "Next Episode",
                        enabled = stream?.nextEpisodeKey != null,
                        sizeDp = 44,
                        onClick = {
                            stream?.nextEpisodeKey?.let { key ->
                                viewModel.persistProgress(
                                    exoPlayer.currentPosition.coerceAtLeast(0L),
                                    exoPlayer.duration.coerceAtLeast(progressState.durationMs)
                                )
                                switchToEpisodeInstant(key)
                            }
                        }
                    )
                }

                // Bottom Scrim Bar: Interactive Scrubber + Skip OP / Skip ED Pills
                val stream = state.stream
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(HUD_BOTTOM_SCRIM)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                        )
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlayerInteractiveSeekSection(
                        progressState = progressState,
                        accentColor = palette.crimson,
                        secondaryTextColor = HUD_TEXT_SECONDARY,
                        onScrubStart = { hudInteractionNonce++ },
                        onScrubCommit = { targetMs ->
                            exoPlayer.seekTo(targetMs)
                            progressState.positionMs = targetMs
                            hudInteractionNonce++
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HudPillChip(
                            icon = AhIcons.Forward30,
                            label = if (state.skipTimes.op != null) {
                                "Skip OP"
                            } else {
                                "Skip OP (+${settings.skipOpSeconds}s)"
                            },
                            onClick = {
                                val action = SkipPromptLogic.computeManualHudSkipOp(
                                    exactOp = state.skipTimes.op,
                                    currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                                    durationMs = exoPlayer.duration.coerceAtLeast(progressState.durationMs),
                                    fallbackSkipSeconds = settings.skipOpSeconds
                                )
                                executeSkipAction(action)
                            }
                        )
                        HudPillChip(
                            icon = AhIcons.FastForward,
                            label = if (state.skipTimes.ed != null || stream?.nextEpisodeKey == null) {
                                "Skip ED"
                            } else {
                                "Skip ED → Next"
                            },
                            onClick = {
                                val action = SkipPromptLogic.computeManualHudSkipEd(
                                    exactEd = state.skipTimes.ed,
                                    currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                                    durationMs = exoPlayer.duration.coerceAtLeast(progressState.durationMs),
                                    fallbackSkipSeconds = settings.skipOpSeconds,
                                    hasNextEpisode = !stream?.nextEpisodeKey.isNullOrBlank()
                                )
                                executeSkipAction(action)
                            }
                        )
                    }
                }
            }
        }

        // One-Tap Skip OP / ED Floating Pill (60fps GPU-animated bar)
        val segment = activeSkipSegment
        AnimatedVisibility(
            visible = !isInPipMode && !isLocked && segment != null && !hudVisible && !showUpNextOverlay && state.errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            if (segment != null) {
                SkipPromptOverlay(
                    segment = segment,
                    currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                    durationMs = exoPlayer.duration.coerceAtLeast(progressState.durationMs),
                    fallbackSkipSeconds = settings.skipOpSeconds,
                    nextEpisodeLabel = state.stream?.nextEpisodeLabel,
                    hasNextEpisode = !state.stream?.nextEpisodeKey.isNullOrBlank(),
                    onExpired = {
                        if (activeSkipSegment == segment) activeSkipSegment = null
                    },
                    onExecute = { action ->
                        activeSkipSegment = null
                        executeSkipAction(action)
                    }
                )
            }
        }

        // Auto-Play Next Episode Countdown Overlay
        if (!isInPipMode && showUpNextOverlay && state.stream?.nextEpisodeKey != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                    )
                    .padding(horizontal = 20.dp, vertical = 22.dp)
                    .clip(AhShape10)
                    .background(Color(0xEE121212))
                    .border(1.5.dp, palette.crimson, AhShape10)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "UP NEXT IN ${upNextSecondsLeft}s",
                        color = palette.crimson,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${state.animeTitle} — ${state.stream?.nextEpisodeLabel.orEmpty()}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(AhShape8)
                                .background(palette.crimson)
                                .clickable {
                                    showUpNextOverlay = false
                                    state.stream?.nextEpisodeKey?.let { switchToEpisodeInstant(it) }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Play Now",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(AhShape8)
                                .background(HUD_SECONDARY_BUTTON_BG)
                                .clickable { showUpNextOverlay = false }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerInteractiveSeekSection(
    progressState: PlaybackProgressState,
    accentColor: Color,
    secondaryTextColor: Color,
    onScrubStart: () -> Unit,
    onScrubCommit: (Long) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val safeDuration = progressState.durationMs.coerceAtLeast(1L)
    val displayPosMs = if (isDragging) {
        (dragFraction * safeDuration).toLong()
    } else {
        progressState.positionMs
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDurationMs(displayPosMs),
                color = if (isDragging) accentColor else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatDurationMs(progressState.durationMs),
                color = secondaryTextColor,
                fontSize = 13.sp
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics {
                    contentDescription = "Playback position"
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        (displayPosMs.toFloat() / safeDuration).coerceIn(0f, 1f), 0f..1f
                    )
                    setProgress { value ->
                        if (progressState.durationMs <= 0L) false else {
                            onScrubCommit((value.coerceIn(0f, 1f) * safeDuration).toLong())
                            true
                        }
                    }
                }
                .pointerInput(safeDuration) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                        onScrubCommit((frac * safeDuration).toLong())
                    }
                }
                .pointerInput(safeDuration) {
                    detectHorizontalDragGestures(
                        onDragStart = { startOffset ->
                            isDragging = true
                            dragFraction = (startOffset.x / size.width.toFloat().coerceAtLeast(1f))
                                .coerceIn(0f, 1f)
                            onScrubStart()
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragFraction = (change.position.x / size.width.toFloat().coerceAtLeast(1f))
                                .coerceIn(0f, 1f)
                            onScrubStart()
                        },
                        onDragEnd = {
                            val target = (dragFraction * safeDuration).toLong()
                            isDragging = false
                            onScrubCommit(target)
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            val barH = 5.dp.toPx()
            val topY = (size.height - barH) / 2f
            val corner = CornerRadius(barH / 2f, barH / 2f)
            val bufferFrac = (progressState.bufferedMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
            val progressFrac = if (isDragging) {
                dragFraction
            } else {
                (progressState.positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
            }

            drawRoundRect(
                color = SEEK_TRACK_BG,
                topLeft = Offset(0f, topY),
                size = Size(size.width, barH),
                cornerRadius = corner
            )
            if (bufferFrac > 0f) {
                drawRoundRect(
                    color = SEEK_BUFFER_COLOR,
                    topLeft = Offset(0f, topY),
                    size = Size(size.width * bufferFrac, barH),
                    cornerRadius = corner
                )
            }
            if (progressFrac > 0f) {
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(0f, topY),
                    size = Size(size.width * progressFrac, barH),
                    cornerRadius = corner
                )
            }
            val thumbRadius = if (isDragging) 9.dp.toPx() else 7.dp.toPx()
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(size.width * progressFrac, size.height / 2f)
            )
            drawCircle(
                color = accentColor,
                radius = thumbRadius * 0.65f,
                center = Offset(size.width * progressFrac, size.height / 2f)
            )
        }
    }
}

@Composable
private fun SkipPromptOverlay(
    segment: SkipSegment,
    currentPositionMs: Long,
    durationMs: Long,
    fallbackSkipSeconds: Int,
    nextEpisodeLabel: String?,
    hasNextEpisode: Boolean,
    onExpired: () -> Unit,
    onExecute: (SkipAction) -> Unit
) {
    val palette = LocalAhPalette.current
    var remainingSeconds by remember(segment) { mutableIntStateOf(SKIP_PROMPT_COUNTDOWN_SECONDS) }
    val barProgress = remember(segment) { Animatable(1f) }

    LaunchedEffect(segment) {
        barProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = SKIP_PROMPT_COUNTDOWN_SECONDS * 1000,
                easing = LinearEasing
            )
        )
        onExpired()
    }

    LaunchedEffect(segment) {
        for (sec in SKIP_PROMPT_COUNTDOWN_SECONDS downTo 1) {
            remainingSeconds = sec
            delay(1000L)
        }
    }

    val action = remember(segment, durationMs, fallbackSkipSeconds, hasNextEpisode) {
        SkipPromptLogic.computeSkipAction(
            segment = segment,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            fallbackSkipSeconds = fallbackSkipSeconds,
            hasNextEpisode = hasNextEpisode
        )
    }
    val titleText = when {
        segment.type == SkipSegmentType.OP -> "Skip OP"
        action.kind == SkipActionKind.NEXT_EPISODE -> "Skip ED • Next"
        else -> "Skip ED"
    }
    val destinationText = when {
        action.kind == SkipActionKind.NEXT_EPISODE -> nextEpisodeLabel ?: "Next Episode"
        segment.isExact -> formatDurationMs(action.targetPositionMs)
        segment.type == SkipSegmentType.OP -> "+${fallbackSkipSeconds}s"
        else -> formatDurationMs(action.targetPositionMs)
    }

    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(AhShape10)
            .background(Color(0xEE121212))
            .border(1.5.dp, palette.crimson, AhShape10)
            .clickable { onExecute(action) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap to jump → $destinationText",
                    color = HUD_TEXT_SECONDARY,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${remainingSeconds}s",
                color = palette.crimson,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color(0x44FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = SKIP_PROMPT_TRANSFORM_ORIGIN
                        scaleX = barProgress.value.coerceIn(0f, 1f)
                    }
                    .background(palette.crimson)
            )
        }
    }
}

@Composable
private fun HudIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0xAA121212))
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun HudPillChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(AhShape20)
            .background(Color(0xCC121212))
            .border(1.dp, Color(0x44FFFFFF), AhShape20)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CenterTransportButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    sizeDp: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(if (enabled) Color(0xAA121212) else Color(0x44121212))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color(0x55FFFFFF),
            modifier = Modifier.size((sizeDp * 0.56f).dp)
        )
    }
}
