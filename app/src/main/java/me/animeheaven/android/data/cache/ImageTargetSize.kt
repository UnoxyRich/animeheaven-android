package me.animeheaven.android.data.cache

/**
 * Canonical Coil decode dimensions shared by both [AnimePreloader] and Compose mobile UI cards
 * so preloaded artwork produces instant 0ms RAM cache hits.
 */
enum class ImageTargetSize(val widthPx: Int, val heightPx: Int) {
    HERO_BANNER(1280, 800),
    DETAIL_HERO(1280, 720),
    DETAIL_POSTER(360, 540),
    CONTINUE_CARD(480, 270),
    POPULAR_CARD(340, 510),
    STANDARD_CARD(480, 270),
    SEARCH_THUMB(256, 144)
}
