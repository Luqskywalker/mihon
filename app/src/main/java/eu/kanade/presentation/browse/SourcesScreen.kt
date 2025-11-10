package eu.kanade.presentation.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreenModel
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.SECONDARY_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.plus
import tachiyomi.source.local.isLocal

@Composable
fun SourcesScreen(
    state: SourcesScreenModel.State,
    contentPadding: PaddingValues,
    onClickItem: (Source, Listing) -> Unit,
    onClickPin: (Source) -> Unit,
    onLongClickItem: (Source) -> Unit,
    // Enhanced features
    navigateUp: () -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    selectedSourceId: Long? = null,
    onSourceSelected: (Source) -> Unit = {},
) {
    Scaffold(
        topBar = { scrollBehavior ->
            SourcesAppBar(
                navigateUp = navigateUp,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        SourcesContent(
            state = state,
            contentPadding = contentPadding + innerPadding,
            searchQuery = searchQuery,
            onClickItem = onClickItem,
            onClickPin = onClickPin,
            onLongClickItem = onLongClickItem,
            selectedSourceId = selectedSourceId,
            onSourceSelected = onSourceSelected,
        )
    }
}

@Composable
private fun SourcesAppBar(
    navigateUp: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
) {
    if (searchQuery.isEmpty()) {
        tachiyomi.presentation.core.components.material.AppBar(
            title = stringResource(MR.strings.label_sources),
            navigateUp = navigateUp,
            scrollBehavior = scrollBehavior,
        )
    } else {
        SearchToolbar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            placeholderText = stringResource(MR.strings.action_filter_sources),
            onNavigateUp = navigateUp,
            scrollBehavior = scrollBehavior,
        )
    }
}

@Composable
private fun SourcesContent(
    state: SourcesScreenModel.State,
    contentPadding: PaddingValues,
    searchQuery: String,
    onClickItem: (Source, Listing) -> Unit,
    onClickPin: (Source) -> Unit,
    onLongClickItem: (Source) -> Unit,
    selectedSourceId: Long?,
    onSourceSelected: (Source) -> Unit,
) {
    when {
        state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
        state.isEmpty -> EmptyScreen(
            stringRes = MR.strings.source_empty_screen,
            modifier = Modifier.padding(contentPadding),
        )
        else -> {
            val filteredItems = remember(state.items, searchQuery) {
                filterSources(state.items, searchQuery)
            }

            if (filteredItems.isEmpty()) {
                EmptySearchScreen(
                    modifier = Modifier.padding(contentPadding),
                    hasSearchQuery = searchQuery.isNotEmpty(),
                )
            } else {
                SourcesList(
                    items = filteredItems,
                    contentPadding = contentPadding,
                    onClickItem = onClickItem,
                    onClickPin = onClickPin,
                    onLongClickItem = onLongClickItem,
                    selectedSourceId = selectedSourceId,
                    onSourceSelected = onSourceSelected,
                )
            }
        }
    }
}

@Composable
private fun SourcesList(
    items: ImmutableList<SourceUiModel>,
    contentPadding: PaddingValues,
    onClickItem: (Source, Listing) -> Unit,
    onClickPin: (Source) -> Unit,
    onLongClickItem: (Source) -> Unit,
    selectedSourceId: Long?,
    onSourceSelected: (Source) -> Unit,
) {
    ScrollbarLazyColumn(
        contentPadding = contentPadding + topSmallPaddingValues,
    ) {
        // Search results header
        if (items.any { it is SourceUiModel.Header }) {
            item(key = "search-results") {
                SearchResultsHeader(
                    itemCount = countSources(items),
                    modifier = Modifier.animateItem(),
                )
            }
        }

        items(
            items = items,
            contentType = {
                when (it) {
                    is SourceUiModel.Header -> "header"
                    is SourceUiModel.Item -> "item"
                }
            },
            key = {
                when (it) {
                    is SourceUiModel.Header -> "header-${it.language}"
                    is SourceUiModel.Item -> "source-${it.source.id}"
                }
            },
        ) { model ->
            when (model) {
                is SourceUiModel.Header -> {
                    SourceHeader(
                        modifier = Modifier.animateItem(),
                        language = model.language,
                    )
                }
                is SourceUiModel.Item -> SourceItem(
                    modifier = Modifier.animateItem(),
                    source = model.source,
                    isSelected = model.source.id == selectedSourceId,
                    onClickItem = { listing ->
                        onClickItem(model.source, listing)
                        onSourceSelected(model.source)
                    },
                    onLongClickItem = onLongClickItem,
                    onClickPin = onClickPin,
                )
            }
        }
    }
}

@Composable
private fun SearchResultsHeader(
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = stringResource(MR.strings.search_results_count, itemCount),
            modifier = Modifier
                .padding(horizontal = MaterialTheme.padding.medium)
                .padding(vertical = MaterialTheme.padding.small),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SourceHeader(
    language: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val displayName = remember(language) {
        LocaleHelper.getSourceDisplayName(language, context)
    }

    Text(
        text = displayName,
        modifier = modifier
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        style = MaterialTheme.typography.header,
    )
}

@Composable
private fun SourceItem(
    source: Source,
    isSelected: Boolean,
    onClickItem: (Listing) -> Unit,
    onLongClickItem: (Source) -> Unit,
    onClickPin: (Source) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    BaseSourceItem(
        modifier = modifier.background(backgroundColor),
        source = source,
        onClickItem = { onClickItem(Listing.Popular) },
        onLongClickItem = { onLongClickItem(source) },
        action = {
            SourceItemActions(
                source = source,
                isSelected = isSelected,
                onClickLatest = { onClickItem(Listing.Latest) },
                onClickPin = { onClickPin(source) },
            )
        },
        contentColor = if (isSelected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    )
}

@Composable
private fun SourceItemActions(
    source: Source,
    isSelected: Boolean,
    onClickLatest: () -> Unit,
    onClickPin: () -> Unit,
) {
    if (source.supportsLatest) {
        TextButton(onClick = onClickLatest) {
            Text(
                text = stringResource(MR.strings.latest),
                style = LocalTextStyle.current.copy(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                ),
            )
        }
    }
    SourcePinButton(
        isPinned = Pin.Pinned in source.pin,
        isSelected = isSelected,
        onClick = onClickPin,
    )
}

@Composable
private fun SourcePinButton(
    isPinned: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val icon = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin
    val tint = when {
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
        isPinned -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY_ALPHA)
    }
    val description = if (isPinned) MR.strings.action_unpin else MR.strings.action_pin
    
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            tint = tint,
            contentDescription = stringResource(description),
        )
    }
}

