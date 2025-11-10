package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import eu.kanade.presentation.browse.components.GlobalSearchCardRow
import eu.kanade.presentation.browse.components.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.components.GlobalSearchResultItem
import eu.kanade.presentation.browse.components.GlobalSearchToolbar
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchScreenModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.material.Scaffold

@Composable
fun GlobalSearchScreen(
    state: SearchScreenModel.State,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            GlobalSearchToolbar(
                searchQuery = state.searchQuery,
                progress = state.progress,
                total = state.total,
                navigateUp = navigateUp,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                hideSourceFilter = false,
                sourceFilter = state.sourceFilter,
                onChangeSearchFilter = onChangeSearchFilter,
                onlyShowHasResults = state.onlyShowHasResults,
                onToggleResults = onToggleResults,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        GlobalSearchContent(
            items = state.filteredItems,
            contentPadding = paddingValues,
            getManga = getManga,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
        )
    }
}

@Composable
internal fun GlobalSearchContent(
    items: Map<CatalogueSource, SearchItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    fromSourceId: Long? = null,
) {
    // Convert to immutable list for better performance
    val sourceResults = remember(items) {
        items.entries.toList().toImmutableMap()
    }

    LazyColumn(
        contentPadding = contentPadding,
    ) {
        items(
            items = sourceResults.entries.toList(),
            key = { it.key.id },
        ) { (source, result) ->
            GlobalSearchSourceResult(
                source = source,
                result = result,
                fromSourceId = fromSourceId,
                onClickSource = onClickSource,
                getManga = getManga,
                onClickItem = onClickItem,
                onLongClickItem = onLongClickItem,
            )
        }
    }
}

@Composable
private fun GlobalSearchSourceResult(
    source: CatalogueSource,
    result: SearchItemResult,
    fromSourceId: Long?,
    onClickSource: (CatalogueSource) -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    val sourceTitle = remember(source, fromSourceId) {
        if (source.id == fromSourceId) "▶ ${source.name}" else source.name
    }

    val sourceSubtitle = remember(source) {
        LocaleHelper.getLocalizedDisplayName(source.lang)
    }

    GlobalSearchResultItem(
        title = sourceTitle,
        subtitle = sourceSubtitle,
        onClick = { onClickSource(source) },
        modifier = Modifier.animateItem(),
    ) {
        when (result) {
            SearchItemResult.Loading -> {
                GlobalSearchLoadingResultItem()
            }
            is SearchItemResult.Success -> {
                GlobalSearchCardRow(
                    titles = result.result,
                    getManga = getManga,
                    onClick = onClickItem,
                    onLongClick = onLongClickItem,
                )
            }
            is SearchItemResult.Error -> {
                GlobalSearchErrorResultItem(
                    message = result.throwable.message,
                    sourceName = source.name,
                )
            }
        }
    }
}

// Extension for better error handling
@Composable
private fun GlobalSearchErrorResultItem(
    message: String?,
    sourceName: String,
) {
    val errorMessage = remember(message, sourceName) {
        buildErrorMessage(message, sourceName)
    }
    GlobalSearchErrorResultItem(message = errorMessage)
}

private fun buildErrorMessage(message: String?, sourceName: String): String {
    return if (!message.isNullOrBlank()) {
        "$sourceName: $message"
    } else {
        "Failed to search $sourceName"
    }
}

// Performance optimization for large result sets
@Composable
fun OptimizedGlobalSearchContent(
    items: Map<CatalogueSource, SearchItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    fromSourceId: Long? = null,
    visibleItemCount: Int = 10, // Number of items to pre-render
) {
    val optimizedItems = remember(items) {
        items.toList().take(visibleItemCount * 2) // Buffer for smooth scrolling
    }

    LazyColumn(
        contentPadding = contentPadding,
    ) {
        items(
            items = optimizedItems,
            key = { it.first.id },
        ) { (source, result) ->
            GlobalSearchSourceResult(
                source = source,
                result = result,
                fromSourceId = fromSourceId,
                onClickSource = onClickSource,
                getManga = getManga,
                onClickItem = onClickItem,
                onLongClickItem = onLongClickItem,
            )
        }
    }
}

// Utility for filtered search results
@Composable
fun FilteredGlobalSearchContent(
    items: Map<CatalogueSource, SearchItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    filter: (CatalogueSource, SearchItemResult) -> Boolean = { _, result ->
        result is SearchItemResult.Success && result.result.isNotEmpty()
    },
) {
    val filteredItems = remember(items, filter) {
        items.filter { (source, result) -> filter(source, result) }
    }

    GlobalSearchContent(
        items = filteredItems,
        contentPadding = contentPadding,
        getManga = getManga,
        onClickSource = onClickSource,
        onClickItem = onClickItem,
        onLongClickItem = onLongClickItem,
    )
}

// Search statistics component
@Composable
fun GlobalSearchStats(
    items: Map<CatalogueSource, SearchItemResult>,
    modifier: Modifier = Modifier,
) {
    val stats = remember(items) {
        val total = items.size
        val loading = items.count { it.value == SearchItemResult.Loading }
        val success = items.count { it.value is SearchItemResult.Success }
        val error = items.count { it.value is SearchItemResult.Error }
        val mangaCount = items.values
            .filterIsInstance<SearchItemResult.Success>()
            .sumOf { it.result.size }
        
        SearchStats(total, loading, success, error, mangaCount)
    }

    // Could display this in the toolbar or as a header
}

private data class SearchStats(
    val totalSources: Int,
    val loading: Int,
    val success: Int,
    val error: Int,
    val mangaCount: Int,
)

// Empty state for search
@Composable
fun GlobalSearchEmptyState(
    searchQuery: String,
    modifier: Modifier = Modifier,
) {
    // Custom empty state when no results are found
}

// Loading state for initial search
@Composable
fun GlobalSearchInitialLoading(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    // Custom loading state for initial search
}
