package me.animeheaven.android.ui.screens.detail

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.animeheaven.android.data.cache.ImageTargetSize
import me.animeheaven.android.data.local.WatchProgressEntity
import me.animeheaven.android.data.model.AnimeCard
import me.animeheaven.android.data.model.EpisodeItem
import me.animeheaven.android.ui.components.AhCoverImage
import me.animeheaven.android.ui.components.AhErrorBox
import me.animeheaven.android.ui.components.AhHorizontalPadding
import me.animeheaven.android.ui.components.AhIcons
import me.animeheaven.android.ui.components.AhLoadingBox
import me.animeheaven.android.ui.components.AhShape10
import me.animeheaven.android.ui.components.AhShape20
import me.animeheaven.android.ui.components.AhShape4
import me.animeheaven.android.ui.components.AhShape6
import me.animeheaven.android.ui.components.AhShape8
import me.animeheaven.android.ui.components.SimilarShowCard
import me.animeheaven.android.ui.components.toGridRows
import me.animeheaven.android.ui.components.topSystemSafePadding
import me.animeheaven.android.ui.theme.LocalAhPalette

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AnimeDetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onPlayEpisode: (episodeKey: String, animeId: String, animeTitle: String, coverUrl: String) -> Unit,
    onOpenTag: (String) -> Unit,
    onOpenAnime: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalAhPalette.current
    val detail = state.detail

    when {
        state.isLoading && detail == null -> AhLoadingBox()
        state.errorMessage != null && detail == null -> {
            AhErrorBox(
                errorMessage = state.errorMessage ?: "Unable to load anime",
                onRetry = { viewModel.load(forceRefresh = true) }
            )
        }
        detail != null -> {
            var synopsisExpanded by remember(detail.id) { mutableStateOf(false) }
            val orderedEpisodes = remember(detail.episodes, state.sortOldestFirst) {
                if (state.sortOldestFirst) detail.episodes.reversed() else detail.episodes
            }
            val bannerScrim = remember(palette.background) {
                Brush.verticalGradient(
                    0.0f to Color(0x99000000),
                    0.45f to Color(0x33000000),
                    1.0f to palette.background
                )
            }
            val coverForPlayer = detail.bannerUrl ?: detail.posterUrl
            val listState = rememberLazyListState(
                prefetchStrategy = remember { LazyListPrefetchStrategy(nestedPrefetchItemCount = 4) }
            )
            val isBannerVisible by remember {
                derivedStateOf { listState.firstVisibleItemIndex == 0 }
            }
            val view = LocalView.current
            val activity = LocalActivity.current
            DisposableEffect(activity, view, palette.isLight, isBannerVisible) {
                val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
                controller?.isAppearanceLightStatusBars = palette.isLight && !isBannerVisible
                onDispose {
                    controller?.isAppearanceLightStatusBars = palette.isLight
                }
            }
            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val epColumns = when {
                    maxWidth >= 840.dp -> 8
                    maxWidth >= 600.dp -> 6
                    else -> 4
                }
                val epRows = remember(orderedEpisodes, epColumns) {
                    orderedEpisodes.toGridRows(epColumns) { it.key }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = navBarBottom + 32.dp)
                ) {
                    // Full-Bleed Netflix Mobile Hero Backdrop
                    item(key = "banner", contentType = "detail_banner") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        ) {
                            AhCoverImage(
                                url = detail.bannerUrl?.takeIf { it.isNotBlank() } ?: detail.posterUrl,
                                contentDescription = detail.title,
                                targetSize = ImageTargetSize.HERO_BANNER,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(bannerScrim)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .topSystemSafePadding()
                                    .padding(horizontal = AhHorizontalPadding, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xAA000000))
                                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                        .clickable(onClick = onBack),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Netflix Mobile Editorial Metadata & Stacked Full-Width CTAs
                    item(key = "info_card") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AhHorizontalPadding),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "A",
                                    color = palette.crimson,
                                    fontSize = 15.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "S E R I E S",
                                    color = palette.textSecondary,
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.2.sp
                                )
                            }

                            Text(
                                text = detail.title,
                                color = palette.textPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 29.sp
                            )

                            if (detail.japaneseTitle.isNotBlank()) {
                                Text(
                                    text = detail.japaneseTitle,
                                    color = palette.textMuted,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Netflix metadata strip: Score (green), Year, Episodes, HD box
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val normalizedScore = detail.score.trim()
                                if (normalizedScore.isNotEmpty() && normalizedScore != "?" && !normalizedScore.equals("N/A", ignoreCase = true)) {
                                    Text(
                                        text = "Score $normalizedScore",
                                        color = palette.positiveGreen,
                                        fontSize = 13.sp,
                                        lineHeight = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (detail.year.isNotBlank()) {
                                    Text(
                                        text = detail.year,
                                        color = palette.textSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                                if (detail.episodesLabel.isNotBlank()) {
                                    Text(
                                        text = "${detail.episodesLabel} Episodes",
                                        color = palette.textSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, palette.textPrimary.copy(alpha = 0.45f), AhShape4)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "HD",
                                        color = palette.textPrimary,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Primary Full-Width Play Button
                            val action = state.primaryAction
                            if (action != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(AhShape6)
                                        .background(palette.primaryActionContainer)
                                        .clickable {
                                            onPlayEpisode(
                                                action.episodeKey,
                                                detail.id,
                                                detail.title,
                                                coverForPlayer
                                            )
                                        }
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = palette.onPrimaryActionContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = action.buttonLabel,
                                        color = palette.onPrimaryActionContainer,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            // Secondary Full-Width My List Button
                            val myListContentColor = if (state.isBookmarked) Color.White else palette.textPrimary
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AhShape6)
                                    .background(if (state.isBookmarked) palette.crimson else palette.surfaceElevated)
                                    .border(
                                        1.dp,
                                        if (state.isBookmarked) palette.crimson else palette.borderSubtle,
                                        AhShape6
                                    )
                                    .clickable(onClick = viewModel::toggleBookmark)
                                    .padding(vertical = 11.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (state.isBookmarked) AhIcons.Bookmark else AhIcons.BookmarkBorder,
                                    contentDescription = if (state.isBookmarked) "In My List" else "My List",
                                    tint = myListContentColor,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.isBookmarked) "In My List" else "Add to My List",
                                    color = myListContentColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (detail.synopsis.isNotBlank()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(AhShape8)
                                        .clickable { synopsisExpanded = !synopsisExpanded }
                                        .padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = detail.synopsis,
                                        color = palette.textSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        maxLines = if (synopsisExpanded) Int.MAX_VALUE else 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = if (synopsisExpanded) "Show less" else "Read more",
                                            color = palette.textPrimary,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = if (synopsisExpanded) AhIcons.ExpandLess else AhIcons.ExpandMore,
                                            contentDescription = null,
                                            tint = palette.textPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            if (detail.tags.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    detail.tags.forEach { tag ->
                                        Box(
                                            modifier = Modifier
                                                .clip(AhShape20)
                                                .background(palette.surfaceElevated)
                                                .border(1.dp, palette.borderSubtle, AhShape20)
                                                .clickable { onOpenTag(tag) }
                                                .padding(horizontal = 11.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = tag,
                                                color = palette.textSecondary,
                                                fontSize = 12.sp,
                                                lineHeight = 15.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!detail.countdownLabel.isNullOrBlank()) {
                        item(key = "countdown") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AhHorizontalPadding)
                                    .padding(top = 12.dp)
                                    .clip(AhShape10)
                                    .background(palette.surface)
                                    .border(1.dp, palette.borderSubtle, AhShape10)
                                    .padding(vertical = 10.dp, horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = detail.countdownLabel,
                                    color = palette.positiveGreen,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (orderedEpisodes.isNotEmpty()) {
                        item(key = "episodes_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AhHorizontalPadding)
                                    .padding(top = 20.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .width(48.dp)
                                            .height(3.dp)
                                            .clip(CircleShape)
                                            .background(palette.crimson)
                                    )
                                    Text(
                                        text = "Episodes (${orderedEpisodes.size})",
                                        color = palette.textPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(AhShape20)
                                        .background(palette.surfaceElevated)
                                        .border(1.dp, palette.borderSubtle, AhShape20)
                                        .clickable(onClick = viewModel::toggleSortOrder)
                                        .padding(horizontal = 11.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = AhIcons.SwapVert,
                                        contentDescription = null,
                                        tint = palette.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (state.sortOldestFirst) "Ep 1 First" else "Newest First",
                                        color = palette.textPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        items(
                            items = epRows,
                            key = { it.key },
                            contentType = { "episode_row" }
                        ) { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AhHorizontalPadding, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.items.forEach { ep ->
                                    EpisodeNetflixTile(
                                        episode = ep,
                                        progress = state.progressMap[ep.key],
                                        onClick = {
                                            onPlayEpisode(
                                                ep.key,
                                                detail.id,
                                                detail.title,
                                                coverForPlayer
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(epColumns - row.items.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (detail.relatedShows.isNotEmpty()) {
                        item(key = "related_shows", contentType = "show_row") {
                            RelatedOrSimilarRow(
                                title = "Related Shows",
                                shows = detail.relatedShows,
                                onOpenAnime = onOpenAnime
                            )
                        }
                    }

                    if (detail.similarShows.isNotEmpty()) {
                        item(key = "similar_shows", contentType = "show_row") {
                            RelatedOrSimilarRow(
                                title = "More Like This",
                                shows = detail.similarShows,
                                onOpenAnime = onOpenAnime
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .background(palette.background.copy(alpha = 0.94f))
                )
            }
        }
    }
}

@Composable
private fun EpisodeNetflixTile(
    episode: EpisodeItem,
    progress: WatchProgressEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAhPalette.current
    val isWatched = progress?.isWatched == true
    val progressFraction = if (progress != null && progress.durationMs > 0L) {
        (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .height(78.dp)
            .clip(AhShape8)
            .background(if (isWatched) palette.surface else palette.surfaceElevated)
            .border(
                1.dp,
                if (isWatched) palette.positiveGreen.copy(alpha = 0.45f) else palette.borderSubtle,
                AhShape8
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isWatched) "WATCHED" else "EP",
                color = if (isWatched) palette.positiveGreen else palette.textMuted,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = episode.numberLabel,
                color = palette.textPrimary,
                fontSize = if (episode.numberLabel.length >= 4) 17.sp else 21.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = episode.releaseAge.ifBlank { " " },
                color = palette.textMuted,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }

        if (progressFraction > 0.02f && !isWatched) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .align(Alignment.BottomCenter)
                    .background(palette.textPrimary.copy(alpha = 0.16f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .fillMaxHeight()
                        .background(palette.crimson)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RelatedOrSimilarRow(
    title: String,
    shows: List<AnimeCard>,
    onOpenAnime: (String) -> Unit
) {
    val palette = LocalAhPalette.current
    val rowState = rememberLazyListState(
        prefetchStrategy = remember { LazyListPrefetchStrategy(nestedPrefetchItemCount = 4) }
    )
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            text = title,
            color = palette.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = AhHorizontalPadding, vertical = 8.dp)
        )
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = AhHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(
                items = shows,
                key = { index, it -> "anime_${index}_${it.id}" },
                contentType = { _, _ -> "show_card" }
            ) { _, show ->
                SimilarShowCard(
                    item = show,
                    onClick = { onOpenAnime(show.id) },
                    modifier = Modifier.width(156.dp)
                )
            }
        }
    }
}
