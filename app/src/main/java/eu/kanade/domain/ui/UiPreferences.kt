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

    fun themeMode() = preferenceStore.getEnum("theme_mode", ThemeMode.SYSTEM)

    fun appTheme() = preferenceStore.getEnum(
        "app_theme",
        if (DeviceUtil.isDynamicColorAvailable) AppTheme.MONET else AppTheme.DEFAULT,
    )

    fun themeDarkAmoled() = preferenceStore.getBoolean("theme_dark_amoled", false)

    fun relativeTime() = preferenceStore.getBoolean("relative_time", true)

    fun dateFormat() = preferenceStore.getString("date_format", "")

    fun tabletUiMode() = preferenceStore.getEnum("tablet_ui_mode", TabletUiMode.AUTOMATIC)

    fun imagesInDescription() = preferenceStore.getBoolean("images_in_description", true)

    fun hideBottomNavigationOnScroll() = preferenceStore.getBoolean("hide_bottom_nav_on_scroll", true)

    fun useGridForLibrary() = preferenceStore.getBoolean("use_grid_library", false)

    fun showLibraryTabs() = preferenceStore.getBoolean("show_library_tabs", true)

    companion object {
        fun dateFormat(format: String): DateTimeFormatter = when (format) {
            "" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            else -> DateTimeFormatter.ofPattern(format, Locale.getDefault())
        }

        val defaultAppTheme: AppTheme
            get() = if (DeviceUtil.isDynamicColorAvailable) AppTheme.MONET else AppTheme.DEFAULT
    }
}
