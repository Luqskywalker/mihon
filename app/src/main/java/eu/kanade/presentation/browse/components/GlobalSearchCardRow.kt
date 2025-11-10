package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.library.components.CommonMangaItemDefaults
import eu.kanade.presentation.library.components.MangaComfortableGridItem
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun GlobalSearchCardRow(
    titles: List<Manga>,
    getManga: @Composable (Manga) -> State<Manga>,
    onClick: (Manga) -> Unit,
    onLongClick: (Manga) -> Unit,
) {
    if (titles.isEmpty()) {
        EmptyResultItem()
        return
    }

    LazyRow(
        contentPadding = PaddingValues(MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        items(
            items = titles,
            key = { manga -> manga.id } // Stable keys for better performance
        ) { manga ->
            val titleState by getManga(manga)
            MangaItem(
                manga = titleState,
                onClick = { onClick(titleState) },
                onLongClick = { onLongClick(titleState) },
            )
        }
    }
}

@Composable
private fun MangaItem(
    manga: Manga,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    // Memoize the cover to prevent recreation on every recomposition
    val cover = remember(manga) { manga.asMangaCover() }
    
    // Memoize the cover alpha calculation
    val coverAlpha = remember(manga.favorite) {
        if (manga.favorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f
    }

    Box(modifier = Modifier.width(96.dp)) {
        MangaComfortableGridItem(
            title = manga.title,
            titleMaxLines = 3,
            coverData = cover,
            coverBadgeStart = {
                // Use optimized badge component
                InLibraryBadgeOptimized(enabled = manga.favorite)
            },
            coverAlpha = coverAlpha,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

@Composable
private fun EmptyResultItem() {
    Text(
        text = stringResource(MR.strings.no_results_found),
        modifier = Modifier
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        style = MaterialTheme.typography.bodyMedium,
    )
}

// Alternative optimized version for better performance
@Composable
fun GlobalSearchCardRowOptimized(
    titles: List<Manga>,
    getManga: @Composable (Manga) -> State<Manga>,
    onClick: (Manga) -> Unit,
    onLongClick: (Manga) -> Unit,
) {
    when {
        titles.isEmpty() -> EmptyResultItem()
        else -> {
            LazyRow(
                contentPadding = PaddingValues(MaterialTheme.padding.small),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            ) {
                items(
                    items = titles,
                    key = { manga -> manga.id }
                ) { manga ->
                    val titleState by getManga(manga)
                    OptimizedMangaItem(
                        manga = titleState,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun OptimizedMangaItem(
    manga: Manga,
    onClick: (Manga) -> Unit,
    onLongClick: (Manga) -> Unit,
) {
    // Extract only necessary properties to minimize recomposition scope
    val mangaId = manga.id
    val title = manga.title
    val isFavorite = manga.favorite
    
    val cover = remember(mangaId) { manga.asMangaCover() }
    val coverAlpha = remember(isFavorite) {
        if (isFavorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f
    }

    Box(modifier = Modifier.width(96.dp)) {
        MangaComfortableGridItem(
            title = title,
            titleMaxLines = 3,
            coverData = cover,
            coverBadgeStart = {
                InLibraryBadgeStable(enabled = isFavorite)
            },
            coverAlpha = coverAlpha,
            onClick = { onClick(manga) },
            onLongClick = { onLongClick(manga) },
        )
    }
}
