package me.animeheaven.android.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import me.animeheaven.android.data.model.AhThemeId

@Immutable
data class AhThemePalette(
    val themeId: AhThemeId,
    val background: Color,
    val backgroundBrush: Brush?,
    val surface: Color,
    val surfaceElevated: Color,
    val surfacePressed: Color,
    val crimson: Color,
    val crimsonDark: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderSubtle: Color,
    val positiveGreen: Color,
    val isLight: Boolean
)

private val CACHED_PALETTES: Array<AhThemePalette> by lazy {
    Array(AhThemeId.entries.size) { idx -> buildPaletteFor(AhThemeId.entries[idx]) }
}

private val CACHED_COLOR_SCHEMES: Array<ColorScheme> by lazy {
    Array(AhThemeId.entries.size) { idx ->
        val p = CACHED_PALETTES[idx]
        if (p.isLight) {
            lightColorScheme(
                primary = p.crimson,
                onPrimary = Color.White,
                background = p.background,
                onBackground = p.textPrimary,
                surface = p.surface,
                onSurface = p.textPrimary,
                surfaceVariant = p.surfaceElevated,
                onSurfaceVariant = p.textSecondary
            )
        } else {
            darkColorScheme(
                primary = p.crimson,
                onPrimary = Color.White,
                background = p.background,
                onBackground = p.textPrimary,
                surface = p.surface,
                onSurface = p.textPrimary,
                surfaceVariant = p.surfaceElevated,
                onSurfaceVariant = p.textSecondary
            )
        }
    }
}

val LocalAhPalette = staticCompositionLocalOf { paletteFor(AhThemeId.DARK) }

fun paletteFor(themeId: AhThemeId): AhThemePalette = CACHED_PALETTES[themeId.ordinal]

private fun buildPaletteFor(themeId: AhThemeId): AhThemePalette {
    val crimson = Color(0xFFDD0021)
    val crimsonDark = Color(0xFF9E0017)
    val green = Color(0xFF28C76F)

    return when (themeId) {
        AhThemeId.DARK -> AhThemePalette(
            themeId = themeId,
            background = Color(0xFF000000),
            backgroundBrush = null,
            surface = Color(0xFF121212),
            surfaceElevated = Color(0xFF1A1A1A),
            surfacePressed = Color(0xFF242424),
            crimson = crimson,
            crimsonDark = crimsonDark,
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFBDBDBD),
            textMuted = Color(0xFF8A8A8A),
            borderSubtle = Color(0xFF262626),
            positiveGreen = green,
            isLight = false
        )
        AhThemeId.DARK_INDIGO -> gradientDarkPalette(
            themeId, Color(0xFF2C1231), Color(0xFF052031), crimson, crimsonDark, green
        )
        AhThemeId.DARK_GRAPHITE -> gradientDarkPalette(
            themeId, Color(0xFF232526), Color(0xFF414345), crimson, crimsonDark, green
        )
        AhThemeId.DARK_TEAL_PLUM -> gradientDarkPalette(
            themeId, Color(0xFF01534E), Color(0xFF5A1C4C), crimson, crimsonDark, green
        )
        AhThemeId.DARK_CRIMSON_VELVET -> gradientDarkPalette(
            themeId, Color(0xFF800000), Color(0xFF5A1C4C), crimson, crimsonDark, green
        )
        AhThemeId.DARK_MAGENTA -> gradientDarkPalette(
            themeId, Color(0xFFAA076B), Color(0xFF61045F), crimson, crimsonDark, green
        )
        AhThemeId.DARK_TWILIGHT -> gradientDarkPalette(
            themeId, Color(0xFF536976), Color(0xFF292E49), crimson, crimsonDark, green
        )
        AhThemeId.LIGHT -> AhThemePalette(
            themeId = themeId,
            background = Color(0xFFFFFFFF),
            backgroundBrush = null,
            surface = Color(0xFFF3F3F8),
            surfaceElevated = Color(0xFFE7E7EE),
            surfacePressed = Color(0xFFDCDCE6),
            crimson = crimson,
            crimsonDark = crimsonDark,
            textPrimary = Color(0xFF1A1A1A),
            textSecondary = Color(0xFF333333),
            textMuted = Color(0xFF666666),
            borderSubtle = Color(0xFFD0D0DC),
            positiveGreen = Color(0xFF158A4A),
            isLight = true
        )
    }
}

private fun gradientDarkPalette(
    themeId: AhThemeId,
    start: Color,
    end: Color,
    crimson: Color,
    crimsonDark: Color,
    green: Color
): AhThemePalette = AhThemePalette(
    themeId = themeId,
    background = end,
    backgroundBrush = Brush.linearGradient(
        colors = listOf(start, end),
        start = Offset.Zero,
        end = Offset(1080f, 900f)
    ),
    surface = Color(0xAA0A0A0A),
    surfaceElevated = Color(0xCC141414),
    surfacePressed = Color(0xE6202020),
    crimson = crimson,
    crimsonDark = crimsonDark,
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFCFCFCF),
    textMuted = Color(0xFF9E9E9E),
    borderSubtle = Color(0x33FFFFFF),
    positiveGreen = green,
    isLight = false
)

private val AhMobileTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold
    ),
    headlineMedium = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold
    ),
    titleLarge = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    )
)

@Composable
fun AnimeHeavenTheme(
    themeId: AhThemeId = AhThemeId.DARK,
    content: @Composable () -> Unit
) {
    val palette = CACHED_PALETTES[themeId.ordinal]
    val colorScheme = CACHED_COLOR_SCHEMES[themeId.ordinal]

    CompositionLocalProvider(LocalAhPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AhMobileTypography
        ) {
            val bgModifier = if (palette.backgroundBrush != null) {
                Modifier.fillMaxSize().background(palette.backgroundBrush)
            } else {
                Modifier.fillMaxSize().background(palette.background)
            }
            Box(modifier = bgModifier) {
                content()
            }
        }
    }
}
