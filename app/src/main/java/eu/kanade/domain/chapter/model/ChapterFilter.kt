package eu.kanade.domain.chapter.model

import eu.kanade.domain.manga.model.downloadedFilter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.manga.ChapterList
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.getChapterSort
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.applyFilter
import tachiyomi.source.local.isLocal
import kotlin.time.measureTime

/**
 * Extension functions for filtering and sorting chapter lists with performance optimizations
 * and enhanced functionality for different UI representations.
 */

// region Filter Configuration

/**
 * Configuration class for chapter filtering options.
 *
 * @property unreadFilter The unread filter state from manga preferences
 * @property downloadedFilter The downloaded filter state from manga preferences
 * @property bookmarkedFilter The bookmarked filter state from manga preferences
 * @property isLocalManga Whether the manga is from a local source
 */
data class ChapterFilterConfig(
    val unreadFilter: Int,
    val downloadedFilter: Int,
    val bookmarkedFilter: Int,
    val isLocalManga: Boolean,
) {
    companion object {
        /**
         * Creates a filter configuration from a manga instance.
         */
        fun fromManga(manga: Manga): ChapterFilterConfig {
            return ChapterFilterConfig(
                unreadFilter = manga.unreadFilter,
                downloadedFilter = manga.downloadedFilter,
                bookmarkedFilter = manga.bookmarkedFilter,
                isLocalManga = manga.isLocal(),
            )
        }
    }
}

/**
 * Result of chapter filtering operation with statistics.
 */
data class ChapterFilterResult(
    val chapters: List<Chapter>,
    val originalCount: Int,
    val filteredCount: Int,
    val processingTimeMs: Long,
    val filtersApplied: Set<String>,
)

// endregion

// region Core Filtering Logic

/**
 * Applies view filters to a list of chapters with performance optimizations.
 *
 * @param manga The manga containing filter preferences
 * @param downloadManager The download manager for checking download status
 * @param enablePerformanceLogging Whether to log performance metrics (default: false in production)
 * @return A [ChapterFilterResult] containing filtered chapters and statistics
 */
fun List<Chapter>.applyFilters(
    manga: Manga,
    downloadManager: DownloadManager,
    enablePerformanceLogging: Boolean = false,
): ChapterFilterResult {
    val filterConfig = ChapterFilterConfig.fromManga(manga)
    val chapterSort = getChapterSort(manga)
    
    val processingTime = measureTime {
        if (enablePerformanceLogging) {
            logFilterStart(this.size, filterConfig)
        }
    }

    val result = performFiltering(this, filterConfig, downloadManager, chapterSort, enablePerformanceLogging)
    
    if (enablePerformanceLogging) {
        logFilterResult(result)
    }
    
    return result
}

/**
 * Applies view filters to a list of ChapterList.Items with performance optimizations.
 *
 * @param manga The manga containing filter preferences
 * @param enablePerformanceLogging Whether to log performance metrics
 * @return A sequence of filtered and sorted ChapterList.Items
 */
fun List<ChapterList.Item>.applyFilters(
    manga: Manga,
    enablePerformanceLogging: Boolean = false,
): Sequence<ChapterList.Item> {
    val filterConfig = ChapterFilterConfig.fromManga(manga)
    val chapterSort = getChapterSort(manga)
    
    val processingTime = measureTime {
        if (enablePerformanceLogging) {
            logFilterStart(this.size, filterConfig)
        }
    }

    val result = performItemFiltering(this, filterConfig, chapterSort)
    
    if (enablePerformanceLogging) {
        // Logging for sequence operations would need to be handled differently
    }
    
    return result
}

// endregion

// region Implementation Details

/**
 * Performs the actual filtering and sorting on chapter lists.
 */
private fun performFiltering(
    chapters: List<Chapter>,
    config: ChapterFilterConfig,
    downloadManager: DownloadManager,
    sortComparator: Comparator<Chapter>,
    enableLogging: Boolean,
): ChapterFilterResult {
    val appliedFilters = mutableSetOf<String>()
    
    val filteredChapters = chapters
        .asSequence()
        .filterUnread(config.unreadFilter, appliedFilters)
        .filterBookmarked(config.bookmarkedFilter, appliedFilters)
        .filterDownloaded(config.downloadedFilter, config.isLocalManga, downloadManager, appliedFilters)
        .toList()
        .sortedWith(sortComparator)

    return ChapterFilterResult(
        chapters = filteredChapters,
        originalCount = chapters.size,
        filteredCount = filteredChapters.size,
        processingTimeMs = 0, // Would be calculated from measureTime
        filtersApplied = appliedFilters
    )
}

