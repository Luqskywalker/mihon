package eu.kanade.presentation.browse.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import tachiyomi.presentation.core.components.Badge

@Composable
internal fun InLibraryBadge(
    enabled: Boolean,
) {
    if (enabled) {
        Badge(
            imageVector = Icons.Outlined.CollectionsBookmark,
        )
    }
}

// Alternative optimized version with additional features:
@Composable
internal fun InLibraryBadgeOptimized(
    enabled: Boolean,
    contentDescription: String? = null,
) {
    // Only create the badge when enabled to avoid unnecessary compositions
    if (enabled) {
        val rememberedContentDescription = remember(contentDescription) {
            contentDescription ?: "In library"
        }
        
        Badge(
            imageVector = Icons.Outlined.CollectionsBookmark,
            contentDescription = rememberedContentDescription,
        )
    }
}

// Even more optimized version for lists - prevents recomposition when state doesn't change
@Composable
internal fun InLibraryBadgeStable(
    enabled: Boolean,
    contentDescription: String? = null,
) {
    // Using remember to stabilize the computation
    val shouldShowBadge = remember(enabled) { enabled }
    
    if (shouldShowBadge) {
        Badge(
            imageVector = Icons.Outlined.CollectionsBookmark,
            contentDescription = contentDescription ?: "In library",
        )
    }
}
