package me.animeheaven.android.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.animeheaven.android.data.model.AhThemeId
import me.animeheaven.android.data.model.FeedViewMode
import me.animeheaven.android.data.model.PreloadMode

@Immutable
data class AppSettings(
    val themeId: AhThemeId = AhThemeId.DARK,
    val defaultViewMode: FeedViewMode = FeedViewMode.LIST,
    val autoPlayNext: Boolean = true,
    val skipOpSeconds: Int = 85,
    val skipPromptEnabled: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val autoLandscapePlayer: Boolean = true,
    val preloadMode: PreloadMode = PreloadMode.AGGRESSIVE
)

class AppSettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _themeId = MutableStateFlow(_settings.value.themeId)
    val themeId: StateFlow<AhThemeId> = _themeId.asStateFlow()

    private fun readSettings(): AppSettings {
        val themeKey = prefs.getString(KEY_THEME, AhThemeId.DARK.cssKey)
        val viewModeName = prefs.getString(KEY_VIEW_MODE, FeedViewMode.LIST.name)
        val autoPlayNext = prefs.getBoolean(KEY_AUTO_PLAY_NEXT, true)
        val skipOpSeconds = prefs.getInt(KEY_SKIP_OP_SECONDS, 85)
        val skipPromptEnabled = prefs.getBoolean(KEY_SKIP_PROMPT_ENABLED, true)
        val doubleTapSeekSeconds = prefs.getInt(KEY_DOUBLE_TAP_SEEK_SECONDS, 10)
        val autoLandscapePlayer = prefs.getBoolean(KEY_AUTO_LANDSCAPE_PLAYER, true)
        val preloadName = prefs.getString(KEY_PRELOAD_MODE, PreloadMode.AGGRESSIVE.name)

        val viewMode = runCatching {
            FeedViewMode.valueOf(viewModeName ?: FeedViewMode.LIST.name)
        }.getOrDefault(FeedViewMode.LIST)

        val preloadMode = runCatching {
            PreloadMode.valueOf(preloadName ?: PreloadMode.AGGRESSIVE.name)
        }.getOrDefault(PreloadMode.AGGRESSIVE)

        return AppSettings(
            themeId = AhThemeId.fromCssKey(themeKey),
            defaultViewMode = viewMode,
            autoPlayNext = autoPlayNext,
            skipOpSeconds = skipOpSeconds,
            skipPromptEnabled = skipPromptEnabled,
            doubleTapSeekSeconds = doubleTapSeekSeconds,
            autoLandscapePlayer = autoLandscapePlayer,
            preloadMode = preloadMode
        )
    }

    fun setTheme(themeId: AhThemeId) {
        if (_themeId.value == themeId) return
        prefs.edit().putString(KEY_THEME, themeId.cssKey).apply()
        _themeId.value = themeId
        _settings.update { it.copy(themeId = themeId) }
    }

    fun setDefaultViewMode(mode: FeedViewMode) {
        prefs.edit().putString(KEY_VIEW_MODE, mode.name).apply()
        _settings.update { it.copy(defaultViewMode = mode) }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY_NEXT, enabled).apply()
        _settings.update { it.copy(autoPlayNext = enabled) }
    }

    fun setSkipOpSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_SKIP_OP_SECONDS, seconds).apply()
        _settings.update { it.copy(skipOpSeconds = seconds) }
    }

    fun setSkipPromptEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SKIP_PROMPT_ENABLED, enabled).apply()
        _settings.update { it.copy(skipPromptEnabled = enabled) }
    }

    fun setDoubleTapSeekSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_DOUBLE_TAP_SEEK_SECONDS, seconds).apply()
        _settings.update { it.copy(doubleTapSeekSeconds = seconds) }
    }

    fun setAutoLandscapePlayer(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LANDSCAPE_PLAYER, enabled).apply()
        _settings.update { it.copy(autoLandscapePlayer = enabled) }
    }

    fun setPreloadMode(mode: PreloadMode) {
        prefs.edit().putString(KEY_PRELOAD_MODE, mode.name).apply()
        _settings.update { it.copy(preloadMode = mode) }
    }

    companion object {
        private const val PREFS_NAME = "animeheaven_android_settings"
        private const val KEY_THEME = "theme_css_key"
        private const val KEY_VIEW_MODE = "default_view_mode"
        private const val KEY_AUTO_PLAY_NEXT = "auto_play_next"
        private const val KEY_SKIP_OP_SECONDS = "skip_op_seconds"
        private const val KEY_SKIP_PROMPT_ENABLED = "skip_prompt_enabled"
        private const val KEY_DOUBLE_TAP_SEEK_SECONDS = "double_tap_seek_seconds"
        private const val KEY_AUTO_LANDSCAPE_PLAYER = "auto_landscape_player"
        private const val KEY_PRELOAD_MODE = "preload_mode"
    }
}
