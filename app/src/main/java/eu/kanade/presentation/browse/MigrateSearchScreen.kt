package eu.kanade.presentation.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import eu.kanade.presentation.browse.components.GlobalSearchToolbar
import eu.kanade.presentation.browse.components.MigrateSearchContent
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchScreenModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.material.Scaffold

@Composable
fun MigrateSearchScreen(
    state: SearchScreenModel.State,
    fromSourceId: Long?,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    // Migration-specific parameters
    migrationFlags: Set<String> = emptySet(),
    onMigrationFilterChange: (Set<String>) -> Unit = {},
    prioritizeByChapters: Boolean = false,
    onPrioritizeByChaptersChange: (Boolean) -> Unit = {},
) {
    // Auto-search when screen loads for migration
    LaunchedEffect(Unit) {
        if (state.searchQuery?.isNotBlank() == true && state.filteredItems.isEmpty()) {
            onSearch(state.searchQuery)
        }
    }

    Scaffold(
        topBar = { scrollBehavior ->
            MigrateSearchToolbar(
                state = state,
                navigateUp = navigateUp,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                onChangeSearchFilter = onChangeSearchFilter,
                onToggleResults = onToggleResults,
                migrationFlags = migrationFlags,
                onMigrationFilterChange = onMigrationFilterChange,
                prioritizeByChapters = prioritizeByChapters,
                onPrioritizeByChaptersChange = onPrioritizeByChaptersChange,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        MigrateSearchContent(
            state = state,
            fromSourceId = fromSourceId,
            contentPadding = paddingValues,
            getManga = getManga,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
            migrationFlags = migrationFlags,
            prioritizeByChapters = prioritizeByChapters,
        )
    }
}

@Composable
private fun MigrateSearchToolbar(
    state: SearchScreenModel.State,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    migrationFlags: Set<String>,
    onMigrationFilterChange: (Set<String>) -> Unit,
    prioritizeByChapters: Boolean,
    onPrioritizeByChaptersChange: (Boolean) -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
) {
    val migrateSearchTitle = remember {
        "Migrate Manga"
    }

    GlobalSearchToolbar(
        searchQuery = state.searchQuery,
        progress = state.progress,
        total = state.total,
        navigateUp = navigateUp,
        onChangeSearchQuery = onChangeSearchQuery,
        onSearch = onSearch,
        hideSourceFilter = true, // Always hide source filter for migration
        sourceFilter = state.sourceFilter,
        onChangeSearchFilter = onChangeSearchFilter,
        onlyShowHasResults = state.onlyShowHasResults,
        onToggleResults = onToggleResults,
        scrollBehavior = scrollBehavior,
        title = migrateSearchTitle,
        // Migration-specific actions
        additionalActions = {
            MigrateSearchActions(
                migrationFlags = migrationFlags,
                onMigrationFilterChange = onMigrationFilterChange,
                prioritizeByChapters = prioritizeByChapters,
                onPrioritizeByChaptersChange = onPrioritizeByChaptersChange,
            )
        },
    )
}

@Composable
private fun MigrateSearchActions(
    migrationFlags: Set<String>,
    onMigrationFilterChange: (Set<String>) -> Unit,
    prioritizeByChapters: Boolean,
    onPrioritizeByChaptersChange: (Boolean) -> Unit,
) {
    // Migration-specific action buttons would go here
    // For example: filter by status, language, etc.
}

@Composable
private fun MigrateSearchContent(
    state: SearchScreenModel.State,
    fromSourceId: Long?,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    migrationFlags: Set<String>,
    prioritizeByChapters: Boolean,
) {
    val filteredAndSortedItems = remember(
        state.filteredItems,
        migrationFlags,
        prioritizeByChapters,
        fromSourceId,
    ) {
        applyMigrationFiltersAndSorting(
            items = state.filteredItems,
            migrationFlags = migrationFlags,
            prioritizeByChapters = prioritizeByChapters,
            fromSourceId = fromSourceId,
        )
    }

    GlobalSearchContent(
        items = filteredAndSortedItems,
        contentPadding = contentPadding,
        getManga = getManga,
        onClickSource = onClickSource,
        onClickItem = onClickItem,
        onLongClickItem = onLongClickItem,
        fromSourceId = fromSourceId,
    )
}

// Migration-specific filtering and sorting
private fun applyMigrationFiltersAndSorting(
    items: Map<CatalogueSource, eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult>,
    migrationFlags: Set<String>,
    prioritizeByChapters: Boolean,
    fromSourceId: Long?,
): Map<CatalogueSource, eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult> {
    return items
        .filter { (source, result) ->
            // Apply migration-specific filters
            when {
                // Filter out the source we're migrating from
                source.id == fromSourceId -> false
                // Apply other migration filters based on flags
                migrationFlags.isNotEmpty() -> applyMigrationFlagFilters(source, result, migrationFlags)
                else -> true
            }
        }
        .let { filteredItems ->
            // Apply sorting based on migration preferences
            if (prioritizeByChapters) {
                sortByChapterCount(filteredItems)
            } else {
                filteredItems
            }
        }
}

private fun applyMigrationFlagFilters(
    source: CatalogueSource,
    result: eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult,
    flags: Set<String>,
): Boolean {
    // Implement migration flag-based filtering
    // Example: filter by language, status, etc.
    return when (result) {
        is eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult.Success -> {
            // Apply filters to successful results
            flags.all { flag ->
                when (flag) {
                    "has_matches" -> result.result.isNotEmpty()
                    "same_language" -> true // Implement language matching
                    "has_updates" -> true // Implement update checking
                    else -> true
                }
            }
        }
        else -> true // Don't filter loading/error states
    }
}

private fun sortByChapterCount(
    items: Map<CatalogueSource, eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult>,
): Map<CatalogueSource, eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult> {
    return items.entries
        .sortedByDescending { (_, result) ->
            when (result) {
                is eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult.Success -> {
                    // Sort by total chapter count or match quality
                    result.result.size
                }
                else -> 0
            }
        }
        .associate { it.key to it.value }
}

// Migration-specific empty state
@Composable
fun MigrateSearchEmptyState(
    searchQuery: String,
    migrationFlags: Set<String>,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    // Custom empty state for migration search
}

// Migration search statistics
@Composable
fun MigrateSearchStats(
    state: SearchScreenModel.State,
    fromSourceId: Long?,
    migrationFlags: Set<String>,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    val stats = remember(state.filteredItems, fromSourceId, migrationFlags) {
        val totalSources = state.filteredItems.size
        val filteredSources = state.filteredItems.count { it.key.id != fromSourceId }
        val successfulMatches = state.filteredItems.values
            .filterIsInstance<eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult.Success>()
            .count { it.result.isNotEmpty() }
        val totalMatches = state.filteredItems.values
            .filterIsInstance<eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult.Success>()
            .sumOf { it.result.size }

        MigrateSearchStatsData(
            totalSources = totalSources,
            filteredSources = filteredSources,
            successfulMatches = successfulMatches,
            totalMatches = totalMatches,
        )
    }

    // Display migration-specific statistics
}

private data class MigrateSearchStatsData(
    val totalSources: Int,
    val filteredSources: Int,
    val successfulMatches: Int,
    val totalMatches: Int,
)

// Quick migration actions
@Composable
fun QuickMigrateActions(
    onQuickMigrate: (CatalogueSource) -> Unit,
    suggestedSources: List<CatalogueSource>,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    // Quick migration buttons for popular sources
}
