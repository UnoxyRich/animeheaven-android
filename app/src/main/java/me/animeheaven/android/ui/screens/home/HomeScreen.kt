package me.animeheaven.android.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.animeheaven.android.data.model.ContinueWatchingItem
import me.animeheaven.android.data.model.FeedViewMode
import me.animeheaven.android.ui.components.AhErrorBox
import me.animeheaven.android.ui.components.AhHorizontalPadding
import me.animeheaven.android.ui.components.AhLoadingBox
import me.animeheaven.android.ui.components.ContinueWatchingRow
import me.animeheaven.android.ui.components.HeroBannerPager
import me.animeheaven.android.ui.components.MobileTopBar
import me.animeheaven.android.ui.components.PopularHorizontalStrip
import me.animeheaven.android.ui.components.ScheduleGridCard
import me.animeheaven.android.ui.components.ScheduleListCard
import me.animeheaven.android.ui.components.SectionHeaderWithToggle
import me.animeheaven.android.ui.components.toGridRows

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenAnime: (String) -> Unit,
    onPlayContinueItem: (ContinueWatchingItem) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPopular: () -> Unit,
    onOpenSeasonal: (path: String, label: String) -> Unit,
    onOpenRandom: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val feed = state.feed

    Column(modifier = modifier.fillMaxSize()) {
        MobileTopBar(
            seasonLabel = feed?.seasonLabel,
            onLogoClick = { viewModel.loadHome(forceRefresh = true) },
            onSeasonalClick = {
                val path = feed?.seasonPath ?: "2025fall.php"
                val label = feed?.seasonLabel ?: "Season"
                onOpenSeasonal(path, label)
            },
            onSearchClick = onOpenSearch,
            onRandomClick = onOpenRandom,
            onSettingsClick = onOpenSettings,
            headerTitle = "For You",
            showFilterPills = true,
            onPopularClick = onOpenPopular
        )

        PullToRefreshBox(
            isRefreshing = state.isRefreshing && feed != null,
            onRefresh = { viewModel.loadHome(forceRefresh = true) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                state.isLoading && feed == null -> AhLoadingBox()
                state.errorMessage != null && feed == null -> {
                    AhErrorBox(
                        errorMessage = state.errorMessage ?: "Network error",
                        onRetry = { viewModel.loadHome(forceRefresh = true) }
                    )
                }
                feed != null -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val gridColumns = when {
                            maxWidth >= 840.dp -> 4
                            maxWidth >= 600.dp -> 3
                            else -> 2
                        }
                        val listState = rememberLazyListState(
                            prefetchStrategy = remember { LazyListPrefetchStrategy(nestedPrefetchItemCount = 4) }
                        )
                        val gridRows = remember(feed.scheduleItems, gridColumns) {
                            feed.scheduleItems.toGridRows(gridColumns) { it.id }
                        }

                        // Warm upcoming schedule details & covers smoothly as user scrolls
                        LaunchedEffect(listState, feed.scheduleItems, state.viewMode, gridColumns) {
                            snapshotFlow {
                                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            }
                                .map { lastIdx ->
                                    val approxScheduleIdx = if (state.viewMode == FeedViewMode.LIST) {
                                        (lastIdx - 3).coerceAtLeast(0)
                                    } else {
                                        ((lastIdx - 3).coerceAtLeast(0)) * gridColumns
                                    }
                                    approxScheduleIdx
                                }
                                .distinctUntilChanged()
                                .collect { startIdx ->
                                    if (startIdx < feed.scheduleItems.size) {
                                        val end = minOf(startIdx + 6, feed.scheduleItems.size)
                                        viewModel.onVisibleScheduleTail(feed.scheduleItems.subList(startIdx, end))
                                    }
                                }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 10.dp, bottom = 18.dp)
                        ) {
                            if (feed.banners.isNotEmpty()) {
                                item(key = "hero_banner", contentType = "hero_banner") {
                                    HeroBannerPager(
                                        banners = feed.banners,
                                        onBannerClick = onOpenAnime
                                    )
                                }
                            }

                            if (state.continueWatching.isNotEmpty()) {
                                item(key = "continue_watching", contentType = "continue_watching") {
                                    ContinueWatchingRow(
                                        items = state.continueWatching,
                                        onPlayItem = onPlayContinueItem
                                    )
                                }
                            }

                            if (feed.popularItems.isNotEmpty()) {
                                item(key = "popular_strip", contentType = "popular_strip") {
                                    PopularHorizontalStrip(
                                        items = feed.popularItems,
                                        onAnimeClick = onOpenAnime,
                                        onMoreClick = onOpenPopular
                                    )
                                }
                            }

                            item(key = "schedule_header", contentType = "section_header") {
                                SectionHeaderWithToggle(
                                    title = "Subbed Anime Schedule",
                                    viewMode = state.viewMode,
                                    onChangeViewMode = viewModel::setViewMode
                                )
                            }

                            if (state.viewMode == FeedViewMode.LIST) {
                                items(
                                    items = feed.scheduleItems,
                                    key = { it.id },
                                    contentType = { "schedule_list_card" }
                                ) { card ->
                                    ScheduleListCard(
                                        item = card,
                                        isBookmarked = state.bookmarkedIds.contains(card.id),
                                        onClick = { onOpenAnime(card.id) },
                                        onToggleBookmark = { viewModel.toggleBookmark(card) }
                                    )
                                }
                            } else {
                                items(
                                    items = gridRows,
                                    key = { it.key },
                                    contentType = { "schedule_grid_row" }
                                ) { row ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = AhHorizontalPadding, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        row.items.forEach { card ->
                                            ScheduleGridCard(
                                                item = card,
                                                isBookmarked = state.bookmarkedIds.contains(card.id),
                                                onClick = { onOpenAnime(card.id) },
                                                onToggleBookmark = { viewModel.toggleBookmark(card) },
                                                modifier = Modifier.weight(1f)
                                            )
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
    }
}
