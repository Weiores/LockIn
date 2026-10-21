package com.lockin.app.domain.model

/**
 * Accent family for the whole app. Each scheme is a single hue rendered with
 * low saturation, so the glass reads as tinted rather than neon.
 */
enum class AccentScheme(val label: String) {
    BLUE("Blue"),
    GREEN("Green"),
    RED("Red"),
    AMBER("Amber"),
    VIOLET("Violet"),
    TEAL("Teal"),
}

/** Whether the app follows the system appearance or is pinned to a mode. */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}
