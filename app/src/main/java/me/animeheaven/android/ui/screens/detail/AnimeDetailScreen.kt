package me.animeheaven.android.ui.screens.detail

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import me.animeheaven.android.ui.components.AhShape12
import me.animeheaven.android.ui.components.AhShape20
import me.animeheaven.android.ui.components.AhShape4
import me.animeheaven.android.ui.components.AhShape8
import me.animeheaven.android.ui.components.SimilarShowCard
import me.animeheaven.android.ui.components.toGridRows
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
                    listOf(Color(0x88000000), Color(0x55000000), palette.background)
                )
            }
            val coverForPlayer = detail.bannerUrl ?: detail.posterUrl
            val listState = rememberLazyListState(
                prefetchStrategy = remember { LazyListPrefetchStrategy(nestedPrefetchItemCount = 4) }
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
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
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    // Hero Banner with Status-Bar Safe Top Actions
                    item(key = "banner", contentType = "detail_banner") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        ) {
                            if (!detail.bannerUrl.isNullOrBlank()) {
                                AhCoverImage(
                                    url = detail.bannerUrl,
                                    contentDescription = detail.title,
                                    targetSize = ImageTargetSize.HERO_BANNER,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AhCoverImage(
                                    url = detail.posterUrl,
                                    contentDescription = detail.title,
                                    targetSize = ImageTargetSize.DETAIL_POSTER,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(bannerScrim)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = AhHorizontalPadding, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xBB121212))
                                        .border(1.dp, Color(0x44FFFFFF), CircleShape)
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

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (state.isBookmarked) palette.crimson else Color(0xBB121212))
                                        .border(1.5.dp, palette.crimson, CircleShape)
                                        .clickable(onClick = viewModel::toggleBookmark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (state.isBookmarked) AhIcons.Bookmark else AhIcons.BookmarkBorder,
                                        contentDescription = if (state.isBookmarked) "Bookmarked" else "Bookmark",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Main Info Card
                    item(key = "info_card") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AhHorizontalPadding)
                                .clip(AhShape12)
                                .background(palette.surface)
                                .border(1.dp, palette.borderSubtle, AhShape12)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                AhCoverImage(
                                    url = detail.posterUrl,
                                    contentDescription = detail.title,
                                    targetSize = ImageTargetSize.DETAIL_POSTER,
                                    modifier = Modifier
                                        .width(114.dp)
                                        .aspectRatio(2f / 3f)
                                        .clip(AhShape8)
                                        .border(1.dp, palette.borderSubtle, AhShape8)
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = detail.title,
                                        color = palette.textPrimary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 25.sp
                                    )
                                    if (detail.japaneseTitle.isNotBlank()) {
                                        Text(
                                            text = detail.japaneseTitle,
                                            color = palette.textMuted,
                                            fontSize = 13.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        MetaStat("Episodes", detail.episodesLabel)
                                        MetaStat("Year", detail.year)
                                        MetaStat("Score", detail.score)
                                    }
                                }
                            }

                            // Primary Full-Width Watch/Resume/Next CTA
                            val action = state.primaryAction
                            if (action != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(AhShape10)
                                        .background(palette.crimson)
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
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = action.buttonLabel,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (detail.synopsis.isNotBlank()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(AhShape8)
                                        .clickable { synopsisExpanded = !synopsisExpanded }
                                        .padding(vertical = 2.dp),
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
                                            color = palette.crimson,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            imageVector = if (synopsisExpanded) AhIcons.ExpandLess else AhIcons.ExpandMore,
                                            contentDescription = null,
                                            tint = palette.crimson,
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
                                                .background(palette.crimson.copy(alpha = 0.16f))
                                                .border(1.dp, palette.crimson, AhShape20)
                                                .clickable { onOpenTag(tag) }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = tag,
                                                color = palette.textPrimary,
                                                fontSize = 12.sp,
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
                                    .padding(top = 10.dp)
                                    .clip(AhShape10)
                                    .background(palette.surface)
                                    .border(1.dp, palette.crimson.copy(alpha = 0.5f), AhShape10)
                                    .padding(vertical = 10.dp, horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = detail.countdownLabel,
                                    color = palette.crimson,
                                    fontSize = 14.sp,
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
                                    .padding(top = 16.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Episodes (${orderedEpisodes.size})",
                                    color = palette.textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier
                                        .clip(AhShape20)
                                        .background(palette.surface)
                                        .border(1.dp, palette.crimson, AhShape20)
                                        .clickable(onClick = viewModel::toggleSortOrder)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = AhIcons.SwapVert,
                                        contentDescription = null,
                                        tint = palette.crimson,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (state.sortOldestFirst) "Ep 1 First" else "Newest First",
                                        color = palette.textPrimary,
                                        fontSize = 12.sp,
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
                                    EpisodeSquareTile(
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
                                title = "Similar Shows",
                                shows = detail.similarShows,
                                onOpenAnime = onOpenAnime
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaStat(label: String, value: String) {
    val palette = LocalAhPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            color = palette.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = palette.crimson,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EpisodeSquareTile(
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
            .clip(AhShape4)
            .background(if (palette.isLight) Color(0xFFFDFDFF) else Color.Black)
            .border(1.5.dp, palette.crimson, AhShape4)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isWatched) "Watched" else "Ep",
                color = palette.textSecondary,
                fontSize = 10.sp,
                maxLines = 1
            )
            Text(
                text = episode.numberLabel,
                color = if (isWatched) palette.crimson else palette.textPrimary,
                fontSize = if (episode.numberLabel.length >= 4) 17.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = episode.releaseAge.ifBlank { " " },
                color = palette.textMuted,
                fontSize = 10.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }

        if (progressFraction > 0.02f && !isWatched) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color(0x55FFFFFF))
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
    Column(modifier = Modifier.padding(top = 18.dp)) {
        Text(
            text = title,
            color = palette.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = AhHorizontalPadding, vertical = 8.dp)
        )
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = AhHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = shows,
                key = { it.id },
                contentType = { "show_card" }
            ) { show ->
                SimilarShowCard(
                    item = show,
                    onClick = { onOpenAnime(show.id) },
                    modifier = Modifier.width(156.dp)
                )
            }
        }
    }
}
