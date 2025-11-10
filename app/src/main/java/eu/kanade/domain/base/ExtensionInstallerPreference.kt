package eu.kanade.domain.base

import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import eu.kanade.domain.base.BasePreferences.ExtensionInstaller
import eu.kanade.tachiyomi.util.system.hasMiuiPackageInstaller
import eu.kanade.tachiyomi.util.system.isShizukuAvailable
import eu.kanade.tachiyomi.util.system.isShizukuInstalled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import kotlin.jvm.Volatile

/**
 * A specialized preference for managing extension installer selection with runtime availability checks.
 * Automatically handles platform compatibility and fallbacks when the selected installer becomes unavailable.
 *
 * @property context The application context for system capability checks
 * @property preferenceStore The underlying preference storage
 */
class ExtensionInstallerPreference(
    private val context: Context,
    preferenceStore: PreferenceStore,
) : Preference<ExtensionInstaller> {

    companion object {
        private const val PREF_KEY_EXTENSION_INSTALLER = "extension_installer"
        
        /**
         * Cache for available entries to avoid recomputation
         */
        @Volatile
        private var cachedEntries: List<ExtensionInstaller>? = null
        
        /**
         * Cache key for entries to handle context changes
         */
        @Volatile
        private var entriesCacheKey: String? = null
    }

    private val basePref = preferenceStore.getEnum(key(), defaultValue())

    override fun key(): String = PREF_KEY_EXTENSION_INSTALLER

    /**
     * Gets the available extension installer entries based on current device capabilities.
     * Results are cached for performance.
     */
    val entries: List<ExtensionInstaller>
        get() {
            val cacheKey = buildCacheKey()
            return if (cachedEntries != null && entriesCacheKey == cacheKey) {
                cachedEntries!!
            } else {
                val availableEntries = computeAvailableEntries()
                cachedEntries = availableEntries
                entriesCacheKey = cacheKey
                availableEntries
            }
        }

    /**
     * Gets all available entries including those that might require additional setup.
     */
    val entriesWithUnavailable: List<ExtensionInstaller>
        get() = ExtensionInstaller.entries.filter { it.isAvailableOnDevice(context) }

    override fun defaultValue(): ExtensionInstaller = computeDefaultInstaller()

    /**
     * Validates and potentially corrects the given installer value based on current device state.
     */
    @VisibleForTesting
    internal fun validateInstaller(value: ExtensionInstaller): ExtensionInstaller {
        return when {
            !value.isAvailableOnDevice(context) -> {
                // Fallback to first available installer
                entries.firstOrNull() ?: ExtensionInstaller.LEGACY
            }
            value == ExtensionInstaller.SHIZUKU && !context.isShizukuAvailable -> {
                // Shizuku is installed but not running/available
                computeDefaultInstaller()
            }
            else -> value
        }
    }

    override fun get(): ExtensionInstaller {
        val storedValue = basePref.get()
        val validatedValue = validateInstaller(storedValue)
        
        // Auto-correct if the stored value is no longer valid
        if (storedValue != validatedValue) {
            basePref.set(validatedValue)
        }
        
        return validatedValue
    }

    override fun set(value: ExtensionInstaller) {
        val validatedValue = validateInstaller(value)
        basePref.set(validatedValue)
    }

    override fun isSet(): Boolean = basePref.isSet()

    override fun delete() {
        cachedEntries = null
        entriesCacheKey = null
        basePref.delete()
    }

    override fun changes(): Flow<ExtensionInstaller> = basePref.changes()
        .map { validateInstaller(it) }

    override fun stateIn(scope: CoroutineScope) = basePref.stateIn(scope)
        .map { validateInstaller(it) }

    // region Utility Methods

    /**
     * Computes the available installer entries based on device capabilities.
     */
    private fun computeAvailableEntries(): List<ExtensionInstaller> {
        return ExtensionInstaller.entries.filter { installer ->
            when (installer) {
                ExtensionInstaller.PACKAGEINSTALLER -> !context.hasMiuiPackageInstaller
                ExtensionInstaller.SHIZUKU -> context.isShizukuInstalled
                else -> true
            } && installer.isAvailableOnDevice(context)
        }.sortedBy { it.ordinal }
    }

    /**
     * Computes the default installer based on device capabilities.
     */
    private fun computeDefaultInstaller(): ExtensionInstaller {
        return entries.firstOrNull { !it.requiresSystemPermission }
            ?: entries.firstOrNull()
            ?: ExtensionInstaller.LEGACY
    }

    /**
     * Builds a cache key based on current device state.
     */
    private fun buildCacheKey(): String {
        return buildString {
            append("miui:").append(context.hasMiuiPackageInstaller)
            append("|shizuku:").append(context.isShizukuInstalled)
            append("|sdk:").append(Build.VERSION.SDK_INT)
        }
    }

    /**
     * Checks if the current installer method requires system permissions.
     */
    fun currentInstallerRequiresPermission(): Boolean {
        return get().requiresSystemPermission
    }

    /**
     * Gets the display name for the current installer.
     */
    fun getCurrentInstallerName(): String {
        return get().name
    }

    /**
     * Refreshes the internal cache. Call this when device state changes (e.g., Shizuku installed).
     */
    fun refresh() {
        cachedEntries = null
        entriesCacheKey = null
    }

    /**
     * Checks if a specific installer is currently available.
     */
    fun isInstallerAvailable(installer: ExtensionInstaller): Boolean {
        return entries.contains(installer)
    }

    // endregion
}

// Extension functions for better integration

/**
 * Extension function to check availability on a specific device.
 */
fun ExtensionInstaller.isAvailableOnDevice(context: Context): Boolean {
    if (!isAvailable()) return false
    
    return when (this) {
        ExtensionInstaller.PACKAGEINSTALLER -> !context.hasMiuiPackageInstaller
        ExtensionInstaller.SHIZUKU -> context.isShizukuInstalled
        else -> true
    }
}

/**
 * Extension function to get the installer's display name from resources.
 */
fun ExtensionInstaller.getDisplayName(context: Context): String {
    // This would need proper resource handling - placeholder implementation
    return when (this) {
        ExtensionInstaller.LEGACY -> "Legacy Installer"
        ExtensionInstaller.PACKAGEINSTALLER -> "Package Installer"
        ExtensionInstaller.SHIZUKU -> "Shizuku"
        ExtensionInstaller.PRIVATE -> "Private Installer"
    }
}

/**
 * Extension function to get installer description or requirements.
 */
fun ExtensionInstaller.getDescription(context: Context): String {
    return when (this) {
        ExtensionInstaller.LEGACY -> "Traditional installation method"
        ExtensionInstaller.PACKAGEINSTALLER -> "System package installer"
        ExtensionInstaller.SHIZUKU -> "Requires Shizuku service"
        ExtensionInstaller.PRIVATE -> "Private session installation"
    }
}

// Usage examples:
/*
 * val installerPref = ExtensionInstallerPreference(context, preferenceStore)
 * 
 * // Get current installer with validation
 * val currentInstaller = installerPref.get()
 * 
 * // Check available options for UI
 * val availableOptions = installerPref.entries
 * 
 * // Set new installer with automatic validation
 * installerPref.set(ExtensionInstaller.SHIZUKU)
 * 
 * // Listen for changes
 * installerPref.changes()
 *     .onEach { installer ->
 *         updateInstallerUI(installer)
 *     }
 *     .launchIn(scope)
 * 
 * // Refresh cache when device state changes
 * installerPref.refresh()
 */