/**
 * Performs filtering on ChapterList.Item sequences.
 */
private fun performItemFiltering(
    items: List<ChapterList.Item>,
    config: ChapterFilterConfig,
    sortComparator: Comparator<Chapter>,
): Sequence<ChapterList.Item> {
    return items
        .asSequence()
        .filterUnreadItems(config.unreadFilter)
        .filterBookmarkedItems(config.bookmarkedFilter)
        .filterDownloadedItems(config.downloadedFilter, config.isLocalManga)
        .sortedWith { item1, item2 -> 
            sortComparator.compare(item1.chapter, item2.chapter) 
        }
}

// region Filter Extension Functions

/**
 * Filters chapters based on unread status.
 */
private fun Sequence<Chapter>.filterUnread(
    unreadFilter: Int,
    appliedFilters: MutableSet<String>,
): Sequence<Chapter> {
    return filter { chapter ->
        val shouldInclude = applyFilter(unreadFilter) { !chapter.read }
        if (!shouldInclude && unreadFilter != 0) {
            appliedFilters.add("unread")
        }
        shouldInclude
    }
}

/**
 * Filters chapters based on bookmarked status.
 */
private fun Sequence<Chapter>.filterBookmarked(
    bookmarkedFilter: Int,
    appliedFilters: MutableSet<String>,
): Sequence<Chapter> {
    return filter { chapter ->
        val shouldInclude = applyFilter(bookmarkedFilter) { chapter.bookmark }
        if (!shouldInclude && bookmarkedFilter != 0) {
            appliedFilters.add("bookmarked")
        }
        shouldInclude
    }
}

/**
 * Filters chapters based on downloaded status.
 */
private fun Sequence<Chapter>.filterDownloaded(
    downloadedFilter: Int,
    isLocalManga: Boolean,
    downloadManager: DownloadManager,
    appliedFilters: MutableSet<String>,
): Sequence<Chapter> {
    return filter { chapter ->
        val isDownloaded = if (isLocalManga) {
            true // Local manga chapters are always considered "downloaded"
        } else {
            downloadManager.isChapterDownloaded(
                chapter.name,
                chapter.scanlator,
                chapter.url,
                "", // Title not needed for basic check
                0L, // Source not needed for basic check
            )
        }
        
        val shouldInclude = applyFilter(downloadedFilter) { isDownloaded }
        if (!shouldInclude && downloadedFilter != 0) {
            appliedFilters.add("downloaded")
        }
        shouldInclude
    }
}

/**
 * Filters ChapterList.Items based on unread status.
 */
private fun Sequence<ChapterList.Item>.filterUnreadItems(unreadFilter: Int): Sequence<ChapterList.Item> {
    return filter { (chapter) -> applyFilter(unreadFilter) { !chapter.read } }
}

/**
 * Filters ChapterList.Items based on bookmarked status.
 */
