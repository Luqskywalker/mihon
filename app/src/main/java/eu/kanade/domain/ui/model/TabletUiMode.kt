package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

enum class TabletUiMode(val titleRes: StringResource) {
    AUTOMATIC(MR.strings.automatic_background),
    ALWAYS(MR.strings.lock_always),
    LANDSCAPE(MR.strings.landscape),
    NEVER(MR.strings.lock_never);

    companion object {
        val default = AUTOMATIC
        val entries = entries.toList()

        /**
         * Determines if tablet UI should be enabled based on current configuration.
         */
        fun shouldUseTabletUi(
            mode: TabletUiMode,
            isTablet: Boolean,
            isLandscape: Boolean,
        ): Boolean = when (mode) {
            AUTOMATIC -> isTablet
            ALWAYS -> true
            LANDSCAPE -> isLandscape
            NEVER -> false
        }

        /**
         * Gets the recommended mode based on device type.
         */
        val recommended: TabletUiMode
            get() = AUTOMATIC

        /**
         * Checks if this mode forces tablet UI regardless of device.
         */
        val forcesTabletUi: Boolean
            get() = this == ALWAYS

        /**
         * Checks if this mode depends on device orientation.
         */
        val isOrientationDependent: Boolean
            get() = this == LANDSCAPE

        /**
         * Checks if this mode respects device type detection.
         */
        val usesDeviceDetection: Boolean
            get() = this == AUTOMATIC
    }

    /**
     * Description explaining what this mode does.
     */
    val descriptionRes: StringResource
        get() = when (this) {
            AUTOMATIC -> MR.strings.tablet_mode_automatic_desc
            ALWAYS -> MR.strings.tablet_mode_always_desc
            LANDSCAPE -> MR.strings.tablet_mode_landscape_desc
            NEVER -> MR.strings.tablet_mode_never_desc
        }

    /**
     * Short label for compact UI displays.
     */
    val shortLabel: String
        get() = when (this) {
            AUTOMATIC -> "Auto"
            ALWAYS -> "Always"
            LANDSCAPE -> "Landscape"
            NEVER -> "Never"
        }

    /**
     * Icon resource identifier for this mode (would need actual resources).
     */
    val iconRes: String
        get() = when (this) {
            AUTOMATIC -> "ic_tablet_auto"
            ALWAYS -> "ic_tablet_always"
            LANDSCAPE -> "ic_landscape"
            NEVER -> "ic_tablet_never"
        }
}
