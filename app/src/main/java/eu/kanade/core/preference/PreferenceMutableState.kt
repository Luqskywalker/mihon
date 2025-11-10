package eu.kanade.core.preference

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import tachiyomi.core.common.preference.Preference
import kotlin.coroutines.CoroutineContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A [MutableState] implementation that synchronizes with a [Preference].
 *
 * This class provides two-way binding between a Compose [MutableState] and a data store preference,
 * automatically updating the state when the preference changes and persisting changes back to the preference.
 *
 * @param T The type of data stored in the preference
 * @property preference The underlying preference to synchronize with
 * @property scope The coroutine scope for collecting preference changes
 * @property onError Optional error handler for preference update failures
 */
class PreferenceMutableState<T>(
    private val preference: Preference<T>,
    scope: CoroutineScope,
    private val onError: (Throwable) -> Unit = { /* default silent handling */ }
) : MutableState<T>, ReadWriteProperty<Any?, T> {

    private val state = mutableStateOf(preference.get())

    init {
        preference.changes()
            .onStart {
                // Ensure initial value is set
                state.value = preference.get()
            }
            .onEach { newValue ->
                state.value = newValue
            }
            .catch { error ->
                onError(error)
            }
            .launchIn(scope)
    }

    override var value: T
        get() = state.value
        set(value) {
            try {
                preference.set(value)
                // Note: state.value will be updated via the flow when the preference changes
            } catch (error: Throwable) {
                onError(error)
                // Re-throw or handle based on requirements
                throw error
            }
        }

    override fun component1(): T = state.value

    override fun component2(): (T) -> Unit = { newValue ->
        value = newValue
    }

    /**
     * Updates the value without triggering the preference setter.
     * Useful for temporary UI state that shouldn't persist.
     */
    fun updateLocalValue(newValue: T) {
        state.value = newValue
    }

    /**
     * Refreshes the state from the current preference value.
     */
    fun refresh() {
        state.value = preference.get()
    }

    /**
     * Delegate support for property delegation.
     */
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

/**
 * Creates a [MutableState] that synchronizes with this preference.
 *
 * @param scope The coroutine scope for collecting preference changes
 * @param onError Optional error handler for preference update failures
 * @return A [PreferenceMutableState] that stays in sync with the preference
 */
fun <T> Preference<T>.asState(
    scope: CoroutineScope,
    onError: (Throwable) -> Unit = { /* default silent handling */ }
): PreferenceMutableState<T> = PreferenceMutableState(this, scope, onError)

/**
 * Creates a [MutableState] that synchronizes with this preference using a specific coroutine context.
 */
fun <T> Preference<T>.asState(
    context: CoroutineContext,
    onError: (Throwable) -> Unit = { /* default silent handling */ }
): PreferenceMutableState<T> {
    val scope = CoroutineScope(context)
    return PreferenceMutableState(this, scope, onError)
}

/**
 * Extension function to create a preference state with a custom error handler.
 */
inline fun <T> Preference<T>.asStateWithErrorHandler(
    scope: CoroutineScope,
    crossinline onError: (Throwable) -> Unit
): PreferenceMutableState<T> = PreferenceMutableState(this, scope, onError)

/**
 * Utility function to create multiple preference states efficiently.
 */
fun <T> List<Preference<T>>.asStates(
    scope: CoroutineScope,
    onError: (Throwable) -> Unit = { /* default silent handling */ }
): List<PreferenceMutableState<T>> = map { it.asState(scope, onError) }

/**
 * Convenience operator for creating preference state with invoke syntax.
 */
operator fun <T> Preference<T>.invoke(scope: CoroutineScope): PreferenceMutableState<T> = asState(scope)

// Example usage:
/*
 * // Basic usage
 * val themePreference = preferences.getEnum("theme", Theme.System)
 * val themeState = themePreference.asState(scope)
 * 
 * // With error handling
 * val settingsState = settingsPreference.asState(scope) { error ->
 *     Log.e("PreferenceState", "Failed to update preference", error)
 * }
 * 
 * // Property delegation
 * var uiMode by themePreference.asState(scope)
 * 
 * // Multiple preferences
 * val preferenceStates = listOf(themePref, languagePref).asStates(scope)
 */