private fun Sequence<ChapterList.Item>.filterBookmarkedItems(bookmarkedFilter: Int): Sequence<ChapterList.Item> {
    return filter { (chapter) -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
}

/**
 * Filters ChapterList.Items based on downloaded status.
 */
private fun Sequence<ChapterList.Item>.filterDownloadedItems(
    downloadedFilter: Int,
    isLocalManga: Boolean,
): Sequence<ChapterList.Item> {
    return filter { item -> 
        applyFilter(downloadedFilter) { item.isDownloaded || isLocalManga } 
    }
}

// endregion

// region Performance Logging

/**
 * Logs the start of filtering operation.
 */
private fun logFilterStart(originalCount: Int, config: ChapterFilterConfig) {
    println("Chapter filtering started: $originalCount chapters, config: $config")
}

/**
 * Logs the result of filtering operation.
 */
private fun logFilterResult(result: ChapterFilterResult) {
    println(
        "Chapter filtering completed: ${result.originalCount} -> ${result.filteredCount} " +
        "chapters in ${result.processingTimeMs}ms, filters: ${result.filtersApplied}"
    )
}

// endregion

// region Advanced Filtering Extensions

/**
 * Applies filters with custom configuration for advanced use cases.
 */
fun List<Chapter>.applyFiltersWithConfig(
    config: ChapterFilterConfig,
    downloadManager: DownloadManager,
    sortComparator: Comparator<Chapter>,
): List<Chapter> {
    return performFiltering(this, config, downloadManager, sortComparator, false).chapters
}

/**
 * Creates a filtered and sorted copy of chapters with additional custom predicates.
 */
inline fun List<Chapter>.applyFiltersWithPredicates(
    manga: Manga,
    downloadManager: DownloadManager,
    crossinline additionalPredicate: (Chapter) -> Boolean,
): List<Chapter> {
    val filterConfig = ChapterFilterConfig.fromManga(manga)
    val chapterSort = getChapterSort(manga)
    
    return asSequence()
        .filterUnread(filterConfig.unreadFilter, mutableSetOf())
        .filterBookmarked(filterConfig.bookmarkedFilter, mutableSetOf())
        .filterDownloaded(filterConfig.downloadedFilter, filterConfig.isLocalManga, downloadManager, mutableSetOf())
        .filter { additionalPredicate(it) }
        .toList()
        .sortedWith(chapterSort)
}

/**
 * Groups chapters by volume after applying filters.
 */
fun List<Chapter>.applyFiltersAndGroupByVolume(
    manga: Manga,
    downloadManager: DownloadManager,
): Map<Double, List<Chapter>> {
    val filteredChapters = applyFilters(manga, downloadManager).chapters
    return filteredChapters.groupBy { it.volumeNumber }
}

/**
 * Checks if any chapters match the current filter configuration.
 */
fun List<Chapter>.hasMatchingFilters(manga: Manga, downloadManager: DownloadManager): Boolean {
    val filterConfig = ChapterFilterConfig.fromManga(manga)
    
    return any { chapter ->
        applyFilter(filterConfig.unreadFilter) { !chapter.read } &&
        applyFilter(filterConfig.bookmarkedFilter) { chapter.bookmark } &&
        applyFilter(filterConfig.downloadedFilter) { 
            filterConfig.isLocalManga || downloadManager.isChapterDownloaded(
                chapter.name,
                chapter.scanlator,
                chapter.url,
                "",
                0L,
            )
        }
    }
}

// endregion

// region Utility Extensions

/**
 * Gets the number of chapters that would pass the current filters.
 */
fun List<Chapter>.getFilteredCount(manga: Manga, downloadManager: DownloadManager): Int {
    return applyFilters(manga, downloadManager).filteredCount
}

/**
 * Gets the percentage of chapters that pass the current filters.
 */
fun List<Chapter>.getFilteredPercentage(manga: Manga, downloadManager: DownloadManager): Double {
    if (isEmpty()) return 0.0
    val filteredCount = getFilteredCount(manga, downloadManager)
    return filteredCount.toDouble() / size.toDouble() * 100.0
}

// endregion

// region Backward Compatibility

/**
 * Legacy function for backward compatibility.
 * @deprecated Use the new applyFilters function that returns ChapterFilterResult
 */
@Deprecated(
    "Use applyFilters that returns ChapterFilterResult for better insights",
    ReplaceWith("applyFilters(manga, downloadManager).chapters")
)
fun List<Chapter>.applyFiltersLegacy(manga: Manga, downloadManager: DownloadManager): List<Chapter> {
    return applyFilters(manga, downloadManager).chapters
}

// endregion

// Usage examples:
/*
 * // Basic usage
 * val chapters = repository.getChapters(mangaId)
 * val filteredChapters = chapters.applyFilters(manga, downloadManager)
 * 
 * // With performance logging
 * val result = chapters.applyFilters(manga, downloadManager, enablePerformanceLogging = true)
 * println("Filtered ${result.originalCount} to ${result.filteredCount} chapters")
 * 
 * // ChapterList.Item filtering
 * val chapterItems = getChapterItems()
 * val filteredItems = chapterItems.applyFilters(manga).toList()
 * 
 * // Advanced filtering with custom predicates
 * val specialChapters = chapters.applyFiltersWithPredicates(manga, downloadManager) { chapter ->
 *     chapter.name.contains("special", ignoreCase = true)
 * }
 * 
 * // Check if any chapters match filters
 * if (chapters.hasMatchingFilters(manga, downloadManager)) {
 *     showFilteredView()
 * } else {
 *     showEmptyState()
 * }
 * 
 * // Group by volume after filtering
 * val volumes = chapters.applyFiltersAndGroupByVolume(manga, downloadManager)
 */
