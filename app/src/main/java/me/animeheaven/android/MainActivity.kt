package me.animeheaven.android

import android.app.PictureInPictureParams
import android.app.PendingIntent
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.animeheaven.android.ui.navigation.AppNavGraph
import me.animeheaven.android.ui.components.LocalArtworkResolver
import me.animeheaven.android.ui.theme.AnimeHeavenTheme
import me.animeheaven.android.ui.theme.paletteFor

class MainActivity : ComponentActivity() {

    private val _isInPipMode = MutableStateFlow(false)
    val isInPipMode: StateFlow<Boolean> = _isInPipMode.asStateFlow()

    @Volatile
    var pipEligibilityCheck: (() -> Boolean)? = null
    private var pipActionHandler: ((String) -> Unit)? = null
    private var pipParams: PictureInPictureParams? = null
    private val pipCommandAction get() = "$packageName.PIP_COMMAND"
    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == pipCommandAction && isInPictureInPictureMode) {
                intent.getStringExtra("command")?.let { pipActionHandler?.invoke(it) }
            }
        }
    }

    internal val supportsPip: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        if (supportsPip) {
            ContextCompat.registerReceiver(this, pipReceiver, IntentFilter(pipCommandAction),
                ContextCompat.RECEIVER_NOT_EXPORTED)
        }
        val container = (application as AnimeHeavenApplication).container

        setContent {
            val themeId by container.settingsStore.themeId.collectAsStateWithLifecycle()
            val palette = paletteFor(themeId)

            LaunchedEffect(palette.isLight) {
                enableEdgeToEdge(
                    statusBarStyle = if (palette.isLight) {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    } else {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (palette.isLight) {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    } else {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    }
                )
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = palette.isLight
                controller.isAppearanceLightNavigationBars = palette.isLight
            }

            AnimeHeavenTheme(themeId = themeId) {
                CompositionLocalProvider(LocalArtworkResolver provides container.artworkResolver) {
                    AppNavGraph(
                        container = container,
                        isInPipModeFlow = isInPipMode,
                        onRegisterPipEligibility = { check -> pipEligibilityCheck = check },
                        onRequestPip = { tryEnterPipMode() }
                    )
                }
            }
        }

        // AnimeHeavenTheme paints its own full-screen background; removing the window drawable
        // eliminates 1 full-screen GPU overdraw layer on every frame.
        window.setBackgroundDrawable(null)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && pipEligibilityCheck?.invoke() == true) {
            tryEnterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        _isInPipMode.value = isInPictureInPictureMode
    }

    override fun onDestroy() {
        if (supportsPip) unregisterReceiver(pipReceiver)
        super.onDestroy()
    }

    internal fun updatePlayerPip(
        eligible: Boolean,
        playing: Boolean,
        source: Rect?,
        onCommand: ((String) -> Unit)?
    ) {
        if (!supportsPip || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        pipActionHandler = onCommand
        val params = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9))
        if (source != null && !source.isEmpty) params.setSourceRectHint(source)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) params.setAutoEnterEnabled(eligible)
        if (onCommand != null) {
            fun action(id: Int, command: String, title: String, icon: Int): RemoteAction {
                val intent = Intent(pipCommandAction).setPackage(packageName).putExtra("command", command)
                val pending = PendingIntent.getBroadcast(this, id, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                return RemoteAction(Icon.createWithResource(this, icon), title, title, pending)
            }
            params.setActions(listOf(
                action(1, "back", "Back 10 seconds", android.R.drawable.ic_media_rew),
                action(2, "toggle", if (playing) "Pause" else "Play",
                    if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play),
                action(3, "forward", "Forward 10 seconds", android.R.drawable.ic_media_ff)
            ))
        } else params.setActions(emptyList())
        pipParams = params.build().also { setPictureInPictureParams(it) }
    }

    fun tryEnterPipMode(): Boolean {
        if (!supportsPip || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return runCatching {
            val params = pipParams ?: PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }.getOrDefault(false)
    }
}
