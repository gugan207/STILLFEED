package com.stillfeed.app.widget.model

/**
 * Widget size classifications matching Figma Canvas `01 — Widgets`:
 * - COMPACT: 250 × 190 dp (Figma Node `216:2`) -> 2x2 or 3x2 on launcher
 * - MEDIUM: 430 × 190 dp (Figma Nodes `216:33`, `216:58`) -> 4x2 on launcher
 * - LARGE: 360 × 360 dp (Figma Nodes `216:81`, `216:107`) -> 3x3, 4x3, or 4x4 on launcher
 */
enum class WidgetSizeCategory {
    COMPACT,
    MEDIUM,
    LARGE
}
