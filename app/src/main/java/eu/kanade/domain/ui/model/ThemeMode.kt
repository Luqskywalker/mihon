package eu.kanade.domain.ui.model

import androidx.appcompat.app.AppCompatDelegate

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
    BATTERY_SAVER;

    companion object {
        val default = SYSTEM
        val entries = entries.toList()

        /**
         * Modes that follow system settings.
         */
        val systemFollowingModes = setOf(SYSTEM, BATTERY_SAVER)

        /**
         * Gets the fallback theme when the current theme is unavailable.
         */
        fun getFallback(current: ThemeMode): ThemeMode = when (current) {
            BATTERY_SAVER -> SYSTEM
            else -> current
        }

        /**
         * Checks if this mode automatically adapts to system changes.
         */
        fun isAutoAdapting(mode: ThemeMode): Boolean = mode in systemFollowingModes

        /**
         * Gets the recommended theme mode based on device capabilities.
         */
        val recommended: ThemeMode
            get() = SYSTEM
    }

    /**
     * Description explaining what this mode does.
     */
    val description: String
        get() = when (this) {
            LIGHT -> "Always use light theme"
            DARK -> "Always use dark theme"
            SYSTEM -> "Follow system theme"
            BATTERY_SAVER -> "Follow system with battery saver"
        }

    /**
     * Short label for compact UI displays.
     */
    val shortLabel: String
        get() = when (this) {
            LIGHT -> "Light"
            DARK -> "Dark"
            SYSTEM -> "System"
            BATTERY_SAVER -> "Battery"
        }

    /**
     * Icon resource identifier for this mode.
     */
    val iconRes: String
        get() = when (this) {
            LIGHT -> "ic_theme_light"
            DARK -> "ic_theme_dark"
            SYSTEM -> "ic_theme_system"
            BATTERY_SAVER -> "ic_theme_battery"
        }
}

/**
 * Applies the theme mode to the AppCompat delegate.
 *
 * @param themeMode The theme mode to apply
 * @param isBatterySaverActive Whether battery saver is currently active (for BATTERY_SAVER mode)
 */
fun setAppCompatDelegateThemeMode(themeMode: ThemeMode, isBatterySaverActive: Boolean = false) {
    val nightMode = when (themeMode) {
        ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ThemeMode.BATTERY_SAVER -> if (isBatterySaverActive) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
    AppCompatDelegate.setDefaultNightMode(nightMode)
}

/**
 * Extension function to check if this theme mode should use dark theme.
 *
 * @param isSystemInDarkMode Whether the system is currently in dark mode
 * @param isBatterySaverActive Whether battery saver is currently active
 */
fun ThemeMode.shouldUseDarkTheme(
    isSystemInDarkMode: Boolean,
    isBatterySaverActive: Boolean = false,
): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkMode
    ThemeMode.BATTERY_SAVER -> isBatterySaverActive || isSystemInDarkMode
}

/**
 * Gets the effective theme mode considering current device state.
 */
fun ThemeMode.getEffectiveMode(isBatterySaverActive: Boolean): ThemeMode = when {
    this == ThemeMode.BATTERY_SAVER && isBatterySaverActive -> ThemeMode.DARK
    this == ThemeMode.BATTERY_SAVER -> ThemeMode.SYSTEM
    else -> this
}
