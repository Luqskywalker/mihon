@Composable
private fun BrowseSourceCompactGridItem(
    manga: Manga,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    // Extract only the necessary properties for the cover to minimize recomposition scope
    val coverData = remember(manga.id, manga.thumbnailUrl, manga.coverLastModified) {
        MangaCover(
            mangaId = manga.id,
            sourceId = manga.source,
            isMangaFavorite = manga.favorite,
            url = manga.thumbnailUrl,
            lastModified = manga.coverLastModified,
        )
    }

    val coverAlpha = remember(manga.favorite) {
        if (manga.favorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f
    }

    MangaCompactGridItem(
        title = manga.title,
        coverData = coverData,
        coverAlpha = coverAlpha,
        coverBadgeStart = {
            InLibraryBadgeStable(enabled = manga.favorite)
        },
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
