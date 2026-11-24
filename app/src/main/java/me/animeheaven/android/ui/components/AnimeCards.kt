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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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

private val HERO_BILLBOARD_SCRIM = Brush.verticalGradient(
    0.0f to Color.Transparent,
    0.42f to Color(0x28000000),
    0.72f to Color(0xCC0B0B0B),
    1.0f to Color(0xF50B0B0B)
)
private val CARD_BOTTOM_SCRIM = Brush.verticalGradient(
    0.0f to Color.Transparent,
    0.35f to Color(0x55000000),
    0.72f to Color(0xCC000000),
    1.0f to Color(0xF5000000)
)
private val RANK_BADGE_SHAPE = RoundedCornerShape(5.dp)
private val TOP10_BADGE_SHAPE = RoundedCornerShape(bottomStart = 6.dp)

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

/**
 * Iconic Netflix Mobile Tall Framed Hero Billboard:
 * Large rounded featured poster card with centered A SERIES branding, bold title,
 * dot-separated contextual tags, and dual White Play + Frosted Dark More Info CTAs.
 */
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = AhHorizontalPadding),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxWidth()
                .height(352.dp)
        ) { page ->
            val banner = banners[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(AhShape16)
                    .background(palette.surface)
                    .border(1.dp, Color(0x33FFFFFF), AhShape16)
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
                        .fillMaxSize()
                        .background(HERO_BILLBOARD_SCRIM)
                )

                // Top-left featured rank pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(AhShape4)
                        .background(palette.crimson)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "#${page + 1} FEATURED",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.7.sp
                    )
                }

                // Bottom centered Netflix editorial stack + dual CTA buttons
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "A",
                            color = palette.crimson,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "S E R I E S",
                            color = Color(0xFFDCDCDC),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.2.sp
                        )
                    }

                    Text(
                        text = banner.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Trending Now  •  Subbed Anime  •  HD",
                        color = Color(0xFFD4D4D4),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Signature Netflix Mobile Dual Buttons: White Play + Translucent Dark Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(AhShape6)
                                .background(Color.White)
                                .clickable { onBannerClick(banner.animeId) }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Play",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(AhShape6)
                                .background(Color(0xB8303030))
                                .border(1.dp, Color(0x2EFFFFFF), AhShape6)
                                .clickable { onBannerClick(banner.animeId) }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AhIcons.BookmarkBorder,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Details",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                            .height(4.dp)
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
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Continue Watching",
            color = palette.textPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
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
                        .width(196.dp)
                        .clip(AhShape10)
                        .background(palette.surface)
                        .border(1.dp, Color(0x1CFFFFFF), AhShape10)
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
                                .background(CARD_BOTTOM_SCRIM)
                        )

                        // Netflix centered frosted play circle
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xA6000000))
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .clip(AhShape4)
                                .background(if (item.isNextEpisode) palette.crimson else Color(0xCC141414))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (item.isNextEpisode) {
                                    "UP NEXT • ${item.targetEpisodeLabel}"
                                } else {
                                    "EP ${item.targetEpisodeLabel}"
                                },
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (!item.isNextEpisode && item.durationMs > 0L) {
                            val fraction = (item.resumePositionMs.toFloat() / item.durationMs.toFloat())
                                .coerceIn(0.04f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.5.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(Color(0x55FFFFFF))
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

                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = item.animeTitle,
                            color = palette.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
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
                            color = palette.textMuted,
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

/**
 * Iconic Netflix Mobile "Top 10 Popular Today" swimlane:
 * Large stylized rank numerals nestled beside 2:3 poster cards with red TOP 10 corner ribbons.
 */
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
            .padding(top = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AhHorizontalPadding, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(AhShape4)
                        .background(palette.crimson)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "TOP 10",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "Popular Today",
                    color = palette.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Row(
                modifier = Modifier
                    .clip(AhShape20)
                    .clickable(onClick = onMoreClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "See All",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "See All Popular",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(
                start = AhHorizontalPadding,
                end = AhHorizontalPadding,
                bottom = 8.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { index, it -> "anime_${index}_${it.id}" },
                contentType = { _, _ -> "popular_card" }
            ) { index, anime ->
                val rank = index + 1
                Row(verticalAlignment = Alignment.Bottom) {
                    if (rank <= 10) {
                        Text(
                            text = rank.toString(),
                            color = if (palette.isLight) Color(0x38000000) else Color(0x5AFFFFFF),
                            fontSize = if (rank == 10) 58.sp else 70.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-3).sp,
                            modifier = Modifier.offset(x = 6.dp, y = 4.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(width = 114.dp, height = 164.dp)
                            .clip(AhShape8)
                            .background(palette.surfaceElevated)
                            .border(1.dp, Color(0x1EFFFFFF), AhShape8)
                            .clickable { onAnimeClick(anime.id) }
                    ) {
                        AhCoverImage(
                            url = anime.coverUrl,
                            contentDescription = anime.title,
                            targetSize = ImageTargetSize.POPULAR_CARD,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (rank <= 10) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .clip(TOP10_BADGE_SHAPE)
                                    .background(palette.crimson)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "TOP",
                                    color = Color.White,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 8.sp
                                )
                                Text(
                                    text = "10",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 10.sp
                                )
                            }
                        }
                        if (anime.title.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(CARD_BOTTOM_SCRIM)
                                    .padding(horizontal = 6.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = anime.title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
            .padding(horizontal = AhHorizontalPadding)
            .padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = palette.textPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
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
                        modifier = Modifier.size(17.dp)
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
                        modifier = Modifier.size(16.dp)
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
            .size(32.dp)
            .clip(CircleShape)
            .background(if (selected) Color.White else palette.surfaceElevated)
            .border(1.dp, if (selected) Color.White else Color(0x30FFFFFF), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon(if (selected) Color.Black else palette.textSecondary)
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
            .padding(horizontal = AhHorizontalPadding, vertical = 5.dp)
            .clip(AhShape10)
            .background(palette.surface)
            .border(1.dp, Color(0x16FFFFFF), AhShape10)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(128.dp)
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
                val badgeText = if (item.rankDirection != null) {
                    "#${item.episodeLabel}"
                } else {
                    "EP ${item.episodeLabel}"
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(5.dp)
                        .clip(AhShape4)
                        .background(palette.crimson)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
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
                fontWeight = FontWeight.Bold,
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
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isBookmarked) palette.crimson else palette.surfaceElevated)
                .border(1.dp, if (isBookmarked) palette.crimson else Color(0x30FFFFFF), CircleShape)
                .clickable(onClick = onToggleBookmark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isBookmarked) AhIcons.Bookmark else AhIcons.BookmarkBorder,
                contentDescription = if (isBookmarked) "Remove Bookmark" else "Bookmark",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
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
            .border(1.dp, Color(0x16FFFFFF), AhShape10)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            AhCoverImage(
                url = item.coverUrl,
                contentDescription = item.title,
                targetSize = ImageTargetSize.STANDARD_CARD,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CARD_BOTTOM_SCRIM)
            )
            if (item.episodeLabel.isNotBlank()) {
                val badgeText = if (item.rankDirection != null) {
                    "#${item.episodeLabel}"
                } else {
                    "EP ${item.episodeLabel}"
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(AhShape4)
                        .background(palette.crimson)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            if (isBookmarked || onToggleBookmark != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isBookmarked) palette.crimson else Color(0xA6000000))
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
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.title,
                color = palette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            if (item.rankDirection != null || item.timerOrStatus.isNotBlank()) {
                RankOrTimerBadge(item = item)
            }
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
            .clip(AhShape8)
            .background(palette.surface)
            .border(1.dp, Color(0x16FFFFFF), AhShape8)
            .clickable(onClick = onClick)
    ) {
        AhCoverImage(
            url = item.coverUrl,
            contentDescription = item.title,
            targetSize = ImageTargetSize.STANDARD_CARD,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )
        Text(
            text = item.title,
            color = palette.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp)
                .height(34.dp)
        )
    }
}

@Composable
private fun RankOrTimerBadge(item: AnimeCard) {
    val palette = LocalAhPalette.current
    if (item.rankDirection != null) {
        val badgeColor = when (item.rankDirection) {
            RankDirection.UP -> palette.positiveGreen
            RankDirection.SAME -> palette.textMuted
            RankDirection.DOWN -> palette.crimson
        }
        val arrow = when (item.rankDirection) {
            RankDirection.UP -> "▲"
            RankDirection.DOWN -> "▼"
            RankDirection.SAME -> "•"
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(badgeColor.copy(alpha = 0.2f), RANK_BADGE_SHAPE),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = arrow,
                    color = badgeColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = item.rankDelta.orEmpty(),
                color = palette.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else if (item.timerOrStatus.isNotBlank()) {
        Text(
            text = item.timerOrStatus,
            color = palette.positiveGreen,
            fontSize = 11.sp,
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
                .border(1.dp, palette.borderSubtle, AhShape12)
                .padding(24.dp),
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
                    .clip(AhShape6)
                    .background(Color.White)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Try Again",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
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
