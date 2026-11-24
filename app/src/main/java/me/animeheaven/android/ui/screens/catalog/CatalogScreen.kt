package me.animeheaven.android.ui.screens.catalog

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.animeheaven.android.data.model.FeedViewMode
import me.animeheaven.android.ui.components.AhErrorBox
import me.animeheaven.android.ui.components.AhHorizontalPadding
import me.animeheaven.android.ui.components.AhLoadingBox
import me.animeheaven.android.ui.components.MobileTopBar
import me.animeheaven.android.ui.components.ScheduleGridCard
import me.animeheaven.android.ui.components.ScheduleListCard
import me.animeheaven.android.ui.components.SectionHeaderWithToggle
import me.animeheaven.android.ui.components.SimilarShowCard
import me.animeheaven.android.ui.components.toGridRows
import me.animeheaven.android.ui.theme.LocalAhPalette

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    onOpenAnime: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSeasonal: (path: String, label: String) -> Unit,
    onOpenRandom: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalAhPalette.current
    val isTopTab = viewModel.kind == CatalogKind.NEW || viewModel.kind == CatalogKind.POPULAR
    val isTagGrid = viewModel.kind == CatalogKind.TAG

    Column(modifier = modifier.fillMaxSize()) {
        if (isTopTab) {
            MobileTopBar(
                seasonLabel = state.seasonLabel,
                onLogoClick = { viewModel.load(forceRefresh = true) },
                onSeasonalClick = {
                    val path = state.seasonPath ?: "2025fall.php"
                    val label = state.seasonLabel ?: "Season"
                    onOpenSeasonal(path, label)
                },
                onSearchClick = onOpenSearch,
                onRandomClick = onOpenRandom,
                onSettingsClick = onOpenSettings,
                headerTitle = state.title,
                showFilterPills = false
            )
        } else {
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
                            .clickable { onBack?.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = state.title.ifBlank { "Catalog" },
                        color = palette.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.borderSubtle)
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing && state.items.isNotEmpty(),
            onRefresh = { viewModel.load(forceRefresh = true) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(if (!isTopTab) Modifier.navigationBarsPadding() else Modifier)
        ) {
            when {
                state.isLoading && state.items.isEmpty() -> AhLoadingBox()
                state.errorMessage != null && state.items.isEmpty() -> {
                    AhErrorBox(
                        errorMessage = state.errorMessage ?: "Failed to load",
                        onRetry = { viewModel.load(forceRefresh = true) }
                    )
                }
                else -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val gridColumns = when {
                            maxWidth >= 840.dp -> 4
                            maxWidth >= 600.dp -> 3
                            else -> 2
                        }
                        val listState = rememberLazyListState(
                            prefetchStrategy = remember { LazyListPrefetchStrategy(nestedPrefetchItemCount = 4) }
                        )
                        val gridRows = remember(state.items, gridColumns) {
                            state.items.toGridRows(gridColumns) { it.id }
                        }

                        LaunchedEffect(listState, state.items, state.viewMode, isTagGrid, gridColumns) {
                            snapshotFlow {
                                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            }
                                .map { lastIdx ->
                                    if (!isTagGrid && state.viewMode == FeedViewMode.LIST) {
                                        lastIdx.coerceAtLeast(0)
                                    } else {
                                        (lastIdx * gridColumns).coerceAtLeast(0)
                                    }
                                }
                                .distinctUntilChanged()
                                .collect { startIdx ->
                                    if (startIdx < state.items.size) {
                                        val end = minOf(startIdx + 6, state.items.size)
                                        viewModel.onVisibleTail(state.items.subList(startIdx, end))
                                    }
                                }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 18.dp)
                        ) {
                            if (isTopTab || !isTagGrid) {
                                item(key = "header", contentType = "section_header") {
                                    val sectionTitle = when {
                                        !isTopTab -> state.title
                                        viewModel.kind == CatalogKind.POPULAR -> "Ranked by Views"
                                        else -> "Latest Releases"
                                    }
                                    SectionHeaderWithToggle(
                                        title = sectionTitle,
                                        viewMode = if (isTagGrid) null else state.viewMode,
                                        onChangeViewMode = if (isTagGrid) null else viewModel::setViewMode
                                    )
                                }
                            }

                            if (isTagGrid) {
                                items(
                                    items = gridRows,
                                    key = { it.key },
                                    contentType = { "tag_grid_row" }
                                ) { row ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = AhHorizontalPadding, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        row.items.forEach { card ->
                                            SimilarShowCard(
                                                item = card,
                                                onClick = { onOpenAnime(card.id) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        repeat(gridColumns - row.items.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            } else if (state.viewMode == FeedViewMode.LIST) {
                                items(
                                    items = state.items,
                                    key = { it.id },
                                    contentType = { "catalog_list_card" }
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
                                    contentType = { "catalog_grid_row" }
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
