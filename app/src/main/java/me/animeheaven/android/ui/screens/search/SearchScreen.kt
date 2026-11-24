package me.animeheaven.android.ui.screens.search

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import me.animeheaven.android.data.cache.ImageTargetSize
import me.animeheaven.android.ui.components.AhCoverImage
import me.animeheaven.android.ui.components.AhErrorBox
import me.animeheaven.android.ui.components.AhHorizontalPadding
import me.animeheaven.android.ui.components.AhIcons
import me.animeheaven.android.ui.components.AhShape10
import me.animeheaven.android.ui.components.AhShape20
import me.animeheaven.android.ui.components.AhShape8
import me.animeheaven.android.ui.components.SimilarShowCard
import me.animeheaven.android.ui.components.toGridRows
import me.animeheaven.android.ui.theme.LocalAhPalette

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onOpenAnime: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalAhPalette.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(80L)
        runCatching { focusRequester.requestFocus() }
        keyboardController?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Native Mobile Search Bar Header
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(AhShape10)
                        .background(if (palette.isLight) Color.White else palette.surfaceElevated)
                        .border(1.dp, if (palette.isLight) palette.borderSubtle else Color(0x33FFFFFF), AhShape10)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (state.query.isEmpty()) {
                            Text(
                                text = "Search anime title...",
                                color = palette.textMuted,
                                fontSize = 15.sp,
                                maxLines = 1
                            )
                        }
                        BasicTextField(
                            value = state.query,
                            onValueChange = viewModel::updateQuery,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = palette.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(palette.crimson),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    viewModel.submitFullSearch()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = palette.crimson,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (state.query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(palette.surfaceElevated)
                                .clickable {
                                    viewModel.clearQuery()
                                    runCatching { focusRequester.requestFocus() }
                                    keyboardController?.show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            if (state.recentSearches.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(
                        start = AhHorizontalPadding,
                        end = AhHorizontalPadding,
                        bottom = 10.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item(key = "history_icon") {
                        Icon(
                            imageVector = AhIcons.History,
                            contentDescription = "Recent Searches",
                            tint = palette.crimson,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    items(
                        items = state.recentSearches,
                        key = { it },
                        contentType = { "recent_chip" }
                    ) { recent ->
                        Box(
                            modifier = Modifier
                                .clip(AhShape20)
                                .background(palette.surfaceElevated)
                                .border(1.dp, palette.borderSubtle, AhShape20)
                                .clickable {
                                    keyboardController?.hide()
                                    viewModel.applyRecentQuery(recent)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = recent,
                                color = palette.textSecondary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
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

        // Results Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (state.isFullSearchMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AhHorizontalPadding, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Results for \"${state.fullResultsTitle}\"",
                        color = palette.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.fastResults.isNotEmpty()) {
                        Text(
                            text = "Live List",
                            color = palette.crimson,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(AhShape20)
                                .clickable(onClick = viewModel::showLiveSuggestions)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            when {
                state.errorMessage != null -> {
                    AhErrorBox(
                        errorMessage = state.errorMessage ?: "Search error",
                        onRetry = viewModel::submitFullSearch
                    )
                }
                state.isFullSearchMode -> {
                    if (state.fullResults.isEmpty() && !state.isLoading) {
                        EmptySearchMessage("No anime found for \"${state.query.trim()}\"")
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val gridColumns = when {
                                maxWidth >= 840.dp -> 4
                                maxWidth >= 600.dp -> 3
                                else -> 2
                            }
                            val gridRows = remember(state.fullResults, gridColumns) {
                                state.fullResults.toGridRows(gridColumns) { it.id }
                            }
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 18.dp)
                            ) {
                                items(
                                    items = gridRows,
                                    key = { it.key },
                                    contentType = { "search_grid_row" }
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
                            }
                        }
                    }
                }
                state.query.trim().length < 2 -> {
                    EmptySearchMessage("Type at least 2 characters for instant matches,\nor press Search on your keyboard.")
                }
                state.fastResults.isEmpty() && !state.isLoading -> {
                    EmptySearchMessage("No instant matches.\nTap Search on your keyboard for full site search.")
                }
                else -> {
                    val fastListState = rememberLazyListState(
                        prefetchStrategy = remember { LazyListPrefetchStrategy(nestedPrefetchItemCount = 4) }
                    )
                    LazyColumn(
                        state = fastListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            horizontal = AhHorizontalPadding,
                            vertical = 12.dp
                        )
                    ) {
                        itemsIndexed(
                            items = state.fastResults,
                            key = { index, it -> "fast_${index}_${it.animeId}" },
                            contentType = { _, _ -> "fast_search_row" }
                        ) { _, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AhShape10)
                                    .background(palette.surface)
                                    .border(1.dp, palette.borderSubtle, AhShape10)
                                    .clickable {
                                        keyboardController?.hide()
                                        onOpenAnime(item.animeId)
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AhCoverImage(
                                    url = item.coverUrl,
                                    contentDescription = item.title,
                                    targetSize = ImageTargetSize.SEARCH_THUMB,
                                    modifier = Modifier
                                        .width(108.dp)
                                        .aspectRatio(16f / 9f)
                                        .clip(AhShape8)
                                )
                                Text(
                                    text = item.title,
                                    color = palette.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item(key = "show_all_btn", contentType = "show_all_btn") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .clip(AhShape10)
                                    .background(if (palette.isLight) palette.crimson else Color.White)
                                    .clickable {
                                        keyboardController?.hide()
                                        viewModel.submitFullSearch()
                                    }
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Show all results for \"${state.query.trim()}\"",
                                    color = if (palette.isLight) Color.White else Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
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
private fun EmptySearchMessage(text: String) {
    val palette = LocalAhPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = palette.textMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center
        )
    }
}
