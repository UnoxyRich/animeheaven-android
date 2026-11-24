package me.animeheaven.android.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowRgb565
import coil3.size.Precision
import kotlinx.coroutines.delay
import me.animeheaven.android.data.cache.ImageTargetSize
import me.animeheaven.android.data.model.AnimeCard
import me.animeheaven.android.data.model.BannerItem
import me.animeheaven.android.data.model.ContinueWatchingItem
import me.animeheaven.android.data.model.FeedViewMode
import me.animeheaven.android.data.model.RankDirection
import me.animeheaven.android.ui.theme.LocalAhPalette

private val HERO_BANNER_SCRIM = Brush.verticalGradient(
    listOf(Color.Transparent, Color(0x99000000), Color(0xEE000000))
)
private val RANK_BADGE_SHAPE = RoundedCornerShape(5.dp)

@Immutable
data class GridRow<T>(
    val key: String,
    val items: List<T>
)

fun <T> List<T>.toGridRows(columns: Int, keyOf: (T) -> String): List<GridRow<T>> {
    if (isEmpty()) return emptyList()
    val rows = ArrayList<GridRow<T>>((size + columns - 1) / columns)
    var i = 0
    while (i < size) {
        val end = minOf(i + columns, size)
        val slice = subList(i, end)
        val rowKey = buildString(columns * 8) {
            for (j in slice.indices) {
                if (j > 0) append('_')
                append(keyOf(slice[j]))
            }
        }
        rows.add(GridRow(key = rowKey, items = slice))
        i = end
    }
    return rows
}

@Composable
fun AhCoverImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    targetSize: ImageTargetSize = ImageTargetSize.STANDARD_CARD
) {
    val context = LocalContext.current
    val request = remember(url, targetSize) {
        ImageRequest.Builder(context)
            .data(url)
            .size(targetSize.widthPx, targetSize.heightPx)
            .precision(Precision.INEXACT)
            .allowRgb565(true)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        filterQuality = FilterQuality.Medium,
        modifier = modifier
    )
}

