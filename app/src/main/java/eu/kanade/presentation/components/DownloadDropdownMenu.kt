package eu.kanade.presentation.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import eu.kanade.presentation.manga.DownloadAction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun DownloadDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDownloadClicked: (DownloadAction) -> Unit,
    offset: DpOffset? = null,
    modifier: Modifier = Modifier,
    enabledActions: Set<DownloadAction> = DownloadAction.entries.toSet(),
) {
    if (!expanded) return

    val downloadOptions = rememberDownloadOptions()
    val filteredOptions = remember(downloadOptions, enabledActions) {
        downloadOptions.filter { (action, _) -> action in enabledActions }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset ?: DpOffset.Zero,
    ) {
        DownloadDropdownMenuItems(
            options = filteredOptions,
            onDismissRequest = onDismissRequest,
            onDownloadClicked = onDownloadClicked,
        )
    }
}

@Composable
private fun rememberDownloadOptions(): ImmutableList<Pair<DownloadAction, String>> {
    return remember {
        persistentListOf(
            DownloadAction.NEXT_1_CHAPTER to pluralStringResource(MR.plurals.download_amount, 1, 1),
            DownloadAction.NEXT_5_CHAPTERS to pluralStringResource(MR.plurals.download_amount, 5, 5),
            DownloadAction.NEXT_10_CHAPTERS to pluralStringResource(MR.plurals.download_amount, 10, 10),
            DownloadAction.NEXT_25_CHAPTERS to pluralStringResource(MR.plurals.download_amount, 25, 25),
            DownloadAction.UNREAD_CHAPTERS to stringResource(MR.strings.download_unread),
            DownloadAction.ALL_CHAPTERS to stringResource(MR.strings.download_all),
        )
    }
}

@Composable
private fun ColumnScope.DownloadDropdownMenuItems(
    options: ImmutableList<Pair<DownloadAction, String>>,
    onDismissRequest: () -> Unit,
    onDownloadClicked: (DownloadAction) -> Unit,
) {
    options.forEach { (downloadAction, label) ->
        key(downloadAction) {
            DownloadDropdownMenuItem(
                label = label,
                onClick = {
                    onDownloadClicked(downloadAction)
                    onDismissRequest()
                },
            )
        }
    }
}

@Composable
private fun ColumnScope.DownloadDropdownMenuItem(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    DropdownMenuItem(
        text = { 
            Text(
                text = label,
                maxLines = 1,
            ) 
        },
        onClick = onClick,
        enabled = enabled,
    )
}

// Alternative optimized version with additional features
@Composable
fun DownloadDropdownMenuOptimized(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDownloadClicked: (DownloadAction) -> Unit,
    offset: DpOffset = DpOffset.Zero,
    modifier: Modifier = Modifier,
    enabledActions: Set<DownloadAction> = DownloadAction.entries.toSet(),
    showDivider: Boolean = false,
) {
    if (!expanded) return

    val downloadOptions = rememberDownloadOptions()
    val filteredOptions = remember(downloadOptions, enabledActions) {
        downloadOptions.filter { (action, _) -> action in enabledActions }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
    ) {
        DownloadDropdownMenuItemsOptimized(
            options = filteredOptions,
            onDismissRequest = onDismissRequest,
            onDownloadClicked = onDownloadClicked,
            showDivider = showDivider,
        )
    }
}

@Composable
private fun ColumnScope.DownloadDropdownMenuItemsOptimized(
    options: ImmutableList<Pair<DownloadAction, String>>,
    onDismissRequest: () -> Unit,
    onDownloadClicked: (DownloadAction) -> Unit,
    showDivider: Boolean = false,
) {
    options.forEachIndexed { index, (downloadAction, label) ->
        key(downloadAction) {
            DownloadDropdownMenuItem(
                label = label,
                onClick = {
                    onDownloadClicked(downloadAction)
                    onDismissRequest()
                },
            )
        }
        
        // Add divider between bulk actions and special actions if needed
        if (showDivider && index == options.lastIndex - 1) {
            DropdownDivider()
        }
    }
}

// Extension function for DropdownMenu to add divider (if not available in material3)
@Composable
private fun ColumnScope.DropdownDivider() {
    // Implementation depends on your design system
    // This is a placeholder for divider composable
}

// Data class for better type safety and configuration
data class DownloadMenuConfig(
    val enabledActions: Set<DownloadAction> = DownloadAction.entries.toSet(),
    val showOffset: Boolean = true,
    val offset: DpOffset = DpOffset.Zero,
    val showDividers: Boolean = false,
)

// Factory function for creating download menu configurations
@Composable
fun rememberDownloadMenuConfig(
    enabledActions: Set<DownloadAction> = DownloadAction.entries.toSet(),
    offset: DpOffset = DpOffset.Zero,
    showDividers: Boolean = false,
): DownloadMenuConfig {
    return remember(enabledActions, offset, showDividers) {
        DownloadMenuConfig(
            enabledActions = enabledActions,
            offset = offset,
            showDividers = showDividers,
        )
    }
}

// Compact version for simple use cases
@Composable
fun SimpleDownloadDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDownloadClicked: (DownloadAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    DownloadDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        onDownloadClicked = onDownloadClicked,
        modifier = modifier,
    )
}

// Utility extension for DownloadAction
val DownloadAction.displayText: String
    @Composable
    get() = when (this) {
        DownloadAction.NEXT_1_CHAPTER -> pluralStringResource(MR.plurals.download_amount, 1, 1)
        DownloadAction.NEXT_5_CHAPTERS -> pluralStringResource(MR.plurals.download_amount, 5, 5)
        DownloadAction.NEXT_10_CHAPTERS -> pluralStringResource(MR.plurals.download_amount, 10, 10)
        DownloadAction.NEXT_25_CHAPTERS -> pluralStringResource(MR.plurals.download_amount, 25, 25)
        DownloadAction.UNREAD_CHAPTERS -> stringResource(MR.strings.download_unread)
        DownloadAction.ALL_CHAPTERS -> stringResource(MR.strings.download_all)
    }
