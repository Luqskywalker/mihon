package eu.kanade.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.zIndex
import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TabbedScreen(
    titleRes: StringResource,
    tabs: ImmutableList<TabContent>,
    state: PagerState = rememberPagerState { tabs.size },
    searchQuery: String? = null,
    onChangeSearchQuery: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            val currentTab = tabs[state.currentPage]
            TabbedScreenToolbar(
                titleRes = titleRes,
                currentTab = currentTab,
                searchQuery = searchQuery,
                onChangeSearchQuery = onChangeSearchQuery,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) { contentPadding ->
        TabbedScreenContent(
            tabs = tabs,
            state = state,
            contentPadding = contentPadding,
            snackbarHostState = snackbarHostState,
            scope = scope,
        )
    }
}

@Composable
private fun TabbedScreenToolbar(
    titleRes: StringResource,
    currentTab: TabContent,
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
) {
    val searchEnabled = currentTab.searchEnabled
    val effectiveSearchQuery = if (searchEnabled) searchQuery else null

    SearchToolbar(
        titleContent = { AppBarTitle(stringResource(titleRes)) },
        searchEnabled = searchEnabled,
        searchQuery = effectiveSearchQuery,
        onChangeSearchQuery = onChangeSearchQuery,
        actions = { AppBarActions(currentTab.actions) },
    )
}

@Composable
private fun TabbedScreenContent(
    tabs: ImmutableList<TabContent>,
    state: PagerState,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val layoutDirection = LocalLayoutDirection.current

    Column(
        modifier = Modifier.padding(
            top = contentPadding.calculateTopPadding(),
            start = contentPadding.calculateStartPadding(layoutDirection),
            end = contentPadding.calculateEndPadding(layoutDirection),
        ),
    ) {
        TabRow(
            tabs = tabs,
            state = state,
            scope = scope,
        )

        TabContentPager(
            tabs = tabs,
            state = state,
            contentPadding = contentPadding,
            snackbarHostState = snackbarHostState,
        )
    }
}

@Composable
private fun TabRow(
    tabs: ImmutableList<TabContent>,
    state: PagerState,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    PrimaryTabRow(
        selectedTabIndex = state.currentPage,
        modifier = Modifier.zIndex(1f),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = state.currentPage == index,
                onClick = { 
                    scope.launch { 
                        state.animateScrollToPage(index) 
                    } 
                },
                text = { 
                    TabText(
                        text = stringResource(tab.titleRes), 
                        badgeCount = tab.badgeNumber,
                        maxLines = 1,
                    ) 
                },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TabContentPager(
    tabs: ImmutableList<TabContent>,
    state: PagerState,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
) {
    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = state,
        verticalAlignment = Alignment.Top,
        userScrollEnabled = true,
    ) { page ->
        key(page) {
            tabs[page].content(
                PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                snackbarHostState,
            )
        }
    }
}

// Optimized version with additional features
@Composable
fun TabbedScreenOptimized(
    titleRes: StringResource,
    tabs: ImmutableList<TabContent>,
    state: PagerState = rememberPagerState { tabs.size },
    searchQuery: String? = null,
    onChangeSearchQuery: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Notify when page changes
    LaunchedEffect(state.currentPage) {
        onPageChanged(state.currentPage)
    }

    // Set initial page if different from current
    LaunchedEffect(initialPage) {
        if (state.currentPage != initialPage) {
            state.scrollToPage(initialPage)
        }
    }

    Scaffold(
        topBar = {
            val currentTab = tabs.getOrNull(state.currentPage) ?: tabs.first()
            TabbedScreenToolbar(
                titleRes = titleRes,
                currentTab = currentTab,
                searchQuery = searchQuery,
                onChangeSearchQuery = onChangeSearchQuery,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) { contentPadding ->
        TabbedScreenContent(
            tabs = tabs,
            state = state,
            contentPadding = contentPadding,
            snackbarHostState = snackbarHostState,
            scope = scope,
        )
    }
}

// Simple version without search functionality
@Composable
fun SimpleTabbedScreen(
    titleRes: StringResource,
    tabs: ImmutableList<TabContent>,
    state: PagerState = rememberPagerState { tabs.size },
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(titleRes),
                actions = {
                    val currentTab = tabs[state.currentPage]
                    AppBarActions(currentTab.actions)
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) { contentPadding ->
        TabbedScreenContent(
            tabs = tabs,
            state = state,
            contentPadding = contentPadding,
            snackbarHostState = snackbarHostState,
            scope = scope,
        )
    }
}

// Data class for tab configuration with additional options
data class TabContent(
    val titleRes: StringResource,
    val badgeNumber: Int? = null,
    val searchEnabled: Boolean = false,
    val actions: ImmutableList<AppBar.AppBarAction> = persistentListOf(),
    val content: @Composable (contentPadding: PaddingValues, snackbarHostState: SnackbarHostState) -> Unit,
)

// Extension function for creating tab content
@Composable
fun TabContent(
    titleRes: StringResource,
    badgeNumber: Int? = null,
    searchEnabled: Boolean = false,
    actions: ImmutableList<AppBar.AppBarAction> = persistentListOf(),
    content: @Composable (contentPadding: PaddingValues, snackbarHostState: SnackbarHostState) -> Unit,
): TabContent {
    return TabContent(
        titleRes = titleRes,
        badgeNumber = badgeNumber,
        searchEnabled = searchEnabled,
        actions = actions,
        content = content,
    )
}

// Utility for creating tab lists
@Composable
fun rememberTabContentList(
    vararg tabs: TabContent,
): ImmutableList<TabContent> {
    return remember(tabs) {
        persistentListOf(*tabs)
    }
}

// Configuration for tabbed screen behavior
data class TabbedScreenConfig(
    val enableSearch: Boolean = true,
    val enableSwipe: Boolean = true,
    val lazyLoadTabs: Boolean = true,
    val tabScrollable: Boolean = true,
)

@Composable
fun rememberTabbedScreenConfig(
    enableSearch: Boolean = true,
    enableSwipe: Boolean = true,
    lazyLoadTabs: Boolean = true,
    tabScrollable: Boolean = true,
): TabbedScreenConfig {
    return remember(enableSearch, enableSwipe, lazyLoadTabs, tabScrollable) {
        TabbedScreenConfig(
            enableSearch = enableSearch,
            enableSwipe = enableSwipe,
            lazyLoadTabs = lazyLoadTabs,
            tabScrollable = tabScrollable,
        )
    }
}
