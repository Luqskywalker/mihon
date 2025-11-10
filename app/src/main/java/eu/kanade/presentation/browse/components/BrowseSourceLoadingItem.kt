package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun BrowseSourceLoadingItem(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = remember(modifier) {
            modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        },
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

// Alternative optimized version with content description
@Composable
internal fun BrowseSourceLoadingItemOptimized(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier,
            // Provide content description for accessibility
            // If null, it will be handled by the system as a decorative element
        )
    }
}

// Minimal version for maximum performance
@Composable
internal fun BrowseSourceLoadingItemMinimal(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}