@Composable
fun HeroBannerPager(
    banners: List<BannerItem>,
    onBannerClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (banners.isEmpty()) return
    val palette = LocalAhPalette.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pagerState = rememberPagerState(pageCount = { banners.size })

    LaunchedEffect(banners, pagerState.isScrollInProgress, lifecycleOwner) {
        if (banners.size <= 1 || pagerState.isScrollInProgress) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(6500L)
                val next = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = AhHorizontalPadding),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxWidth()
                .height(188.dp)
        ) { page ->
            val banner = banners[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(AhShape12)
                    .background(palette.surface)
                    .border(1.dp, palette.borderSubtle, AhShape12)
                    .clickable { onBannerClick(banner.animeId) }
            ) {
                AhCoverImage(
                    url = banner.bannerImageUrl,
                    contentDescription = banner.title,
                    targetSize = ImageTargetSize.HERO_BANNER,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(HERO_BANNER_SCRIM)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(AhShape4)
                                .background(palette.crimson)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "FEATURED",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = banner.title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (banners.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                banners.forEachIndexed { idx, _ ->
                    val selected = idx == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .width(if (selected) 18.dp else 6.dp)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(if (selected) palette.crimson else palette.textMuted.copy(alpha = 0.45f))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContinueWatchingRow(
    items: List<ContinueWatchingItem>,
    onPlayItem: (ContinueWatchingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val palette = LocalAhPalette.current
    val rowState = rememberLazyListState(
        prefetchStrategy = remember { LazyListPrefetchStrategy(nestedPrefetchItemCount = 4) }
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(
            text = "Continue Watching",
            color = palette.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = AhHorizontalPadding, vertical = 6.dp)
        )

        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = AhHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = items,
                key = { "${it.animeId}_${it.targetEpisodeKey}" },
                contentType = { "continue_card" }
            ) { item ->
                Column(
                    modifier = Modifier
                        .width(205.dp)
                        .clip(AhShape10)
                        .background(palette.surface)
                        .border(1.dp, palette.borderSubtle, AhShape10)
                        .clickable { onPlayItem(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    ) {
                        AhCoverImage(
                            url = item.coverUrl,
                            contentDescription = item.animeTitle,
                            targetSize = ImageTargetSize.CONTINUE_CARD,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x33000000))
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .clip(AhShape4)
                                .background(
                                    if (item.isNextEpisode) palette.crimson else Color(0xDD121212)
                                )
                                .then(
                                    if (!item.isNextEpisode) {
                                        Modifier.border(1.dp, palette.crimson, AhShape4)
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (item.isNextEpisode) {
                                    "NEXT • ${item.targetEpisodeLabel}"
                                } else {
                                    "EP ${item.targetEpisodeLabel}"
                                },
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!item.isNextEpisode && item.durationMs > 0L) {
                            val fraction = (item.resumePositionMs.toFloat() / item.durationMs.toFloat())
                                .coerceIn(0.03f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(Color(0x88000000))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxHeight()
                                        .background(palette.crimson)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = item.animeTitle,
                            color = palette.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (item.isNextEpisode) {
                                "Finished ${item.lastWatchedLabel} • Up Next"
                            } else {
                                "${formatDurationMs(item.resumePositionMs)} / ${formatDurationMs(item.durationMs)}"
                            },
                            color = palette.crimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PopularHorizontalStrip(
    items: List<AnimeCard>,
    onAnimeClick: (String) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val palette = LocalAhPalette.current
    val rowState = rememberLazyListState(
        prefetchStrategy = remember { LazyListPrefetchStrategy(nestedPrefetchItemCount = 5) }
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AhHorizontalPadding, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Popular Today",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .clip(AhShape20)
                    .clickable(onClick = onMoreClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "See All",
                    color = palette.crimson,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "See All Popular",
                    tint = palette.crimson,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = AhHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = items,
                key = { it.id },
                contentType = { "popular_card" }
            ) { anime ->
                Box(
                    modifier = Modifier
                        .size(width = 128.dp, height = 128.dp)
                        .clip(AhShape10)
                        .background(palette.surfaceElevated)
                        .border(1.dp, palette.borderSubtle, AhShape10)
                        .clickable { onAnimeClick(anime.id) }
                ) {
                    AhCoverImage(
                        url = anime.coverUrl,
                        contentDescription = anime.title,
                        targetSize = ImageTargetSize.POPULAR_CARD,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (anime.title.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color(0xC8000000))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = anime.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeaderWithToggle(
    title: String,
    viewMode: FeedViewMode?,
    onChangeViewMode: ((FeedViewMode) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val palette = LocalAhPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AhHorizontalPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = palette.textPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (viewMode != null && onChangeViewMode != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleToggleButton(
                    selected = viewMode == FeedViewMode.LIST,
                    onClick = { onChangeViewMode(FeedViewMode.LIST) }
                ) { tint ->
                    Icon(
                        imageVector = AhIcons.FormatListBulleted,
                        contentDescription = "List View",
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                CircleToggleButton(
                    selected = viewMode == FeedViewMode.GRID,
                    onClick = { onChangeViewMode(FeedViewMode.GRID) }
                ) { tint ->
                    Icon(
                        imageVector = AhIcons.GridView,
                        contentDescription = "Grid View",
                        tint = tint,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CircleToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit
) {
    val palette = LocalAhPalette.current
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (selected) palette.crimson else palette.surfaceElevated)
            .border(1.2.dp, if (selected) palette.crimson else palette.borderSubtle, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon(if (selected) Color.White else palette.textSecondary)
    }
}

@Composable
fun ScheduleListCard(
    item: AnimeCard,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAhPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AhHorizontalPadding, vertical = 4.dp)
            .clip(AhShape10)
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, AhShape10)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(126.dp)
                .aspectRatio(16f / 9f)
                .clip(AhShape8)
        ) {
            AhCoverImage(
                url = item.coverUrl,
                contentDescription = item.title,
                targetSize = ImageTargetSize.STANDARD_CARD,
                modifier = Modifier.fillMaxSize()
            )
            if (item.episodeLabel.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(palette.crimson)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Ep ${item.episodeLabel}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = item.title,
                color = palette.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.japaneseTitle.isNotBlank()) {
                Text(
                    text = item.japaneseTitle,
                    color = palette.textMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (item.rankDirection != null || item.timerOrStatus.isNotBlank()) {
                RankOrTimerBadge(item = item)
            }
        }

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isBookmarked) palette.crimson else palette.surfaceElevated)
                .border(1.2.dp, palette.crimson, CircleShape)
                .clickable(onClick = onToggleBookmark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isBookmarked) AhIcons.Bookmark else AhIcons.BookmarkBorder,
                contentDescription = if (isBookmarked) "Remove Bookmark" else "Bookmark",
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun ScheduleGridCard(
    item: AnimeCard,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalAhPalette.current
    Column(
        modifier = modifier
            .clip(AhShape10)
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, AhShape10)
            .clickable(onClick = onClick)
            .padding(7.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(AhShape8)
        ) {
            AhCoverImage(
                url = item.coverUrl,
                contentDescription = item.title,
                targetSize = ImageTargetSize.STANDARD_CARD,
                modifier = Modifier.fillMaxSize()
            )
            if (item.episodeLabel.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(palette.crimson)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.episodeLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (isBookmarked || onToggleBookmark != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isBookmarked) palette.crimson else Color(0x99000000))
                        .then(
                            if (onToggleBookmark != null) Modifier.clickable(onClick = onToggleBookmark)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBookmarked) AhIcons.Bookmark else AhIcons.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            color = palette.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        if (item.japaneseTitle.isNotBlank()) {
            Text(
                text = item.japaneseTitle,
                color = palette.textMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (item.rankDirection != null || item.timerOrStatus.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            RankOrTimerBadge(item = item)
        }
    }
}

@Composable
fun SimilarShowCard(
    item: AnimeCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAhPalette.current
    Column(
        modifier = modifier
            .clip(AhShape10)
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, AhShape10)
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        AhCoverImage(
            url = item.coverUrl,
            contentDescription = item.title,
            targetSize = ImageTargetSize.STANDARD_CARD,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(AhShape8)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            color = palette.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        )
    }
}

@Composable
private fun RankOrTimerBadge(item: AnimeCard) {
    val palette = LocalAhPalette.current
    if (item.rankDirection != null) {
        val badgeColor = when (item.rankDirection) {
            RankDirection.UP -> palette.crimson
            RankDirection.SAME -> palette.crimson
            RankDirection.DOWN -> palette.surfaceElevated
        }
        val arrow = when (item.rankDirection) {
            RankDirection.UP -> "▲"
            RankDirection.DOWN -> "▼"
            RankDirection.SAME -> "•"
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(badgeColor, RANK_BADGE_SHAPE),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = arrow,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = item.rankDelta.orEmpty(),
                color = palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else if (item.timerOrStatus.isNotBlank()) {
        Text(
            text = item.timerOrStatus,
            color = palette.crimson,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AhLoadingBox(
    message: String = "Loading AnimeHeaven...",
    modifier: Modifier = Modifier
) {
    val palette = LocalAhPalette.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = palette.crimson)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = palette.textSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun AhErrorBox(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAhPalette.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(AhShape12)
                .background(palette.surface)
                .border(1.dp, palette.crimson, AhShape12)
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Unable to load content",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = palette.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clip(AhShape8)
                    .background(palette.crimson)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Retry",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000L
    val hours = totalSec / 3600L
    val minutes = (totalSec % 3600L) / 60L
    val seconds = totalSec % 60L
    return buildString(8) {
        if (hours > 0) {
            append(hours)
            append(':')
            if (minutes < 10) append('0')
            append(minutes)
            append(':')
            if (seconds < 10) append('0')
            append(seconds)
        } else {
            append(minutes)
            append(':')
            if (seconds < 10) append('0')
            append(seconds)
        }
    }
}
