package eu.kanade.domain.manga.model

import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.metadata.comicinfo.ComicInfo
import tachiyomi.core.metadata.comicinfo.ComicInfoPublishingStatus
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

// Constants for better maintainability
private const val CHAPTER_SHOW_DOWNLOADED = 1
private const val CHAPTER_SHOW_NOT_DOWNLOADED = 2

// Cache for expensive operations
private val coverCache: CoverCache by lazy { Injekt.get() }
private val basePreferences: BasePreferences by lazy { Injekt.get() }

// Extension properties for Manga
val Manga.readingMode: Long get() = viewerFlags and ReadingMode.MASK.toLong()
val Manga.readerOrientation: Long get() = viewerFlags and ReaderOrientation.MASK.toLong()

val Manga.downloadedFilter: TriState
    get() = when {
        basePreferences.downloadedOnly().get() -> TriState.ENABLED_IS
        downloadedFilterRaw == CHAPTER_SHOW_DOWNLOADED -> TriState.ENABLED_IS
        downloadedFilterRaw == CHAPTER_SHOW_NOT_DOWNLOADED -> TriState.ENABLED_NOT
        else -> TriState.DISABLED
    }

val Manga.hasActiveFilters: Boolean
    get() = unreadFilter != TriState.DISABLED ||
        downloadedFilter != TriState.DISABLED ||
        bookmarkedFilter != TriState.DISABLED

val Manga.hasCustomCover: Boolean
    get() = coverCache.getCustomCoverFile(id).exists()

// Conversion functions
fun Manga.toSManga(): SManga = SManga.create().apply {
    url = this@toSManga.url
    title = this@toSManga.title
    artist = this@toSManga.artist
    author = this@toSManga.author
    description = this@toSManga.description
    genre = this@toSManga.genre?.joinToString().orEmpty()
    status = this@toSManga.status.toInt()
    thumbnail_url = this@toSManga.thumbnailUrl
    initialized = this@toSManga.initialized
}

fun Manga.updateFrom(other: SManga): Manga = copy(
    author = other.author ?: author,
    artist = other.artist ?: artist,
    description = other.description ?: description,
    genre = other.genre?.let { other.getGenres() } ?: genre,
    thumbnailUrl = other.thumbnail_url ?: thumbnailUrl,
    status = other.status.toLong(),
    updateStrategy = other.update_strategy,
    initialized = other.initialized && initialized,
)

// ComicInfo creation with optimized logic
fun createComicInfo(
    manga: Manga,
    chapter: Chapter,
    urls: List<String>,
    categories: List<String>? = null,
    sourceName: String,
): ComicInfo = ComicInfo(
    title = ComicInfo.Title(chapter.name),
    series = ComicInfo.Series(manga.title),
    number = chapter.chapterNumber.takeIf { it >= 0 }?.let(::formatChapterNumber),
    web = ComicInfo.Web(urls.joinToString(" ")),
    summary = manga.description?.let(ComicInfo::Summary),
    writer = manga.author?.let(ComicInfo::Writer),
    penciller = manga.artist?.let(ComicInfo::Penciller),
    translator = chapter.scanlator?.let(ComicInfo::Translator),
    genre = manga.genre?.joinToString()?.let(ComicInfo::Genre),
    publishingStatus = ComicInfo.PublishingStatusTachiyomi(
        ComicInfoPublishingStatus.toComicInfoValue(manga.status)
    ),
    categories = categories?.joinToString()?.let(ComicInfo::CategoriesTachiyomi),
    source = ComicInfo.SourceMihon(sourceName),
    inker = null,
    colorist = null,
    letterer = null,
    coverArtist = null,
    tags = null,
)

// Helper function for chapter number formatting
private fun formatChapterNumber(number: Double): ComicInfo.Number = 
    ComicInfo.Number(if (number.rem(1) == 0.0) number.toInt().toString() else number.toString())

// Deprecated function for backward compatibility
@Deprecated("Use hasActiveFilters property instead", ReplaceWith("hasActiveFilters"))
fun Manga.chaptersFiltered(): Boolean = hasActiveFilters
