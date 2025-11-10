package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionFilterState
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.horizontalPadding

@Composable
fun ExtensionFilterScreen(
    navigateUp: () -> Unit,
    state: ExtensionFilterState.Success,
    onClickToggle: (String) -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
) {
    Scaffold(
        topBar = { scrollBehavior ->
            ExtensionFilterAppBar(
                navigateUp = navigateUp,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        ExtensionFilterContent(
            contentPadding = contentPadding,
            state = state,
            searchQuery = searchQuery,
            onClickLang = onClickToggle,
        )
    }
}

@Composable
private fun ExtensionFilterAppBar(
    navigateUp: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
) {
    if (searchQuery.isEmpty()) {
        AppBar(
            title = stringResource(MR.strings.label_extensions),
            navigateUp = navigateUp,
            scrollBehavior = scrollBehavior,
        )
    } else {
        SearchToolbar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            placeholderText = stringResource(MR.strings.action_filter_languages),
            onNavigateUp = navigateUp,
            scrollBehavior = scrollBehavior,
        )
    }
}

@Composable
private fun ExtensionFilterContent(
    contentPadding: PaddingValues,
    state: ExtensionFilterState.Success,
    searchQuery: String,
    onClickLang: (String) -> Unit,
) {
    val filteredLanguages = remember(state.languages, searchQuery) {
        filterLanguages(state.languages, searchQuery)
    }

    if (filteredLanguages.isEmpty) {
        EmptyFilterScreen(
            modifier = Modifier.padding(contentPadding),
            hasSearchQuery = searchQuery.isNotEmpty(),
        )
        return
    }

    LanguageList(
        contentPadding = contentPadding,
        languages = filteredLanguages,
        enabledLanguages = state.enabledLanguages,
        onClickLang = onClickLang,
    )
}

@Composable
private fun LanguageList(
    contentPadding: PaddingValues,
    languages: ImmutableList<String>,
    enabledLanguages: Set<String>,
    onClickLang: (String) -> Unit,
) {
    val context = LocalContext.current

    LazyColumn(
        contentPadding = contentPadding,
    ) {
        items(
            items = languages,
            key = { it },
        ) { language ->
            SwitchPreferenceWidget(
                modifier = Modifier
                    .animateItem()
                    .horizontalPadding(),
                title = LocaleHelper.getSourceDisplayName(language, context),
                checked = language in enabledLanguages,
                onCheckedChanged = { onClickLang(language) },
            )
        }
    }
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
            stringResource(MR.strings.empty_screen)
        },
    )
}

private fun filterLanguages(
    languages: ImmutableList<String>,
    query: String,
): ImmutableList<String> {
    if (query.isBlank()) return languages

    return languages.filter { language ->
        language.contains(query, ignoreCase = true) ||
            LocaleHelper.getSourceDisplayName(language, LocalContext.current)
                .contains(query, ignoreCase = true)
    }.toImmutableList()
}

// Extension function to convert to immutable list
private fun <T> List<T>.toImmutableList(): ImmutableList<T> = 
    if (this is ImmutableList<T>) this else persistentListOf<T>().addAll(this)
