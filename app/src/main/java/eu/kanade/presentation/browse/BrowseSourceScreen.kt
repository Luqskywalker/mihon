package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.browse.components.BrowseSourceComfortableGrid
import eu.kanade.presentation.browse.components.BrowseSourceCompactGrid
import eu.kanade.presentation.browse.components.BrowseSourceList
import eu.kanade.presentation.components.AppBar
import eu.kanade.tachiyomi.source.Source
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.source.local.LocalSource

@Composable
fun BrowseSourceContent(
    source: Source?,
    mangaList: LazyPagingItems<StateFlow<Manga>>,
    columns: GridCells,
    displayMode: LibraryDisplayMode,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    onWebViewClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLocalSourceHelpClick: () -> Unit,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
) {
    val context = LocalContext.current

    // Memoize error state to avoid recomputation
    val errorState = remember(mangaList.loadState) {
        mangaList.loadState.refresh.takeIf { it is LoadState.Error }
            ?: mangaList.loadState.append.takeIf { it is LoadState.Error }
    }

    // Memoize loading state
    val isLoading = remember(mangaList.loadState) {
        mangaList.itemCount == 0 && mangaList.loadState.refresh is LoadState.Loading
    }

    // Memoize empty state
    val isEmpty = remember(mangaList.itemCount, errorState) {
        mangaList.itemCount == 0
    }

    // Handle error snackbar
    LaunchedEffect(errorState) {
        if (mangaList.itemCount > 0 && errorState is LoadState.Error) {
            val result = snackbarHostState.showSnackbar(
                message = context.stringResource(MR.strings.error_loading_content),
                actionLabel = context.stringResource(MR.strings.action_retry),
                duration = SnackbarDuration.Indefinite,
            )
            when (result) {
                SnackbarResult.Dismissed -> Unit // Let it dismiss naturally
                SnackbarResult.ActionPerformed -> launchIO { mangaList.retry() }
            }
        }
    }

    // Show loading state
    if (isLoading) {
        LoadingScreen(Modifier.padding(contentPadding))
        return
    }

    // Show empty state
    if (isEmpty) {
        EmptyScreen(
            modifier = Modifier.padding(contentPadding),
            message = when (errorState) {
                is LoadState.Error -> context.stringResource(MR.strings.error_loading_content)
                else -> stringResource(MR.strings.no_results_found)
            },
            actions = getEmptyScreenActions(
                source = source,
                onRetry = mangaList::refresh,
                onWebViewClick = onWebViewClick,
                onHelpClick = onHelpClick,
                onLocalSourceHelpClick = onLocalSourceHelpClick,
            ),
        )
        return
    }

    // Show content based on display mode
    BrowseSourceDisplay(
        displayMode = displayMode,
        mangaList = mangaList,
        columns = columns,
        contentPadding = contentPadding,
        onMangaClick = onMangaClick,
        onMangaLongClick = onMangaLongClick,
    )
}

@Composable
private fun BrowseSourceDisplay(
    displayMode: LibraryDisplayMode,
    mangaList: LazyPagingItems<StateFlow<Manga>>,
    columns: GridCells,
    contentPadding: PaddingValues,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
) {
    when (displayMode) {
        LibraryDisplayMode.ComfortableGrid -> {
            BrowseSourceComfortableGrid(
                mangaList = mangaList,
                columns = columns,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
            )
        }
        LibraryDisplayMode.List -> {
            BrowseSourceList(
                mangaList = mangaList,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
            )
        }
        LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> {
            BrowseSourceCompactGrid(
                mangaList = mangaList,
                columns = columns,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
            )
        }
    }
}

@Composable
private fun getEmptyScreenActions(
    source: Source?,
    onRetry: () -> Unit,
    onWebViewClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLocalSourceHelpClick: () -> Unit,
): List<EmptyScreenAction> = remember(source) {
    when (source) {
        is LocalSource -> persistentListOf(
            EmptyScreenAction(
                stringRes = MR.strings.local_source_help_guide,
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = onLocalSourceHelpClick,
            ),
        )
        else -> persistentListOf(
            EmptyScreenAction(
                stringRes = MR.strings.action_retry,
                icon = Icons.Outlined.Refresh,
                onClick = onRetry,
            ),
            EmptyScreenAction(
                stringRes = MR.strings.action_open_in_web_view,
                icon = Icons.Outlined.Public,
                onClick = onWebViewClick,
            ),
            EmptyScreenAction(
                stringRes = MR.strings.label_help,
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = onHelpClick,
            ),
        )
    }
}

@Composable
internal fun MissingSourceScreen(
    source: StubSource,
    navigateUp: () -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = source.name,
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        EmptyScreen(
            message = stringResource(MR.strings.source_not_installed, source.toString()),
            modifier = Modifier.padding(paddingValues),
        )
    }
}
