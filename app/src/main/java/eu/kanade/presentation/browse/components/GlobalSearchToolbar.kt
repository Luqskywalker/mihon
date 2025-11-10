package eu.kanade.presentation.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun GlobalSearchToolbar(
    searchQuery: String?,
    progress: Int,
    total: Int,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    hideSourceFilter: Boolean,
    sourceFilter: SourceFilter,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onlyShowHasResults: Boolean,
    onToggleResults: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    // Memoize progress state to prevent unnecessary recomputations
    val showProgressIndicator by remember(progress, total) {
        derivedStateOf { progress in 1..<total }
    }

    val progressFraction by remember(progress, total) {
        derivedStateOf { progress / total.toFloat() }
    }

    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        SearchToolbarWithProgress(
            searchQuery = searchQuery,
            onChangeSearchQuery = onChangeSearchQuery,
            onSearch = onSearch,
            onClickCloseSearch = navigateUp,
            navigateUp = navigateUp,
            scrollBehavior = scrollBehavior,
            showProgressIndicator = showProgressIndicator,
            progressFraction = progressFraction
        )

        FilterChipsRow(
            hideSourceFilter = hideSourceFilter,
            sourceFilter = sourceFilter,
            onChangeSearchFilter = onChangeSearchFilter,
            onlyShowHasResults = onlyShowHasResults,
            onToggleResults = onToggleResults
        )

        HorizontalDivider()
    }
}

@Composable
private fun SearchToolbarWithProgress(
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onClickCloseSearch: () -> Unit,
    navigateUp: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    showProgressIndicator: Boolean,
    progressFraction: Float,
) {
    Box {
        SearchToolbar(
            searchQuery = searchQuery,
            onChangeSearchQuery = onChangeSearchQuery,
            onSearch = onSearch,
            onClickCloseSearch = onClickCloseSearch,
            navigateUp = navigateUp,
            scrollBehavior = scrollBehavior,
        )
        
        if (showProgressIndicator) {
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    hideSourceFilter: Boolean,
    sourceFilter: SourceFilter,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onlyShowHasResults: Boolean,
    onToggleResults: () -> Unit,
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        if (!hideSourceFilter) {
            SourceFilterChips(
                sourceFilter = sourceFilter,
                onChangeSearchFilter = onChangeSearchFilter
            )
            VerticalDivider()
        }

        ResultsFilterChip(
            onlyShowHasResults = onlyShowHasResults,
            onToggleResults = onToggleResults
        )
    }
}

@Composable
private fun SourceFilterChips(
    sourceFilter: SourceFilter,
    onChangeSearchFilter: (SourceFilter) -> Unit,
) {
    FilterChip(
        selected = sourceFilter == SourceFilter.PinnedOnly,
        onClick = { onChangeSearchFilter(SourceFilter.PinnedOnly) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.PushPin,
                contentDescription = stringResource(MR.strings.pinned_sources),
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
        label = {
            Text(text = stringResource(MR.strings.pinned_sources))
        },
    )
    
    FilterChip(
        selected = sourceFilter == SourceFilter.All,
        onClick = { onChangeSearchFilter(SourceFilter.All) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.DoneAll,
                contentDescription = stringResource(MR.strings.all),
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
        label = {
            Text(text = stringResource(MR.strings.all))
        },
    )
}

@Composable
private fun ResultsFilterChip(
    onlyShowHasResults: Boolean,
    onToggleResults: () -> Unit,
) {
    FilterChip(
        selected = onlyShowHasResults,
        onClick = onToggleResults,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = stringResource(MR.strings.has_results),
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
        label = {
            Text(text = stringResource(MR.strings.has_results))
        },
    )
}

// Alternative optimized version with additional features
@Composable
fun GlobalSearchToolbarOptimized(
    searchQuery: String?,
    progress: Int,
    total: Int,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    hideSourceFilter: Boolean,
    sourceFilter: SourceFilter,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onlyShowHasResults: Boolean,
    onToggleResults: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    val toolbarState = remember(
        searchQuery, progress, total, hideSourceFilter, 
        sourceFilter, onlyShowHasResults
    ) {
        derivedStateOf {
            GlobalSearchToolbarState(
                showProgressIndicator = progress in 1..<total,
                progressFraction = progress / total.toFloat(),
                showSourceFilters = !hideSourceFilter
            )
        }
    }

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        SearchToolbarWithProgress(
            searchQuery = searchQuery,
            onChangeSearchQuery = onChangeSearchQuery,
            onSearch = onSearch,
            onClickCloseSearch = navigateUp,
            navigateUp = navigateUp,
            scrollBehavior = scrollBehavior,
            showProgressIndicator = toolbarState.value.showProgressIndicator,
            progressFraction = toolbarState.value.progressFraction
        )

        if (toolbarState.value.showSourceFilters || onlyShowHasResults) {
            FilterChipsRow(
                hideSourceFilter = hideSourceFilter,
                sourceFilter = sourceFilter,
                onChangeSearchFilter = onChangeSearchFilter,
                onlyShowHasResults = onlyShowHasResults,
                onToggleResults = onToggleResults
            )
            HorizontalDivider()
        }
    }
}

private data class GlobalSearchToolbarState(
    val showProgressIndicator: Boolean,
    val progressFraction: Float,
    val showSourceFilters: Boolean
)
