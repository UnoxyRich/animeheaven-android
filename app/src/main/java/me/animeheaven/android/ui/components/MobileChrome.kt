package me.animeheaven.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.animeheaven.android.ui.theme.LocalAhPalette

val AhHorizontalPadding = 16.dp
val AhShape4: Shape = RoundedCornerShape(4.dp)
val AhShape6: Shape = RoundedCornerShape(6.dp)
val AhShape8: Shape = RoundedCornerShape(8.dp)
val AhShape10: Shape = RoundedCornerShape(10.dp)
val AhShape12: Shape = RoundedCornerShape(12.dp)
val AhShape16: Shape = RoundedCornerShape(16.dp)
val AhShape20: Shape = RoundedCornerShape(24.dp)

enum class TopTabDestination(
    val label: String,
    val icon: ImageVector
) {
    HOME("Schedule", AhIcons.CalendarMonth),
    NEW("New", AhIcons.AutoAwesome),
    POPULAR("Popular", AhIcons.Whatshot),
    BOOKMARKS("Bookmarks", AhIcons.Bookmarks)
}

/**
 * 2025/2026 Netflix Mobile Top Bar:
 * - Signature condensed red "A" monogram + "For You" editorial title
 * - Clean Search, Shuffle, and Settings actions
 * - Frosted capsule filter pills strip underneath (Seasonal, Surprise Me)
 */
@Composable
fun MobileTopBar(
    seasonLabel: String?,
    onLogoClick: () -> Unit,
    onSeasonalClick: () -> Unit,
    onSearchClick: () -> Unit,
    onRandomClick: () -> Unit,
    onSettingsClick: () -> Unit,
    headerTitle: String = "For You",
    showFilterPills: Boolean = true,
    onPopularClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalAhPalette.current
    val topBrush = if (palette.isLight) {
        Brush.verticalGradient(listOf(palette.surface, palette.background))
    } else {
        Brush.verticalGradient(listOf(Color(0xEE0B0B0B), Color(0xBB0B0B0B), Color.Transparent))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(topBrush)
            .statusBarsPadding()
            .padding(bottom = if (showFilterPills) 8.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AhHorizontalPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Netflix-style Red "A" Monogram + Context Title
            Row(
                modifier = Modifier
                    .clip(AhShape8)
                    .clickable(onClick = onLogoClick)
                    .padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "A",
                    color = palette.crimson,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = headerTitle,
                    color = palette.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Frosted Search pill (keeps "Search" text accessible for UI tests & thumb ergonomics)
            Row(
                modifier = Modifier
                    .clip(AhShape20)
                    .background(if (palette.isLight) palette.surfaceElevated else Color(0x2BFFFFFF))
                    .border(
                        1.dp,
                        if (palette.isLight) palette.borderSubtle else Color(0x2EFFFFFF),
                        AhShape20
                    )
                    .clickable(onClick = onSearchClick)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Search",
                    color = palette.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            TopBarIconButton(
                icon = AhIcons.Tune,
                contentDescription = "Settings & Themes",
                onClick = onSettingsClick
            )
        }

        if (showFilterPills) {
            // Netflix Mobile Horizontal Glass Category Pills (Home only)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = AhHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!seasonLabel.isNullOrBlank()) {
                    NetflixFilterPill(
                        label = seasonLabel,
                        onClick = onSeasonalClick
                    )
                }
                if (onPopularClick != null) {
                    NetflixFilterPill(
                        label = "Top 10",
                        icon = AhIcons.Whatshot,
                        onClick = onPopularClick
                    )
                }
                NetflixFilterPill(
                    label = "Surprise Me",
                    icon = AhIcons.Shuffle,
                    onClick = onRandomClick
                )
            }
        }
    }
}

@Composable
private fun NetflixFilterPill(
    label: String,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val palette = LocalAhPalette.current
    Row(
        modifier = Modifier
            .clip(AhShape20)
            .background(if (palette.isLight) palette.surfaceElevated else Color(0x1FFFFFFF))
            .border(
                width = 1.dp,
                color = if (palette.isLight) palette.borderSubtle else Color(0x3DFFFFFF),
                shape = AhShape20
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.textPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label,
            color = palette.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
            .size(36.dp)
            .clip(CircleShape)
            .background(if (palette.isLight) palette.surfaceElevated else Color(0x24FFFFFF))
            .border(
                1.dp,
                if (palette.isLight) palette.borderSubtle else Color(0x2EFFFFFF),
                CircleShape
            )
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

/**
 * Authentic Netflix Mobile Bottom Navigation Bar:
 * Deep matte #121212 chrome with pure white active icon/label and muted gray inactive items.
 */
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
            .background(if (palette.isLight) palette.surface else Color(0xF5121212))
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
                .background(if (palette.isLight) palette.borderSubtle else Color(0x22FFFFFF))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopTabDestination.entries.forEach { tab ->
                val selected = tab == activeTab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(AhShape8)
                        .heightIn(min = 48.dp)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelectTab(tab) })
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = if (selected) palette.textPrimary else palette.textMuted,
                        modifier = Modifier.size(23.dp)
                    )
                    Text(
                        text = tab.label,
                        color = if (selected) palette.textPrimary else palette.textMuted,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun MobileNavigationRail(activeTab: TopTabDestination, onSelectTab: (TopTabDestination) -> Unit) {
    val palette = LocalAhPalette.current
    NavigationRail(containerColor = palette.surface) {
        TopTabDestination.entries.forEach { tab ->
            NavigationRailItem(
                selected = activeTab == tab,
                onClick = { onSelectTab(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) }
            )
        }
    }
}
