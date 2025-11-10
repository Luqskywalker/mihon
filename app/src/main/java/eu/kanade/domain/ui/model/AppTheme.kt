package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

enum class AppTheme(
    val titleRes: StringResource?,
    val isDeprecated: Boolean = false,
) {
    // Modern themes
    DEFAULT(MR.strings.label_default),
    MONET(MR.strings.theme_monet),
    CATPPUCCIN(MR.strings.theme_catppuccin),
    GREEN_APPLE(MR.strings.theme_greenapple),
    LAVENDER(MR.strings.theme_lavender),
    MIDNIGHT_DUSK(MR.strings.theme_midnightdusk),
    NORD(MR.strings.theme_nord),
    STRAWBERRY_DAIQUIRI(MR.strings.theme_strawberrydaiquiri),
    TAKO(MR.strings.theme_tako),
    TEALTURQUOISE(MR.strings.theme_tealturquoise),
    TIDAL_WAVE(MR.strings.theme_tidalwave),
    YINYANG(MR.strings.theme_yinyang),
    YOTSUBA(MR.strings.theme_yotsuba),
    MONOCHROME(MR.strings.theme_monochrome),

    // Deprecated themes (hidden from selection)
    DARK_BLUE(null, true),
    HOT_PINK(null, true),
    BLUE(null, true);

    companion object {
        val default = DEFAULT
        val availableThemes = entries.filterNot { it.isDeprecated }
        val deprecatedThemes = entries.filter { it.isDeprecated }

        /**
         * Gets the fallback theme when the current theme is deprecated or unavailable.
         */
        fun getFallback(current: AppTheme): AppTheme = when {
            !current.isDeprecated -> current
            else -> default
        }

        /**
         * Checks if a theme is available for selection in the UI.
         */
        fun isAvailable(theme: AppTheme): Boolean = !theme.isDeprecated

        /**
         * Gets themes grouped by category for UI presentation.
         */
        val groupedThemes: Map<String, List<AppTheme>> = mapOf(
            "Recommended" to listOf(DEFAULT, MONET),
            "Colorful" to listOf(GREEN_APPLE, LAVENDER, STRAWBERRY_DAIQUIRI, TEALTURQUOISE, TIDAL_WAVE),
            "Dark" to listOf(MIDNIGHT_DUSK, NORD, MONOCHROME),
            "Muted" to listOf(CATPPUCCIN, TAKO, YINYANG, YOTSUBA),
        )

        /**
         * Gets themes that work well with dark mode.
         */
        val darkModeCompatible: Set<AppTheme> = setOf(
            MIDNIGHT_DUSK, NORD, MONOCHROME, CATPPUCCIN, TAKO
        )

        /**
         * Gets themes that work well with light mode.
         */
        val lightModeCompatible: Set<AppTheme> = setOf(
            DEFAULT, GREEN_APPLE, LAVENDER, STRAWBERRY_DAIQUIRI, TEALTURQUOISE, YOTSUBA
        )

        /**
         * Gets themes that are system-adaptive (change with system theme).
         */
        val systemAdaptive: Set<AppTheme> = setOf(MONET, DEFAULT)
    }

    /**
     * Checks if this theme is compatible with dark mode.
     */
    val isDarkModeCompatible: Boolean
        get() = this in darkModeCompatible

    /**
     * Checks if this theme is compatible with light mode.
     */
    val isLightModeCompatible: Boolean
        get() = this in lightModeCompatible

    /**
     * Checks if this theme adapts to system theme changes.
     */
    val isSystemAdaptive: Boolean
        get() = this in systemAdaptive

    /**
     * Gets the display name for the theme, handling deprecated themes gracefully.
     */
    val displayName: String
        get() = titleRes?.let { MR.strings.getString(it) } ?: name

    /**
     * Gets the theme category for organization purposes.
     */
    val category: String
        get() = when (this) {
            DEFAULT, MONET -> "Recommended"
            GREEN_APPLE, LAVENDER, STRAWBERRY_DAIQUIRI, TEALTURQUOISE, TIDAL_WAVE -> "Colorful"
            MIDNIGHT_DUSK, NORD, MONOCHROME -> "Dark"
            CATPPUCCIN, TAKO, YINYANG, YOTSUBA -> "Muted"
            else -> "Other"
        }
}
