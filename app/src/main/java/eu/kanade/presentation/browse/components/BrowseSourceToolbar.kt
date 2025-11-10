package eu.kanade.presentation.browse.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.RadioMenuItem
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.LocalSource

@Composable
fun BrowseSourceToolbar(
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    source: Source?,
    displayMode: LibraryDisplayMode,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
    navigateUp: () -> Unit,
    onWebViewClick: () -> Unit,
    onHelpClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearch: (String) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    // Memoize source properties to avoid recomputation on every recomposition
    val sourceProperties by remember(source) {
        derivedStateOf {
            SourceProperties(
                title = source?.name,
                isLocalSource = source is LocalSource,
                isConfigurableSource = source is ConfigurableSource
            )
        }
    }

    var selectingDisplayMode by remember { mutableStateOf(false) }

    // Memoize display mode icon to avoid recomputation
    val displayModeIcon by remember(displayMode) {
        derivedStateOf {
            if (displayMode == LibraryDisplayMode.List) {
                Icons.AutoMirrored.Filled.ViewList
            } else {
                Icons.Filled.ViewModule
            }
        }
    }

    // Memoize toolbar actions to prevent recreation on every recomposition
    val toolbarActions by remember(
        sourceProperties.isLocalSource,
        sourceProperties.isConfigurableSource,
        displayModeIcon,
        onWebViewClick,
        onHelpClick,
        onSettingsClick
    ) {
        derivedStateOf {
            buildToolbarActions(
                isLocalSource = sourceProperties.isLocalSource,
                isConfigurableSource = sourceProperties.isConfigurableSource,
                displayModeIcon = displayModeIcon,
                onDisplayModeClick = { selectingDisplayMode = true },
                onWebViewClick = onWebViewClick,
                onHelpClick = onHelpClick,
                onSettingsClick = onSettingsClick
            )
        }
    }

    SearchToolbar(
        navigateUp = navigateUp,
        titleContent = { 
            AppBarTitle(sourceProperties.title ?: "")
        },
        searchQuery = searchQuery,
        onChangeSearchQuery = onSearchQueryChange,
        onSearch = onSearch,
        onClickCloseSearch = navigateUp,
        actions = {
            AppBarActions(actions = toolbarActions)

            DisplayModeDropdown(
                expanded = selectingDisplayMode,
                currentDisplayMode = displayMode,
                onDismissRequest = { selectingDisplayMode = false },
                onDisplayModeChange = { newMode ->
                    selectingDisplayMode = false
                    onDisplayModeChange(newMode)
                }
            )
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun DisplayModeDropdown(
    expanded: Boolean,
    currentDisplayMode: LibraryDisplayMode,
    onDismissRequest: () -> Unit,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DisplayModeDropdownItem(
            displayMode = LibraryDisplayMode.ComfortableGrid,
            currentDisplayMode = currentDisplayMode,
            labelRes = MR.strings.action_display_comfortable_grid,
            onDisplayModeChange = onDisplayModeChange
        )
        DisplayModeDropdownItem(
            displayMode = LibraryDisplayMode.CompactGrid,
            currentDisplayMode = currentDisplayMode,
            labelRes = MR.strings.action_display_grid,
            onDisplayModeChange = onDisplayModeChange
        )
        DisplayModeDropdownItem(
            displayMode = LibraryDisplayMode.List,
            currentDisplayMode = currentDisplayMode,
            labelRes = MR.strings.action_display_list,
            onDisplayModeChange = onDisplayModeChange
        )
    }
}

@Composable
private fun DisplayModeDropdownItem(
    displayMode: LibraryDisplayMode,
    currentDisplayMode: LibraryDisplayMode,
    labelRes: MR.strings,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
) {
    RadioMenuItem(
        text = { Text(text = stringResource(labelRes)) },
        isChecked = currentDisplayMode == displayMode,
    ) {
        onDisplayModeChange(displayMode)
    }
}

private data class SourceProperties(
    val title: String?,
    val isLocalSource: Boolean,
    val isConfigurableSource: Boolean
)

private fun buildToolbarActions(
    isLocalSource: Boolean,
    isConfigurableSource: Boolean,
    displayModeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onDisplayModeClick: () -> Unit,
    onWebViewClick: () -> Unit,
    onHelpClick: () -> Unit,
    onSettingsClick: () -> Unit,
): ImmutableList<AppBar.AppBarAction> {
    val actions = mutableListOf<AppBar.AppBarAction>()

    // Always add display mode action
    actions.add(
        AppBar.Action(
            title = stringResource(MR.strings.action_display_mode),
            icon = displayModeIcon,
            onClick = onDisplayModeClick,
        )
    )

    // Add context-specific overflow actions
    if (isLocalSource) {
        actions.add(
            AppBar.OverflowAction(
                title = stringResource(MR.strings.label_help),
                onClick = onHelpClick,
            )
        )
    } else {
        actions.add(
            AppBar.OverflowAction(
                title = stringResource(MR.strings.action_open_in_web_view),
                onClick = onWebViewClick,
            )
        )
    }

    if (isConfigurableSource) {
        actions.add(
            AppBar.OverflowAction(
                title = stringResource(MR.strings.action_settings),
                onClick = onSettingsClick,
            )
        )
    }

    return actions.toImmutableList()
}
