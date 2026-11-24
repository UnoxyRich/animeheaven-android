package me.animeheaven.android.data.cache

/**
 * Canonical Coil decode dimensions shared by both [AnimePreloader] and Compose mobile UI cards
 * so preloaded artwork produces instant 0ms RAM cache hits.
 */
enum class ImageTargetSize(val widthPx: Int, val heightPx: Int) {
    HERO_BANNER(1080, 460),
    DETAIL_POSTER(360, 540),
    CONTINUE_CARD(480, 270),
    POPULAR_CARD(320, 320),
    STANDARD_CARD(420, 236),
    SEARCH_THUMB(256, 144)
}
