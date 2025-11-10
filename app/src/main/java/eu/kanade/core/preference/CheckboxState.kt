package eu.kanade.core.preference

import androidx.compose.ui.state.ToggleableState
import tachiyomi.core.common.preference.CheckboxState

/**
 * Converts a [CheckboxState.TriState] to a Compose [ToggleableState].
 *
 * This extension function maps the domain-specific tri-state checkbox state
 * to the corresponding UI toggle state used in Jetpack Compose.
 *
 * @receiver The tri-state checkbox state to convert
 * @return The corresponding [ToggleableState] for Compose UI
 *
 * @sample eu.kanade.core.preference.TriStateConversionTest.testTriStateToToggleableStateConversion
 */
fun <T> CheckboxState.TriState<T>.asToggleableState(): ToggleableState = when (this) {
    is CheckboxState.TriState.Exclude -> ToggleableState.Indeterminate
    is CheckboxState.TriState.Include -> ToggleableState.On
    is CheckboxState.TriState.None -> ToggleableState.Off
}

/**
 * Alternative implementation with exhaustive when statement for better safety.
 * This version ensures the compiler will warn if new TriState variants are added.
 */
fun <T> CheckboxState.TriState<T>.toToggleableState(): ToggleableState = when (this) {
    is CheckboxState.TriState.Exclude -> ToggleableState.Indeterminate
    is CheckboxState.TriState.Include -> ToggleableState.On
    is CheckboxState.TriState.None -> ToggleableState.Off
    // No else branch - compiler will error if new TriState variants are added
}

/**
 * Reverse conversion from [ToggleableState] to [CheckboxState.TriState].
 * Useful for two-way data binding scenarios.
 */
fun <T> ToggleableState.toTriState(): CheckboxState.TriState<T> = when (this) {
    ToggleableState.Indeterminate -> CheckboxState.TriState.Exclude()
    ToggleableState.On -> CheckboxState.TriState.Include()
    ToggleableState.Off -> CheckboxState.TriState.None()
}

/**
 * Extension property version for cases where property syntax is more appropriate.
 */
val <T> CheckboxState.TriState<T>.toggleableState: ToggleableState
    get() = when (this) {
        is CheckboxState.TriState.Exclude -> ToggleableState.Indeterminate
        is CheckboxState.TriState.Include -> ToggleableState.On
        is CheckboxState.TriState.None -> ToggleableState.Off
    }

// Optional: Inline class for type-safe conversions if needed
@JvmInline
value class TriStateConverter<T>(private val triState: CheckboxState.TriState<T>) {
    fun asToggleableState(): ToggleableState = triState.asToggleableState()
}

// Usage examples:
/*
 * val triState: CheckboxState.TriState<String> = CheckboxState.TriState.Include()
 * val toggleState: ToggleableState = triState.asToggleableState()
 * 
 * // Two-way binding example:
 * fun <T> updateState(toggleState: ToggleableState): CheckboxState.TriState<T> {
 *     return toggleState.toTriState()
 * }
 */
