package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.library.components.CommonMangaItemDefaults
import eu.kanade.presentation.library.components.MangaListItem
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.presentation.core.util.plus

@Composable
fun BrowseSourceList(
    mangaList: LazyPagingItems<StateFlow<Manga>>,
    contentPadding: PaddingValues,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
) {
    val showLoadingIndicator by remember(mangaList.loadState) {
        derivedStateOf {
            mangaList.loadState.refresh is LoadState.Loading || 
            mangaList.loadState.append is LoadState.Loading
        }
    }

    // Handle error states if needed
    LaunchedEffect(mangaList.loadState) {
        if (mangaList.loadState.refresh is LoadState.Error) {
            // You could trigger error handling logic here
        }
    }

    LazyColumn(
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
    ) {
        // Prepend loading indicator
        if (mangaList.loadState.prepend is LoadState.Loading) {
            item(key = "prepend_loading") {
                BrowseSourceLoadingItem()
            }
        }

        // Manga items with stable keys for better performance
        items(
            items = mangaList,
            key = { mangaFlow -> 
                // Use the manga ID as a stable key to prevent unnecessary recompositions
                val mangaState = mangaFlow.collectAsState()
                mangaState.value.id
            }
        ) { mangaFlow ->
            val manga by mangaFlow.collectAsState()
            BrowseSourceListItem(
                manga = manga,
                onClick = { onMangaClick(manga) },
                onLongClick = { onMangaLongClick(manga) },
            )
        }

        // Append loading indicator
        if (showLoadingIndicator) {
            item(key = "append_loading") {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun BrowseSourceListItem(
    manga: Manga,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    // Memoize the MangaCover creation to avoid recreating it on every recomposition
    val coverData = remember(manga.id, manga.thumbnailUrl, manga.coverLastModified) {
        MangaCover(
            mangaId = manga.id,
            sourceId = manga.source,
            isMangaFavorite = manga.favorite,
            url = manga.thumbnailUrl,
            lastModified = manga.coverLastModified,
        )
    }

    // Memoize the cover alpha calculation
    val coverAlpha = remember(manga.favorite) {
        if (manga.favorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f
    }

    MangaListItem(
        title = manga.title,
        coverData = coverData,
        coverAlpha = coverAlpha,
        badge = {
            // Use the optimized badge component
            InLibraryBadgeOptimized(enabled = manga.favorite)
        },
        onLongClick = onLongClick,
        onClick = onClick,
    )
}

// Alternative version with even more optimization for large lists
@Composable
private fun BrowseSourceListItemOptimized(
    manga: Manga,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    // Extract only the necessary manga properties to minimize recomposition scope
    val mangaId = manga.id
    val title = manga.title
    val isFavorite = manga.favorite
    val thumbnailUrl = manga.thumbnailUrl
    val coverLastModified = manga.coverLastModified
    val sourceId = manga.source

    val coverData = remember(mangaId, thumbnailUrl, coverLastModified) {
        MangaCover(
            mangaId = mangaId,
            sourceId = sourceId,
            isMangaFavorite = isFavorite,
            url = thumbnailUrl,
            lastModified = coverLastModified,
        )
    }

    val coverAlpha = remember(isFavorite) {
        if (isFavorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f
    }

    MangaListItem(
        title = title,
        coverData = coverData,
        coverAlpha = coverAlpha,
        badge = {
            InLibraryBadgeStable(enabled = isFavorite)
        },
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
