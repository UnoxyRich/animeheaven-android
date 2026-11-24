package me.animeheaven.android

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.animeheaven.android.ui.navigation.AppNavGraph
import me.animeheaven.android.ui.theme.AnimeHeavenTheme
import me.animeheaven.android.ui.theme.paletteFor

class MainActivity : ComponentActivity() {

    private val _isInPipMode = MutableStateFlow(false)
    val isInPipMode: StateFlow<Boolean> = _isInPipMode.asStateFlow()

    @Volatile
    var pipEligibilityCheck: (() -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        val container = (application as AnimeHeavenApplication).container

        setContent {
            val themeId by container.settingsStore.themeId.collectAsStateWithLifecycle()
            val palette = paletteFor(themeId)

            LaunchedEffect(palette.isLight) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = palette.isLight
                controller.isAppearanceLightNavigationBars = palette.isLight
            }

            AnimeHeavenTheme(themeId = themeId) {
                AppNavGraph(
                    container = container,
                    isInPipModeFlow = isInPipMode,
                    onRegisterPipEligibility = { check -> pipEligibilityCheck = check },
                    onRequestPip = { tryEnterPipMode() }
                )
            }
        }

        // AnimeHeavenTheme paints its own full-screen background; removing the window drawable
        // eliminates 1 full-screen GPU overdraw layer on every frame.
        window.setBackgroundDrawable(null)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (pipEligibilityCheck?.invoke() == true) {
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

    fun tryEnterPipMode(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return runCatching {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }.getOrDefault(false)
    }
}
