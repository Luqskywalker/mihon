// Basic usage
@Composable
fun MangaItem(manga: Manga) {
    Text(
        text = relativeDateText(manga.lastUpdate),
        style = MaterialTheme.typography.bodyMedium
    )
}

// For lists - more efficient
@Composable
fun MangaList(mangas: List<Manga>) {
    val dateFormatter = rememberDateFormatter()
    
    LazyColumn {
        items(mangas) { manga ->
            Text(
                text = dateFormatter(manga.lastUpdate),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Using time ago for recent items
@Composable
fun RecentUpdateItem(manga: Manga) {
    Text(
        text = relativeTimeAgoText(manga.lastUpdate),
        style = MaterialTheme.typography.bodySmall
    )
}

// Custom formatting
@Composable
fun DetailedDateItem(manga: Manga) {
    Text(
        text = formattedDateText(manga.lastUpdate, "EEE, MMM d, yyyy"),
        style = MaterialTheme.typography.bodySmall
    )
}
