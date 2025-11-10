package eu.kanade.presentation.category

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Gets the visual display name for a category, handling system categories appropriately.
 */
val Category.visualName: String
    @Composable
    get() = when {
        isSystemCategory -> stringResource(MR.strings.label_default)
        else -> name
    }

/**
 * Gets the visual display name for a category, handling system categories appropriately.
 * This version is optimized for Compose and memoizes the result.
 */
@Composable
fun Category.rememberVisualName(): String = remember(this) {
    when {
        isSystemCategory -> stringResource(MR.strings.label_default)
        else -> name
    }
}

/**
 * Gets the visual display name for a category, handling system categories appropriately.
 * Android Context version for use outside of Compose.
 */
fun Category.visualName(context: Context): String =
    when {
        isSystemCategory -> context.stringResource(MR.strings.label_default)
        else -> name
    }

/**
 * Extension function to get visual name with memoization for specific use cases.
 * Useful when the category object might change but the visual name calculation is expensive.
 */
@Composable
fun Category.rememberVisualName(key: Any? = null): String = remember(this, key) {
    when {
        isSystemCategory -> stringResource(MR.strings.label_default)
        else -> name
    }
}

// Alternative optimized versions for specific scenarios

/**
 * Optimized version for lists that only depends on necessary properties.
 */
@Composable
fun Category.rememberVisualNameOptimized(): String = remember(id, isSystemCategory, name) {
    when {
        isSystemCategory -> stringResource(MR.strings.label_default)
        else -> name
    }
}

/**
 * Factory function for creating a visual name provider that can be reused.
 */
@Composable
fun rememberCategoryVisualNameProvider(): (Category) -> String {
    val defaultString = stringResource(MR.strings.label_default)
    return remember(defaultString) { { category ->
        when {
            category.isSystemCategory -> defaultString
            else -> category.name
        }
    }
}

// Utility functions for common category operations

/**
 * Checks if this category should show visual indicators for being the default category.
 */
val Category.shouldShowDefaultIndicator: Boolean
    get() = isSystemCategory

/**
 * Gets the appropriate content description for accessibility.
 */
@Composable
fun Category.getAccessibilityDescription(): String = remember(this) {
    when {
        isSystemCategory -> stringResource(MR.strings.default_category_accessibility_description)
        else -> name
    }
}

/**
 * Android Context version of accessibility description.
 */
fun Category.getAccessibilityDescription(context: Context): String =
    when {
        isSystemCategory -> context.stringResource(MR.strings.default_category_accessibility_description)
        else -> name
    }
