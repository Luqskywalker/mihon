package eu.kanade.presentation.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.tachiyomi.ui.browse.migration.sources.MigrateSourceScreenModel
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.Scroller.STICKY_HEADER_KEY_PREFIX
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun MigrateSourceScreen(
    state: MigrateSourceScreenModel.State,
    contentPadding: PaddingValues,
    onClickItem: (Source) -> Unit,
    onToggleSortingDirection: () -> Unit,
    onToggleSortingMode: () -> Unit,
    // Additional migration features
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    selectedSourceId: Long? = null,
    onSourceSelected: (Source) -> Unit = {},
) {
    val context = LocalContext.current

    // Auto-scroll to selected source if any
    LaunchedEffect(selectedSourceId) {
        // Could implement auto-scroll logic here
    }

    when {
        state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
        state.isEmpty -> EmptyScreen(
            stringRes = MR.strings.information_empty_library,
            modifier = Modifier.padding(contentPadding),
        )
        else -> MigrateSourceContent(
            state = state,
            contentPadding = contentPadding,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onClickItem = onClickItem,
            onLongClickItem = { source ->
                val sourceId = source.id.toString()
                context.copyToClipboard(sourceId, sourceId)
            },
            sortingMode = state.sortingMode,
            onToggleSortingMode = onToggleSortingMode,
            sortingDirection = state.sortingDirection,
            onToggleSortingDirection = onToggleSortingDirection,
            selectedSourceId = selectedSourceId,
            onSourceSelected = onSourceSelected,
        )
    }
}

@Composable
private fun MigrateSourceContent(
    state: MigrateSourceScreenModel.State,
    contentPadding: PaddingValues,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClickItem: (Source) -> Unit,
    onLongClickItem: (Source) -> Unit,
    sortingMode: SetMigrateSorting.Mode,
    onToggleSortingMode: () -> Unit,
    sortingDirection: SetMigrateSorting.Direction,
    onToggleSortingDirection: () -> Unit,
    selectedSourceId: Long?,
    onSourceSelected: (Source) -> Unit,
) {
    val filteredAndSortedList = remember(
        state.items,
        searchQuery,
        sortingMode,
        sortingDirection,
    ) {
        applySourceFiltersAndSorting(
            list = state.items,
            searchQuery = searchQuery,
            sortingMode = sortingMode,
            sortingDirection = sortingDirection,
        )
    }

    ScrollbarLazyColumn(
        contentPadding = contentPadding + topSmallPaddingValues,
    ) {
        if (searchQuery.isNotEmpty()) {
            item(key = "search-results-count") {
                SearchResultsHeader(
                    totalCount = state.items.size,
                    filteredCount = filteredAndSortedList.size,
                    searchQuery = searchQuery,
                )
            }
        }

        stickyHeader(key = STICKY_HEADER_KEY_PREFIX) {
            MigrateSourceListHeader(
                sortingMode = sortingMode,
                sortingDirection = sortingDirection,
                onToggleSortingMode = onToggleSortingMode,
                onToggleSortingDirection = onToggleSortingDirection,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
            )
        }

        items(
            items = filteredAndSortedList,
            key = { (source, _) -> "migrate-${source.id}" },
        ) { (source, count) ->
            MigrateSourceItem(
                modifier = Modifier.animateItem(),
                source = source,
                count = count,
                isSelected = source.id == selectedSourceId,
                onClickItem = { 
                    onClickItem(source)
                    onSourceSelected(source)
                },
                onLongClickItem = { onLongClickItem(source) },
            )
        }
    }
}

@Composable
private fun MigrateSourceListHeader(
    sortingMode: SetMigrateSorting.Mode,
    sortingDirection: SetMigrateSorting.Direction,
    onToggleSortingMode: () -> Unit,
    onToggleSortingDirection: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
    ) {
        // Search bar could be added here for filtering sources
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(MR.strings.migration_selection_prompt),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.header,
            )

            SortingControls(
                sortingMode = sortingMode,
                sortingDirection = sortingDirection,
                onToggleSortingMode = onToggleSortingMode,
                onToggleSortingDirection = onToggleSortingDirection,
            )
        }
    }
}

