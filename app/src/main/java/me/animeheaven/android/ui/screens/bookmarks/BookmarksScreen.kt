package me.animeheaven.android.ui.screens.bookmarks

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.animeheaven.android.data.cache.ImageTargetSize
import me.animeheaven.android.data.model.ContinueWatchingItem
import me.animeheaven.android.ui.components.AhCoverImage
import me.animeheaven.android.ui.components.AhHorizontalPadding
import me.animeheaven.android.ui.components.AhIcons
import me.animeheaven.android.ui.components.AhShape10
import me.animeheaven.android.ui.components.AhShape6
import me.animeheaven.android.ui.components.AhShape8
import me.animeheaven.android.ui.components.ContinueWatchingRow
import me.animeheaven.android.ui.components.MobileTopBar
import me.animeheaven.android.ui.components.SectionHeaderWithToggle
import me.animeheaven.android.ui.components.toGridRows
import me.animeheaven.android.ui.theme.LocalAhPalette

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarksScreen(
    viewModel: BookmarksViewModel,
    onOpenHome: () -> Unit,
    onOpenAnime: (String) -> Unit,
    onPlayContinueItem: (ContinueWatchingItem) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSeasonal: (path: String, label: String) -> Unit,
    onOpenRandom: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalAhPalette.current

    Column(modifier = modifier.fillMaxSize()) {
        MobileTopBar(
            seasonLabel = state.seasonLabel,
            onLogoClick = onOpenHome,
            onSeasonalClick = {
                val label = state.seasonLabel ?: "Season"
                state.seasonPath?.takeIf { it.isNotBlank() }?.let { onOpenSeasonal(it, label) }
            },
            onSearchClick = onOpenSearch,
            onRandomClick = onOpenRandom,
            onSettingsClick = onOpenSettings,
            headerTitle = "My Bookmarks",
            showFilterPills = false
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val gridColumns = when {
                maxWidth >= 840.dp -> 4
                maxWidth >= 600.dp -> 3
                else -> 2
            }
            val listState = rememberLazyListState(
                prefetchStrategy = remember { LazyListPrefetchStrategy(nestedPrefetchItemCount = 4) }
            )
            val bookmarkRows = remember(state.bookmarks, gridColumns) {
                state.bookmarks.toGridRows(gridColumns) { it.animeId }
            }

            if (state.bookmarks.isEmpty() && state.continueWatching.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = AhIcons.BookmarkBorder,
                            contentDescription = null,
                            tint = palette.textMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No Bookmarks Yet",
                            color = palette.textPrimary,
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tap the bookmark icon on any anime to save it here.",
                            color = palette.textSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 18.dp)
                ) {
                    if (state.continueWatching.isNotEmpty()) {
                        item(key = "continue_watching", contentType = "continue_watching") {
                            ContinueWatchingRow(
                                items = state.continueWatching,
                                onPlayItem = onPlayContinueItem
                            )
                        }
                        item(key = "bookmarks_header", contentType = "section_header") {
                            SectionHeaderWithToggle(
                                title = "Saved List",
                                viewMode = null,
                                onChangeViewMode = null
                            )
                        }
                    }

                    if (state.bookmarks.isEmpty()) {
                        item(key = "empty", contentType = "empty") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp, vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = AhIcons.BookmarkBorder,
                                    contentDescription = null,
                                    tint = palette.textMuted,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "No Bookmarks Yet",
                                    color = palette.textPrimary,
                                    fontSize = 18.sp,
                                    lineHeight = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Tap the bookmark icon on any anime to save it here.",
                                    color = palette.textSecondary,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(
                            items = bookmarkRows,
                            key = { it.key },
                            contentType = { "bookmark_grid_row" }
                        ) { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AhHorizontalPadding, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.items.forEach { bm ->
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(AhShape10)
                                            .background(palette.surface)
                                            .border(1.dp, palette.borderSubtle, AhShape10)
                                            .clickable { onOpenAnime(bm.animeId) }
                                            .padding(7.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f / 9f)
                                                .clip(AhShape8)
                                        ) {
                                            AhCoverImage(
                                                url = bm.coverUrl,
                                                contentDescription = bm.title,
                                                targetSize = ImageTargetSize.STANDARD_CARD,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            if (bm.lastEpisodeLabel.isNotBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(5.dp)
                                                        .clip(AhShape6)
                                                        .background(palette.crimson)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Ep ${bm.lastEpisodeLabel}",
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        lineHeight = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xBB000000))
                                                    .clickable { viewModel.removeBookmark(bm.animeId) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = AhIcons.DeleteOutline,
                                                    contentDescription = "Remove Bookmark",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = bm.title,
                                            color = palette.textPrimary,
                                            fontSize = 13.sp,
                                            lineHeight = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                        )
                                    }
                                }
                                repeat(gridColumns - row.items.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
