package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.tachiyomi.ui.browse.source.SourcesFilterScreenModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.horizontalPadding

@Composable
fun SourcesFilterScreen(
    navigateUp: () -> Unit,
    state: SourcesFilterScreenModel.State.Success,
    onClickLanguage: (String) -> Unit,
    onClickSource: (Source) -> Unit,
    // Enhanced features
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    selectedLanguages: Set<String> = emptySet(),
    onToggleAllLanguages: (Boolean) -> Unit = {},
) {
    Scaffold(
        topBar = { scrollBehavior ->
            SourcesFilterAppBar(
                navigateUp = navigateUp,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        SourcesFilterContent(
            contentPadding = contentPadding,
            state = state,
            searchQuery = searchQuery,
            onClickLanguage = onClickLanguage,
            onClickSource = onClickSource,
            selectedLanguages = selectedLanguages,
            onToggleAllLanguages = onToggleAllLanguages,
        )
    }
}

@Composable
private fun SourcesFilterAppBar(
    navigateUp: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
) {
    if (searchQuery.isEmpty()) {
        AppBar(
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
private fun SourcesFilterContent(
    contentPadding: PaddingValues,
    state: SourcesFilterScreenModel.State.Success,
    searchQuery: String,
    onClickLanguage: (String) -> Unit,
    onClickSource: (Source) -> Unit,
    selectedLanguages: Set<String>,
    onToggleAllLanguages: (Boolean) -> Unit,
) {
    val filteredItems = remember(state.items, searchQuery) {
        filterSourcesByQuery(state.items, searchQuery)
    }

    if (filteredItems.isEmpty()) {
        EmptyFilterScreen(
            modifier = Modifier.padding(contentPadding),
            hasSearchQuery = searchQuery.isNotEmpty(),
        )
        return
    }

    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        // Bulk actions header
        if (searchQuery.isEmpty()) {
            item(key = "bulk-actions") {
                BulkLanguageActions(
                    totalLanguages = state.items.size,
                    selectedCount = state.enabledLanguages.size,
                    onToggleAll = onToggleAllLanguages,
                    modifier = Modifier.animateItemFastScroll(),
                )
            }
        }

        // Search results count
        if (searchQuery.isNotEmpty()) {
            item(key = "search-results") {
                SearchResultsHeader(
                    totalItems = countTotalSources(state.items),
                    filteredItems = countTotalSources(filteredItems),
                    modifier = Modifier.animateItemFastScroll(),
                )
            }
        }

        filteredItems.forEach { (language, sources) ->
            val isLanguageEnabled = language in state.enabledLanguages
            val isLanguageSelected = language in selectedLanguages

            item(
                key = "lang-$language",
                contentType = "source-filter-header",
            ) {
                SourcesFilterHeader(
                    modifier = Modifier.animateItemFastScroll(),
                    language = language,
                    enabled = isLanguageEnabled,
                    isSelected = isLanguageSelected,
                    sourceCount = sources.size,
                    onClickItem = onClickLanguage,
                )
            }

            if (isLanguageEnabled) {
                items(
                    items = sources,
                    key = { "source-${it.id}" },
                    contentType = { "source-filter-item" },
                ) { source ->
                    SourcesFilterItem(
                        modifier = Modifier.animateItemFastScroll(),
                        source = source,
                        enabled = "${source.id}" !in state.disabledSources,
                        onClickItem = onClickSource,
                    )
                }
            }
        }
    }
}

@Composable
private fun BulkLanguageActions(
    totalLanguages: Int,
    selectedCount: Int,
    onToggleAll: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allSelected = selectedCount == totalLanguages
    val someSelected = selectedCount > 0 && selectedCount < totalLanguages

    SwitchPreferenceWidget(
        modifier = modifier.horizontalPadding(),
        title = stringResource(MR.strings.action_select_all),
        subtitle = stringResource(MR.strings.languages_selected_count, selectedCount, totalLanguages),
        checked = allSelected,
        onCheckedChanged = { onToggleAll(!allSelected) },
        enabled = totalLanguages > 0,
    )
}

@Composable
private fun SearchResultsHeader(
    totalItems: Int,
    filteredItems: Int,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
    ) {
        androidx.compose.material3.Text(
            text = stringResource(MR.strings.search_results_count, filteredItems, totalItems),
            modifier = Modifier
                .padding(horizontal = androidx.compose.material3.MaterialTheme.padding.medium)
                .padding(vertical = androidx.compose.material3.MaterialTheme.padding.small),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SourcesFilterHeader(
    language: String,
    enabled: Boolean,
    isSelected: Boolean,
    sourceCount: Int,
    onClickItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val displayName = remember(language) {
        LocaleHelper.getSourceDisplayName(language, context)
    }

    SwitchPreferenceWidget(
        modifier = modifier,
        title = displayName,
        subtitle = stringResource(MR.strings.source_count, sourceCount),
        checked = enabled,
        onCheckedChanged = { onClickItem(language) },
        // Visual indication for selection state
        titleColor = if (isSelected) {
            androidx.compose.material3.MaterialTheme.colorScheme.primary
        } else {
            androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        },
    )
}

@Composable
private fun SourcesFilterItem(
    source: Source,
    enabled: Boolean,
    onClickItem: (Source) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseSourceItem(
        modifier = modifier,
        source = source,
        showLanguageInContent = false,
        onClickItem = { onClickItem(source) },
        action = {
            Checkbox(
                checked = enabled,
                onCheckedChange = null,
                enabled = true,
            )
        },
        // Visual feedback for enabled/disabled state
        contentAlpha = if (enabled) 1f else 0.6f,
    )
}

@Composable
private fun EmptyFilterScreen(
    modifier: Modifier = Modifier,
    hasSearchQuery: Boolean,
) {
    EmptyScreen(
        modifier = modifier,
        message = if (hasSearchQuery) {
            stringResource(MR.strings.no_results_found)
        } else {
            stringResource(MR.strings.source_filter_empty_screen)
        },
    )
}

// Helper functions
private fun filterSourcesByQuery(
    items: ImmutableMap<String, ImmutableList<Source>>,
    query: String,
): ImmutableMap<String, ImmutableList<Source>> {
    if (query.isBlank()) return items

    return items.mapNotNull { (language, sources) ->
        val filteredSources = sources.filter { source ->
            source.name.contains(query, ignoreCase = true) ||
                source.id.toString().contains(query) ||
                language.contains(query, ignoreCase = true)
        }
        if (filteredSources.isNotEmpty()) {
            language to filteredSources.toImmutableList()
        } else {
            null
        }
    }.toMap().toImmutableMap()
}

private fun countTotalSources(items: ImmutableMap<String, ImmutableList<Source>>): Int {
    return items.values.sumOf { it.size }
}

private fun <K, V> Map<K, V>.toImmutableMap(): ImmutableMap<K, V> {
    return persistentListOf<Pair<K, V>>().addAll(this.entries.map { it.key to it.value }).toMap()
}

private fun <T> List<T>.toImmutableList(): ImmutableList<T> {
    return persistentListOf<T>().addAll(this)
}

// Extension for better performance
private val androidx.compose.material3.MaterialTheme.padding
    @Composable get() = tachiyomi.presentation.core.components.material.padding
