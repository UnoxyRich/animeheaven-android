package me.animeheaven.android.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.animeheaven.android.data.local.AppSettingsStore
import me.animeheaven.android.data.model.AhThemeId
import me.animeheaven.android.data.model.FeedViewMode
import me.animeheaven.android.data.model.PreloadMode
import me.animeheaven.android.data.repository.AnimeHeavenRepository
import me.animeheaven.android.ui.components.AhHorizontalPadding
import me.animeheaven.android.ui.components.AhIcons
import me.animeheaven.android.ui.components.AhShape10
import me.animeheaven.android.ui.components.AhShape12
import me.animeheaven.android.ui.components.AhShape20
import me.animeheaven.android.ui.components.AhShape8
import me.animeheaven.android.ui.components.topSystemSafePadding
import me.animeheaven.android.ui.theme.LocalAhPalette
import me.animeheaven.android.ui.theme.paletteFor

private val SKIP_OP_SECONDS_OPTIONS = listOf(60, 85, 90)
private val DOUBLE_TAP_SECONDS_OPTIONS = listOf(5, 10, 15)

@Composable
fun SettingsScreen(
    settingsStore: AppSettingsStore,
    repository: AnimeHeavenRepository,
    onGetCacheSummary: suspend () -> String = { "" },
    onClearAllCaches: suspend () -> Unit = {},
    onBack: () -> Unit
) {
    val settings by settingsStore.settings.collectAsStateWithLifecycle()
    val palette = LocalAhPalette.current
    val scope = rememberCoroutineScope()
    var statusBanner by remember { mutableStateOf<String?>(null) }
    var statusBannerNonce by remember { mutableIntStateOf(0) }
    var cacheSummary by remember { mutableStateOf("Calculating...") }
    var pendingClear by remember { mutableStateOf<ClearTarget?>(null) }

    fun showStatus(message: String) {
        statusBanner = message
        statusBannerNonce++
    }

    LaunchedEffect(statusBanner, statusBannerNonce) {
        if (statusBanner != null) {
            delay(1800L)
            statusBanner = null
        }
    }

    pendingClear?.let { target ->
        val history = target == ClearTarget.HISTORY
        AlertDialog(
            onDismissRequest = { pendingClear = null },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    text = if (history) "Clear watch history?" else "Clear offline caches?",
                    color = palette.textPrimary
                )
            },
            text = {
                Text(
                    text = if (history) "Saved progress and Continue Watching will be removed."
                    else "Cached pages, images, and video will be removed. Watch history and bookmarks will stay.",
                    color = palette.textSecondary
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingClear = null }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingClear = null
                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                        withContext(NonCancellable) {
                            if (history) repository.clearWatchHistory() else onClearAllCaches()
                        }
                        if (!history) cacheSummary = onGetCacheSummary()
                        showStatus(if (history) "Watch history cleared" else "Offline caches cleared")
                    }
                }) {
                    Text("Clear", color = palette.crimson)
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        cacheSummary = onGetCacheSummary()
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .topSystemSafePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AhHorizontalPadding, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(palette.surfaceElevated)
                            .border(1.dp, palette.borderSubtle, CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Settings & Themes",
                            color = palette.textPrimary,
                            fontSize = 19.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Appearance, playback gestures & cache",
                            color = palette.textMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.borderSubtle)
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val themeCols = if (maxWidth >= 600.dp) 4 else 2
                val themeRows = remember(themeCols) { AhThemeId.entries.chunked(themeCols) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AhHorizontalPadding,
                        top = 14.dp,
                        end = AhHorizontalPadding,
                        bottom = navBarBottom + 20.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "theme_section") {
                        SettingsCard(title = "AnimeHeaven Theme") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                themeRows.forEach { rowThemes ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowThemes.forEach { theme ->
                                            val preview = paletteFor(theme)
                                            val selected = settings.themeId == theme
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clip(AhShape10)
                                                    .background(palette.surfaceElevated)
                                                    .border(
                                                        width = if (selected) 2.dp else 1.dp,
                                                        color = if (selected) palette.crimson else palette.borderSubtle,
                                                        shape = AhShape10
                                                    )
                                                    .clickable {
                                                        settingsStore.setTheme(theme)
                                                        showStatus(theme.displayName)
                                                    }
                                                    .padding(horizontal = 9.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .then(
                                                            if (preview.swatchBrush != null) {
                                                                Modifier.background(preview.swatchBrush)
                                                            } else {
                                                                Modifier.background(preview.background)
                                                            }
                                                        )
                                                        .border(
                                                            width = if (selected) 2.dp else 1.5.dp,
                                                            color = if (selected) {
                                                                preview.crimson
                                                            } else if (preview.isLight) {
                                                                preview.borderSubtle
                                                            } else {
                                                                Color(0x55FFFFFF)
                                                            },
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (selected) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Check,
                                                            contentDescription = "Selected",
                                                            tint = if (preview.isLight) Color(0xFF1A1A1A) else Color.White,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = theme.displayName,
                                                        color = palette.textPrimary,
                                                        fontSize = 13.sp,
                                                        lineHeight = 16.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = theme.subtitle,
                                                        color = palette.textMuted,
                                                        fontSize = 10.sp,
                                                        lineHeight = 13.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        repeat(themeCols - rowThemes.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "catalog_view") {
                        SettingsCard(title = "Default Catalog View") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OptionChip(
                                    label = "List View",
                                    selected = settings.defaultViewMode == FeedViewMode.LIST,
                                    onClick = {
                                        settingsStore.setDefaultViewMode(FeedViewMode.LIST)
                                        showStatus("View: List")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                OptionChip(
                                    label = "Grid View",
                                    selected = settings.defaultViewMode == FeedViewMode.GRID,
                                    onClick = {
                                        settingsStore.setDefaultViewMode(FeedViewMode.GRID)
                                        showStatus("View: Grid")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    item(key = "playback_toggles") {
                        SettingsCard(title = "Playback & Player") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SwitchRow(
                                    title = "Auto-Play Next Episode",
                                    subtitle = "Show 5s Up Next countdown when episode finishes",
                                    checked = settings.autoPlayNext,
                                    onCheckedChange = {
                                        settingsStore.setAutoPlayNext(it)
                                        showStatus(if (it) "Auto-Play: On" else "Auto-Play: Off")
                                    }
                                )
                                SwitchRow(
                                    title = "Auto-Rotate to Landscape",
                                    subtitle = "Enter fullscreen landscape automatically when watching",
                                    checked = settings.autoLandscapePlayer,
                                    onCheckedChange = {
                                        settingsStore.setAutoLandscapePlayer(it)
                                        showStatus(if (it) "Auto-Landscape: On" else "Auto-Landscape: Off")
                                    }
                                )
                                SwitchRow(
                                    title = "Skip OP & ED Auto-Prompt",
                                    subtitle = "Show 7-second tap pill during anime openings & endings",
                                    checked = settings.skipPromptEnabled,
                                    onCheckedChange = {
                                        settingsStore.setSkipPromptEnabled(it)
                                        showStatus(if (it) "Skip Prompt: On" else "Skip Prompt: Off")
                                    }
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Fallback Skip OP Duration",
                                        color = palette.textSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 17.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SKIP_OP_SECONDS_OPTIONS.forEach { seconds ->
                                            OptionChip(
                                                label = "${seconds}s",
                                                selected = settings.skipOpSeconds == seconds,
                                                onClick = {
                                                    settingsStore.setSkipOpSeconds(seconds)
                                                    showStatus("Skip OP: ${seconds}s")
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Double-Tap Seek Step",
                                        color = palette.textSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 17.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        DOUBLE_TAP_SECONDS_OPTIONS.forEach { seconds ->
                                            OptionChip(
                                                label = "±${seconds}s",
                                                selected = settings.doubleTapSeekSeconds == seconds,
                                                onClick = {
                                                    settingsStore.setDoubleTapSeekSeconds(seconds)
                                                    showStatus("Seek: ±${seconds}s")
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "preload_strategy") {
                        SettingsCard(title = "Preload & Mobile Data Strategy") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PreloadMode.entries.forEach { mode ->
                                    OptionChip(
                                        label = mode.displayName,
                                        selected = settings.preloadMode == mode,
                                        onClick = {
                                            settingsStore.setPreloadMode(mode)
                                            showStatus(mode.displayName)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Text(
                                    text = settings.preloadMode.subtitle,
                                    color = palette.textMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }

                    item(key = "data_management") {
                        SettingsCard(title = "Local History & Offline Storage") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (cacheSummary.isNotBlank()) {
                                    Text(
                                        text = cacheSummary,
                                        color = palette.textSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                ActionButton(
                                    label = "Clear Continue Watching & Progress",
                                    onClick = { pendingClear = ClearTarget.HISTORY }
                                )
                                ActionButton(
                                    label = "Clear Recent Searches",
                                    onClick = {
                                        scope.launch {
                                            repository.clearSearchHistory()
                                            showStatus("Search history cleared")
                                        }
                                    }
                                )
                                ActionButton(
                                    label = "Clear Offline Cache (Web • Images • Video)",
                                    onClick = { pendingClear = ClearTarget.CACHE }
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = statusBanner != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(AhShape20)
                    .background(palette.crimson)
                    .border(1.dp, Color(0x33FFFFFF), AhShape20)
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    text = statusBanner.orEmpty(),
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = LocalAhPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AhShape12)
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, AhShape12)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            color = palette.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalAhPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AhShape8)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = palette.textMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.crimson,
                uncheckedThumbColor = palette.textSecondary,
                uncheckedTrackColor = palette.surfaceElevated
            )
        )
    }
}

@Composable
private fun OptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAhPalette.current
    Box(
        modifier = modifier
            .clip(AhShape8)
            .background(if (selected) palette.crimson else palette.surfaceElevated)
            .border(
                width = 1.dp,
                color = if (selected) palette.crimson else palette.borderSubtle,
                shape = AhShape8
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else palette.textSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    onClick: () -> Unit
) {
    val palette = LocalAhPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AhShape8)
            .background(palette.surfaceElevated)
            .border(1.dp, palette.borderSubtle, AhShape8)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = AhIcons.DeleteSweep,
            contentDescription = null,
            tint = palette.crimson,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = palette.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
private enum class ClearTarget { HISTORY, CACHE }