@Composable
private fun EmptySearchScreen(
    modifier: Modifier = Modifier,
    hasSearchQuery: Boolean,
) {
    EmptyScreen(
        modifier = modifier,
        message = if (hasSearchQuery) {
            stringResource(MR.strings.no_results_found)
        } else {
            stringResource(MR.strings.source_empty_screen)
        },
    )
}

@Composable
fun SourceOptionsDialog(
    source: Source,
    onClickPin: () -> Unit,
    onClickDisable: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = source.visualName)
        },
        text = {
            SourceOptionsContent(
                source = source,
                onClickPin = onClickPin,
                onClickDisable = onClickDisable,
            )
        },
        onDismissRequest = onDismiss,
        confirmButton = {},
    )
}

@Composable
private fun SourceOptionsContent(
    source: Source,
    onClickPin: () -> Unit,
    onClickDisable: () -> Unit,
) {
    Column {
        val pinTextId = if (Pin.Pinned in source.pin) {
            MR.strings.action_unpin
        } else {
            MR.strings.action_pin
        }
        
        Text(
            text = stringResource(pinTextId),
            modifier = Modifier
                .clickable(onClick = onClickPin)
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        )
        
        if (!source.isLocal()) {
            Text(
                text = stringResource(MR.strings.action_disable),
                modifier = Modifier
                    .clickable(onClick = onClickDisable)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            )
        }
    }
}

// Helper functions
private fun filterSources(
    items: ImmutableList<SourceUiModel>,
    query: String,
): ImmutableList<SourceUiModel> {
    if (query.isBlank()) return items

    return items.filter { model ->
        when (model) {
            is SourceUiModel.Header -> {
                // Keep headers that have matching sources
                true // We'll filter empty headers later
            }
            is SourceUiModel.Item -> {
                model.source.name.contains(query, ignoreCase = true) ||
                    model.source.id.toString().contains(query) ||
                    model.source.lang.contains(query, ignoreCase = true)
            }
        }
    }.let { filtered ->
        // Remove headers that have no items
        val result = mutableListOf<SourceUiModel>()
        var currentHeader: SourceUiModel.Header? = null
        
        filtered.forEach { model ->
            when (model) {
                is SourceUiModel.Header -> {
                    currentHeader = model
                }
                is SourceUiModel.Item -> {
                    if (currentHeader != null) {
                        result.add(currentHeader!!)
                        currentHeader = null
                    }
                    result.add(model)
                }
            }
        }
        result.toImmutableList()
    }
}

private fun countSources(items: ImmutableList<SourceUiModel>): Int {
    return items.count { it is SourceUiModel.Item }
}

private fun <T> List<T>.toImmutableList(): ImmutableList<T> {
    return persistentListOf<T>().addAll(this)
}

// Extension for background modifier
private fun Modifier.background(color: androidx.compose.ui.graphics.Color): Modifier {
    return this.then(androidx.compose.foundation.background(color))
}

sealed interface SourceUiModel {
    data class Item(val source: Source) : SourceUiModel
    data class Header(val language: String) : SourceUiModel
}
