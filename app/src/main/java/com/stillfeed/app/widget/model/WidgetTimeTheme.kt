package com.stillfeed.app.widget.model

/**
 * Diurnal time and atmospheric themes matching Figma Canvas `01 — Widgets` (`214:2`).
 *
 * "The environment changes with time. The companion changes with real attention."
 */
enum class WidgetTimeTheme {
    /** 06:00 - 11:59: Warm morning cream (#FAF2D1), radiant sun with 8 rays, soft clouds, blooming companion */
    MORNING,

    /** 12:00 - 15:59: Fresh daytime sky (#D3EFE8), dual cloud formations, lush sage hills */
    DAY,

    /** 16:00 - 18:59: Warm afternoon peach (#F9D6BF), amber sun, terracotta hills */
    AFTERNOON,

    /** 19:00 - 21:59: Twilight lavender (#BAA3D8), setting sun, mindful companion with gentle brows */
    EVENING,

    /** 22:00 - 05:59: Midnight deep navy (#112335), golden full moon, 5 stars, sleeping companion */
    NIGHT
}
