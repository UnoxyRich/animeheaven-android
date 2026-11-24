package me.animeheaven.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.launch
import me.animeheaven.android.data.local.AppSettingsStore
import me.animeheaven.android.data.model.AhThemeId
import me.animeheaven.android.data.model.FeedViewMode
import me.animeheaven.android.data.model.PreloadMode
import me.animeheaven.android.data.repository.AnimeHeavenRepository
import me.animeheaven.android.ui.components.AhHorizontalPadding
import me.animeheaven.android.ui.components.AhIcons
import me.animeheaven.android.ui.components.AhShape10
import me.animeheaven.android.ui.components.AhShape12
import me.animeheaven.android.ui.components.AhShape8
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
    var cacheSummary by remember { mutableStateOf("Calculating...") }

    LaunchedEffect(Unit) {
        cacheSummary = onGetCacheSummary()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // Header Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .statusBarsPadding()
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
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Appearance, playback gestures & cache",
                        color = palette.textMuted,
                        fontSize = 12.sp
                    )
                }

                if (statusBanner != null) {
                    Box(
                        modifier = Modifier
                            .clip(AhShape8)
                            .background(palette.crimson)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = statusBanner.orEmpty(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
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
                    horizontal = AhHorizontalPadding,
                    vertical = 14.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "theme_section") {
                    SettingsCard(title = "AnimeHeaven Theme") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            themeRows.forEach { rowThemes ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowThemes.forEach { theme ->
                                        val preview = paletteFor(theme)
                                        val selected = settings.themeId == theme
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(AhShape10)
                                                .background(palette.surfaceElevated)
                                                .border(
                                                    width = if (selected) 2.dp else 1.dp,
                                                    color = if (selected) palette.crimson else palette.borderSubtle,
                                                    shape = AhShape10
                                                )
                                                .clickable {
                                                    settingsStore.setTheme(theme)
                                                    statusBanner = theme.displayName
                                                }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .then(
                                                        if (preview.backgroundBrush != null) {
                                                            Modifier.background(preview.backgroundBrush)
                                                        } else {
                                                            Modifier.background(preview.background)
                                                        }
                                                    )
                                                    .border(2.dp, preview.crimson, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (selected) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "Selected",
                                                        tint = preview.crimson,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = theme.displayName,
                                                    color = palette.textPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = theme.subtitle,
                                                    color = palette.textMuted,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
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
                                    statusBanner = "View: List"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            OptionChip(
                                label = "Grid View",
                                selected = settings.defaultViewMode == FeedViewMode.GRID,
                                onClick = {
                                    settingsStore.setDefaultViewMode(FeedViewMode.GRID)
                                    statusBanner = "View: Grid"
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
                                    statusBanner = if (it) "Auto-Play: On" else "Auto-Play: Off"
                                }
                            )
                            SwitchRow(
                                title = "Auto-Rotate to Landscape",
                                subtitle = "Enter fullscreen landscape automatically when watching",
                                checked = settings.autoLandscapePlayer,
                                onCheckedChange = {
                                    settingsStore.setAutoLandscapePlayer(it)
                                    statusBanner = if (it) "Auto-Landscape: On" else "Auto-Landscape: Off"
                                }
                            )
                            SwitchRow(
                                title = "Skip OP & ED Auto-Prompt",
                                subtitle = "Show 7-second tap pill during anime openings & endings",
                                checked = settings.skipPromptEnabled,
                                onCheckedChange = {
                                    settingsStore.setSkipPromptEnabled(it)
                                    statusBanner = if (it) "Skip Prompt: On" else "Skip Prompt: Off"
                                }
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Fallback Skip OP Duration",
                                    color = palette.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SKIP_OP_SECONDS_OPTIONS.forEach { seconds ->
                                        OptionChip(
                                            label = "${seconds}s",
                                            selected = settings.skipOpSeconds == seconds,
                                            onClick = {
                                                settingsStore.setSkipOpSeconds(seconds)
                                                statusBanner = "Skip OP: ${seconds}s"
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
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DOUBLE_TAP_SECONDS_OPTIONS.forEach { seconds ->
                                        OptionChip(
                                            label = "±${seconds}s",
                                            selected = settings.doubleTapSeekSeconds == seconds,
                                            onClick = {
                                                settingsStore.setDoubleTapSeekSeconds(seconds)
                                                statusBanner = "Seek: ±${seconds}s"
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
                                        statusBanner = mode.displayName
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
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            ActionButton(
                                label = "Clear Continue Watching & Progress",
                                onClick = {
                                    scope.launch {
                                        repository.clearWatchHistory()
                                        statusBanner = "Watch history cleared"
                                    }
                                }
                            )
                            ActionButton(
                                label = "Clear Recent Searches",
                                onClick = {
                                    scope.launch {
                                        repository.clearSearchHistory()
                                        statusBanner = "Search history cleared"
                                    }
                                }
                            )
                            ActionButton(
                                label = "Clear Offline Cache (Web • Images • Video)",
                                onClick = {
                                    scope.launch {
                                        onClearAllCaches()
                                        cacheSummary = onGetCacheSummary()
                                        statusBanner = "All caches cleared"
                                    }
                                }
                            )
                        }
                    }
                }
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
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = palette.textMuted,
                fontSize = 12.sp
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
