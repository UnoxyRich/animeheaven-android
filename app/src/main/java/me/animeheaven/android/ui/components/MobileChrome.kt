package me.animeheaven.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.animeheaven.android.ui.theme.LocalAhPalette

val AhHorizontalPadding = 16.dp
val AhShape4: Shape = RoundedCornerShape(4.dp)
val AhShape8: Shape = RoundedCornerShape(8.dp)
val AhShape10: Shape = RoundedCornerShape(10.dp)
val AhShape12: Shape = RoundedCornerShape(12.dp)
val AhShape20: Shape = RoundedCornerShape(20.dp)

enum class TopTabDestination(
    val label: String,
    val icon: ImageVector
) {
    HOME("Schedule", AhIcons.CalendarMonth),
    NEW("New", AhIcons.AutoAwesome),
    POPULAR("Popular", AhIcons.Whatshot),
    BOOKMARKS("Bookmarks", AhIcons.Bookmarks)
}

@Composable
fun MobileTopBar(
    seasonLabel: String?,
    onLogoClick: () -> Unit,
    onSeasonalClick: () -> Unit,
    onSearchClick: () -> Unit,
    onRandomClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAhPalette.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AhHorizontalPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // AnimeHeaven AH Emblem
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(AhShape8)
                    .background(Color.Black)
                    .border(1.5.dp, palette.crimson, AhShape8)
                    .clickable(onClick = onLogoClick),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "A",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "H",
                        color = palette.crimson,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            if (!seasonLabel.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(AhShape20)
                        .background(palette.surfaceElevated)
                        .border(1.dp, palette.crimson.copy(alpha = 0.7f), AhShape20)
                        .clickable(onClick = onSeasonalClick)
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = seasonLabel,
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Compact Search Pill
            Row(
                modifier = Modifier
                    .clip(AhShape20)
                    .background(palette.surfaceElevated)
                    .border(1.dp, palette.borderSubtle, AhShape20)
                    .clickable(onClick = onSearchClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = palette.crimson,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Search",
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            TopBarIconButton(
                icon = AhIcons.Shuffle,
                contentDescription = "Random Anime",
                onClick = onRandomClick
            )
            TopBarIconButton(
                icon = AhIcons.Tune,
                contentDescription = "Settings & Themes",
                onClick = onSettingsClick
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

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val palette = LocalAhPalette.current
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(palette.surfaceElevated)
            .border(1.dp, palette.borderSubtle, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = palette.textPrimary,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
fun MobileBottomBar(
    activeTab: TopTabDestination,
    onSelectTab: (TopTabDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAhPalette.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.borderSubtle)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopTabDestination.entries.forEach { tab ->
                val selected = tab == activeTab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(AhShape10)
                        .clickable { onSelectTab(tab) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(AhShape20)
                            .background(if (selected) palette.crimson.copy(alpha = 0.16f) else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) palette.crimson else palette.textMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = tab.label,
                        color = if (selected) palette.crimson else palette.textMuted,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