@Composable
private fun SortingControls(
    sortingMode: SetMigrateSorting.Mode,
    sortingDirection: SetMigrateSorting.Direction,
    onToggleSortingMode: () -> Unit,
    onToggleSortingDirection: () -> Unit,
) {
    Row {
        IconButton(onClick = onToggleSortingMode) {
            when (sortingMode) {
                SetMigrateSorting.Mode.ALPHABETICAL -> Icon(
                    Icons.Outlined.SortByAlpha,
                    contentDescription = stringResource(MR.strings.action_sort_alpha),
                )
                SetMigrateSorting.Mode.TOTAL -> Icon(
                    Icons.Outlined.Numbers,
                    contentDescription = stringResource(MR.strings.action_sort_count),
                )
            }
        }
        IconButton(onClick = onToggleSortingDirection) {
            when (sortingDirection) {
                SetMigrateSorting.Direction.ASCENDING -> Icon(
                    Icons.Outlined.ArrowUpward,
                    contentDescription = stringResource(MR.strings.action_asc),
                )
                SetMigrateSorting.Direction.DESCENDING -> Icon(
                    Icons.Outlined.ArrowDownward,
                    contentDescription = stringResource(MR.strings.action_desc),
                )
            }
        }
    }
}

@Composable
private fun SearchResultsHeader(
    totalCount: Int,
    filteredCount: Int,
    searchQuery: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(MR.strings.search_results),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "$filteredCount/$totalCount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MigrateSourceItem(
    source: Source,
    count: Long,
    isSelected: Boolean,
    onClickItem: () -> Unit,
    onLongClickItem: () -> Unit,
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
        showLanguageInContent = source.lang.isNotEmpty(),
        onClickItem = onClickItem,
        onLongClickItem = onLongClickItem,
        icon = { 
            SourceIcon(
                source = source,
                isSelected = isSelected,
            ) 
        },
        action = {
            SourceBadge(
                count = count,
                isSelected = isSelected,
            )
        },
        content = { _, sourceLangString ->
            SourceContent(
                source = source,
                sourceLangString = sourceLangString,
                isSelected = isSelected,
            )
        },
    )
}

@Composable
private fun SourceBadge(
    count: Long,
    isSelected: Boolean,
) {
    BadgeGroup {
        Badge(
            text = count.formatCount(),
            backgroundColor = if (isSelected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            },
            textColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimary
            },
        )
    }
}

@Composable
private fun SourceContent(
    source: Source,
    sourceLangString: String?,
    isSelected: Boolean,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.padding.medium)
            .weight(1f),
    ) {
        Text(
            text = source.name.ifBlank { source.id.toString() },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (sourceLangString != null) {
                Text(
                    modifier = Modifier.secondaryItemAlpha(),
                    text = sourceLangString,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                )
            }
            if (source.isStub) {
                Text(
                    modifier = Modifier.secondaryItemAlpha(),
                    text = stringResource(MR.strings.not_installed),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// Helper functions
private fun applySourceFiltersAndSorting(
    list: ImmutableList<Pair<Source, Long>>,
    searchQuery: String,
    sortingMode: SetMigrateSorting.Mode,
    sortingDirection: SetMigrateSorting.Direction,
): ImmutableList<Pair<Source, Long>> {
    var result = list

    // Apply search filter
    if (searchQuery.isNotBlank()) {
        result = result.filter { (source, _) ->
            source.name.contains(searchQuery, ignoreCase = true) ||
                source.id.toString().contains(searchQuery)
        }.toImmutableList()
    }

    // Apply sorting
    result = when (sortingMode) {
        SetMigrateSorting.Mode.ALPHABETICAL -> {
            result.sortedBy { (source, _) -> source.name.lowercase() }
        }
        SetMigrateSorting.Mode.TOTAL -> {
            result.sortedBy { (_, count) -> count }
        }
    }.toImmutableList()

    // Apply direction
    if (sortingDirection == SetMigrateSorting.Direction.DESCENDING) {
        result = result.reversed().toImmutableList()
    }

    return result
}

private fun Long.formatCount(): String {
    return when {
        this >= 1_000_000 -> "${this / 1_000_000}M"
        this >= 1_000 -> "${this / 1_000}K"
        else -> toString()
    }
}

// Extension for immutable list conversion
private fun <T> List<T>.toImmutableList(): ImmutableList<T> {
    return kotlinx.collections.immutable.persistentListOf<T>().addAll(this)
}
