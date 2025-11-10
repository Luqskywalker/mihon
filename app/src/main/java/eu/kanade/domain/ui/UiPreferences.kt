package eu.kanade.domain.ui

import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isDynamicColorAvailable
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class UiPreferences(
    private val preferenceStore: PreferenceStore,
) {

    // Theme preferences
    fun themeMode() = preferenceStore.getEnum("theme_mode", ThemeMode.SYSTEM)
    fun appTheme() = preferenceStore.getEnum("app_theme", AppTheme.defaultAppTheme)
    fun themeDarkAmoled() = preferenceStore.getBoolean("theme_dark_amoled", false)
    fun themeFollowSystemAccent() = preferenceStore.getBoolean("theme_follow_system_accent", true)

    // Date & Time preferences
    fun relativeTime() = preferenceStore.getBoolean("relative_time", true)
    fun dateFormat() = preferenceStore.getString("date_format", "")
    fun timeFormat() = preferenceStore.getString("time_format", "")
    fun use24HourFormat() = preferenceStore.getBoolean("use_24_hour_format", false)

    // Layout preferences
    fun tabletUiMode() = preferenceStore.getEnum("tablet_ui_mode", TabletUiMode.AUTOMATIC)
    fun useGridForLibrary() = preferenceStore.getBoolean("use_grid_library", false)
    fun libraryColumnsPortrait() = preferenceStore.getInt("library_columns_portrait", 3)
    fun libraryColumnsLandscape() = preferenceStore.getInt("library_columns_landscape", 5)
    fun showLibraryTabs() = preferenceStore.getBoolean("show_library_tabs", true)
    fun compactLibraryGrid() = preferenceStore.getBoolean("compact_library_grid", false)

    // Navigation preferences
    fun hideBottomNavigationOnScroll() = preferenceStore.getBoolean("hide_bottom_nav_on_scroll", true)
    fun bottomNavigationLabels() = preferenceStore.getBoolean("bottom_nav_labels", true)
    fun gestureNavigation() = preferenceStore.getBoolean("gesture_navigation", true)

    // Content preferences
    fun imagesInDescription() = preferenceStore.getBoolean("images_in_description", true)
    fun showContentWarnings() = preferenceStore.getBoolean("show_content_warnings", true)
    fun autoHideSystemBars() = preferenceStore.getBoolean("auto_hide_system_bars", false)
    fun immersiveReaderMode() = preferenceStore.getBoolean("immersive_reader_mode", false)

    // Animation preferences
    fun reduceAnimations() = preferenceStore.getBoolean("reduce_animations", false)
    fun pageTransitions() = preferenceStore.getBoolean("page_transitions", true)
    fun smoothScrolling() = preferenceStore.getBoolean("smooth_scrolling", true)

    companion object {
        val defaultAppTheme: AppTheme
            get() = if (DeviceUtil.isDynamicColorAvailable) AppTheme.MONET else AppTheme.DEFAULT

        fun dateFormat(format: String): DateTimeFormatter = when (format) {
            "" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            else -> DateTimeFormatter.ofPattern(format, Locale.getDefault())
        }

        fun timeFormat(format: String, use24Hour: Boolean): DateTimeFormatter = when {
            format.isNotEmpty() -> DateTimeFormatter.ofPattern(format, Locale.getDefault())
            use24Hour -> DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
            else -> DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        }

        fun getLibraryColumns(isLandscape: Boolean, columnsPortrait: Int, columnsLandscape: Int): Int =
            if (isLandscape) columnsLandscape else columnsPortrait

        fun shouldUseTabletUi(
            tabletUiMode: TabletUiMode,
            isTablet: Boolean,
            isLandscape: Boolean,
        ): Boolean = TabletUiMode.shouldUseTabletUi(tabletUiMode, isTablet, isLandscape)
    }
}
