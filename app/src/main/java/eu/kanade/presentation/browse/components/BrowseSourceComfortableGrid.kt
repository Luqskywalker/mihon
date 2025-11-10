package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.library.components.CommonMangaItemDefaults
import eu.kanade.presentation.library.components.MangaComfortableGridItem
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.presentation.core.util.plus

@Composable
fun BrowseSourceComfortableGrid(
    mangaList: LazyPagingItems<StateFlow<Manga>>,
    columns: GridCells,
    contentPadding: PaddingValues,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
) {
    val showLoadingIndicator by remember {
        derivedStateOf {
            mangaList.loadState.refresh is LoadState.Loading || 
            mangaList.loadState.append is LoadState.Loading
        }
    }

    LaunchedEffect(mangaList.loadState) {
        if (mangaList.loadState.refresh is LoadState.Error) {
            // Handle error state if needed
        }
    }

    LazyVerticalGrid(
        columns = columns,
        contentPadding = contentPadding + PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridHorizontalSpacer),
    ) {
        // Prepend loading indicator
        if (mangaList.loadState.prepend is LoadState.Loading) {
            item(
                key = "prepend_loading",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                BrowseSourceLoadingItem()
            }
        }

        // Manga items with stable keys
        items(
            items = mangaList,
            key = { item -> 
                // Use manga ID as key for stable recompositions
                item.value.collectAsState().value.id 
            }
        ) { mangaFlow ->
            val manga by mangaFlow.collectAsState()
            BrowseSourceComfortableGridItem(
                manga = manga,
                onClick = { onMangaClick(manga) },
                onLongClick = { onMangaLongClick(manga) },
            )
        }

        // Append loading indicator
        if (showLoadingIndicator) {
            item(
                key = "append_loading",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun BrowseSourceComfortableGridItem(
    manga: Manga,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val coverData = remember(manga) {
        MangaCover(
            mangaId = manga.id,
            sourceId = manga.source,
            isMangaFavorite = manga.favorite,
            url = manga.thumbnailUrl,
            lastModified = manga.coverLastModified,
        )
    }

    MangaComfortableGridItem(
        title = manga.title,
        coverData = coverData,
        coverAlpha = if (manga.favorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = {
            InLibraryBadge(enabled = manga.favorite)
        },
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
