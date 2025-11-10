package eu.kanade.domain.base

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.util.system.GLUtil
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.i18n.MR
import kotlin.properties.Delegates

/**
 * Centralized management for base application preferences.
 * Provides type-safe access to shared preferences with proper default values.
 *
 * @property context The application context for system services and resources
 * @property preferenceStore The underlying preference storage implementation
 */
class BasePreferences(
    private val context: Context,
    private val preferenceStore: PreferenceStore,
) {

    companion object {
        private const val PREF_DOWNLOADED_ONLY = "pref_downloaded_only"
        private const val PREF_INCOGNITO_MODE = "incognito_mode"
        private const val PREF_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val PREF_DISPLAY_PROFILE = "pref_display_profile_key"
        private const val PREF_HARDWARE_BITMAP_THRESHOLD = "pref_hardware_bitmap_threshold"
        private const val PREF_ALWAYS_DECODE_LONG_STRIP_WITH_SSIV = "pref_always_decode_long_strip_with_ssiv"
        
        // Extension installer preference keys
        private const val PREF_EXTENSION_INSTALLER = "pref_extension_installer"
        private const val PREF_EXTENSION_INSTALLER_LAST_USED = "pref_extension_installer_last_used"
    }

    // region Basic Preferences
    
    /**
     * Preference for filtering to downloaded content only.
     */
    val downloadedOnly: Preference<Boolean> = preferenceStore.getBoolean(
        Preference.appStateKey(PREF_DOWNLOADED_ONLY),
        defaultValue = false,
    )

    /**
     * Preference for enabling incognito browsing mode.
     * Hides reading history and recent activity.
     */
    val incognitoMode: Preference<Boolean> = preferenceStore.getBoolean(
        Preference.appStateKey(PREF_INCOGNITO_MODE),
        defaultValue = false,
    )

    /**
     * Preference tracking whether the onboarding flow has been completed.
     */
    val shownOnboardingFlow: Preference<Boolean> = preferenceStore.getBoolean(
        Preference.appStateKey(PREF_ONBOARDING_COMPLETE),
        defaultValue = false,
    )

    /**
     * Preference for user display profile (theme/UI customization).
     */
    val displayProfile: Preference<String> = preferenceStore.getString(
        PREF_DISPLAY_PROFILE,
        defaultValue = "",
    )

    /**
     * Preference for hardware bitmap rendering threshold.
     * Values below this threshold use hardware acceleration.
     */
    val hardwareBitmapThreshold: Preference<Int> = preferenceStore.getInt(
        PREF_HARDWARE_BITMAP_THRESHOLD,
        defaultValue = GLUtil.SAFE_TEXTURE_LIMIT,
    )

    /**
     * Preference for always using Subsampling Scale Image View for long strips.
     */
    val alwaysDecodeLongStripWithSSIV: Preference<Boolean> = preferenceStore.getBoolean(
        PREF_ALWAYS_DECODE_LONG_STRIP_WITH_SSIV,
        defaultValue = false,
    )

    // endregion

    // region Extension Installer Preferences
    
    /**
     * Manages extension installer preferences with platform compatibility checks.
     */
    fun extensionInstaller() = ExtensionInstallerPreference(context, preferenceStore)

    /**
     * Preference for the currently selected extension installer method.
     */
    val extensionInstallerMethod: Preference<String> = preferenceStore.getString(
        Preference.appStateKey(PREF_EXTENSION_INSTALLER),
        defaultValue = getDefaultExtensionInstaller().name,
    )

    /**
     * Preference for tracking the last used extension installer method.
     */
    val lastUsedExtensionInstaller: Preference<String> = preferenceStore.getString(
        PREF_EXTENSION_INSTALLER_LAST_USED,
        defaultValue = ExtensionInstaller.LEGACY.name,
    )

    // endregion

    // region Extension Installer Enum
    
    /**
     * Available extension installation methods with platform compatibility information.
     *
     * @property titleRes String resource for display name
     * @property requiresSystemPermission Whether this method requires system-level install permissions
     * @property minSdkVersion Minimum API level required for this method (null if no minimum)
     * @property maxSdkVersion Maximum API level supported (null if no maximum)
     */
    enum class ExtensionInstaller(
        val titleRes: StringResource,
        val requiresSystemPermission: Boolean,
        val minSdkVersion: Int? = null,
        val maxSdkVersion: Int? = null,
    ) {
        LEGACY(
            titleRes = MR.strings.ext_installer_legacy,
            requiresSystemPermission = true,
            maxSdkVersion = Build.VERSION_CODES.Q, // Deprecated in Android 11+
        ),
        PACKAGEINSTALLER(
            titleRes = MR.strings.ext_installer_packageinstaller,
            requiresSystemPermission = true,
            minSdkVersion = Build.VERSION_CODES.LOLLIPOP,
        ),
        SHIZUKU(
            titleRes = MR.strings.ext_installer_shizuku,
            requiresSystemPermission = false,
            minSdkVersion = Build.VERSION_CODES.R,
        ),
        PRIVATE(
            titleRes = MR.strings.ext_installer_private,
            requiresSystemPermission = false,
            minSdkVersion = Build.VERSION_CODES.N,
        );

        /**
         * Checks if this installer method is available on the current device.
         */
        fun isAvailable(): Boolean {
            return when {
                minSdkVersion != null && Build.VERSION.SDK_INT < minSdkVersion -> false
                maxSdkVersion != null && Build.VERSION.SDK_INT > maxSdkVersion -> false
                else -> true
            }
        }

        companion object {
            /**
             * Gets the default extension installer based on device capabilities.
             */
            fun getDefault(): ExtensionInstaller {
                return values().firstOrNull { it.isAvailable() && !it.requiresSystemPermission }
                    ?: PACKAGEINSTALLER
            }

            /**
             * Gets all available installers for the current device.
             */
            fun getAvailableInstallers(): List<ExtensionInstaller> {
                return values().filter { it.isAvailable() }
            }

            /**
             * Finds an installer by name, falling back to default if not found.
             */
            fun fromName(name: String?): ExtensionInstaller {
                return values().find { it.name == name } ?: getDefault()
            }
        }
    }

    // endregion

    // region Utility Methods
    
    /**
     * Gets the default extension installer based on current device capabilities.
     */
    private fun getDefaultExtensionInstaller(): ExtensionInstaller {
        return ExtensionInstaller.getDefault()
    }

    /**
     * Updates the last used extension installer method.
     */
    fun updateLastUsedInstaller(installer: ExtensionInstaller) {
        lastUsedExtensionInstaller.set(installer.name)
    }

    /**
     * Gets the currently selected extension installer with availability check.
     * Falls back to an available installer if the current selection is unavailable.
     */
    fun getCurrentExtensionInstaller(): ExtensionInstaller {
        val currentName = extensionInstallerMethod.get()
        val currentInstaller = ExtensionInstaller.fromName(currentName)
        
        return if (currentInstaller.isAvailable()) {
            currentInstaller
        } else {
            // Fallback to default available installer
            val defaultInstaller = getDefaultExtensionInstaller()
            // Auto-correct the stored preference
            extensionInstallerMethod.set(defaultInstaller.name)
            defaultInstaller
        }
    }

    /**
     * Resets all preferences to their default values.
     * Note: This does not reset extension installer preferences.
     */
    fun resetToDefaults() {
        downloadedOnly.delete()
        incognitoMode.delete()
        alwaysDecodeLongStripWithSSIV.delete()
        hardwareBitmapThreshold.delete()
        // Don't reset onboarding and display profile as they are user-specific
    }

    /**
     * Checks if any performance-related preferences are enabled.
     */
    fun arePerformanceFeaturesEnabled(): Boolean {
        return hardwareBitmapThreshold.get() != GLUtil.SAFE_TEXTURE_LIMIT ||
                alwaysDecodeLongStripWithSSIV.get()
    }

    // endregion

    // region Delegated Properties for Common Access Patterns
    
    /**
     * Delegated property for frequently accessed downloaded only preference.
     */
    var isDownloadedOnly by Delegates.preference(delegatedPreference = downloadedOnly)

    /**
     * Delegated property for frequently accessed incognito mode preference.
     */
    var isIncognitoMode by Delegates.preference(delegatedPreference = incognitoMode)

    /**
     * Delegated property for onboarding completion status.
     */
    var hasShownOnboarding by Delegates.preference(delegatedPreference = shownOnboardingFlow)

    // endregion
}

// Extension function for preference delegation
inline fun <T> Delegates.preference(
    delegatedPreference: Preference<T>
): kotlin.properties.ReadWriteProperty<Any?, T> {
    return object : kotlin.properties.ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T {
            return delegatedPreference.get()
        }

        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) {
            delegatedPreference.set(value)
        }
    }
}

// Usage examples:
/*
 * // Basic preference access
 * val basePrefs = BasePreferences(context, preferenceStore)
 * 
 * // Check if downloaded only mode is enabled
 * if (basePrefs.downloadedOnly.get()) {
 *     showDownloadedOnlyWarning()
 * }
 * 
 * // Use delegated properties for frequent access
 * basePrefs.isIncognitoMode = true
 * 
 * // Get current extension installer with availability check
 * val installer = basePrefs.getCurrentExtensionInstaller()
 * 
 * // Check available installers
 * val availableInstallers = BasePreferences.ExtensionInstaller.getAvailableInstallers()
 * 
 * // Reset performance settings
 * if (needsReset) {
 *     basePrefs.resetToDefaults()
 * }
 */
