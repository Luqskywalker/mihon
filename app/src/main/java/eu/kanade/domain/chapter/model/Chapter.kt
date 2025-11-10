package eu.kanade.domain.chapter.model

import eu.kanade.tachiyomi.data.database.models.Chapter as DbChapter
import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.source.model.SChapter
import tachiyomi.domain.chapter.model.Chapter

/**
 * Extension functions for converting between different chapter representations.
 * Provides type-safe conversions with proper null handling and validation.
 */

// region Chapter to SChapter Conversion

/**
 * Converts a domain [Chapter] to a source [SChapter].
 * 
 * @return A new [SChapter] instance with data copied from this chapter
 * 
 * @throws IllegalArgumentException if required fields are invalid
 */
fun Chapter.toSChapter(): SChapter {
    validateChapterForSChapterConversion()
    
    return SChapter.create().apply {
        url = this@toSChapter.url
        name = this@toSChapter.name
        date_upload = this@toSChapter.dateUpload
        chapter_number = this@toSChapter.chapterNumber.toFloat()
        scanlator = this@toSChapter.scanlator
    }
}

/**
 * Validates that the chapter has valid data for SChapter conversion.
 */
private fun Chapter.validateChapterForSChapterConversion() {
    require(url.isNotBlank()) { "Chapter URL cannot be blank for SChapter conversion" }
    require(name.isNotBlank()) { "Chapter name cannot be blank for SChapter conversion" }
    require(chapterNumber >= 0) { "Chapter number cannot be negative" }
}

// endregion

// region SChapter to Chapter Conversion

/**
 * Creates a new [Chapter] by copying data from an [SChapter].
 * 
 * @param sChapter The source chapter to copy data from
 * @return A new [Chapter] instance with data from the SChapter
 */
fun Chapter.copyFromSChapter(sChapter: SChapter): Chapter {
    validateSChapterForConversion(sChapter)
    
    return this.copy(
        name = sChapter.name.trim(),
        url = sChapter.url,
        dateUpload = sChapter.date_upload.coerceAtLeast(0),
        chapterNumber = sChapter.chapter_number.toDouble().coerceAtLeast(0.0),
        scanlator = sChapter.scanlator?.cleanupScanlator(),
    )
}

/**
 * Creates a new [Chapter] from an [SChapter] with manga context.
 * 
 * @param sChapter The source chapter to convert
 * @param mangaId The manga ID to associate with the chapter
 * @param sourceOrder The order of the chapter in the source
 * @return A new [Chapter] instance
 */
fun Chapter.createFromSChapter(
    sChapter: SChapter,
    mangaId: Long,
    sourceOrder: Long = 0L,
): Chapter {
    validateSChapterForConversion(sChapter)
    require(mangaId > 0) { "Manga ID must be positive" }
    require(sourceOrder >= 0) { "Source order cannot be negative" }
    
    return Chapter.create().copy(
        name = sChapter.name.trim(),
        url = sChapter.url,
        dateUpload = sChapter.date_upload.coerceAtLeast(0),
        chapterNumber = sChapter.chapter_number.toDouble().coerceAtLeast(0.0),
        scanlator = sChapter.scanlator?.cleanupScanlator(),
        mangaId = mangaId,
        sourceOrder = sourceOrder,
        dateFetch = System.currentTimeMillis(),
    )
}

/**
 * Validates that the SChapter has valid data for conversion.
 */
private fun validateSChapterForConversion(sChapter: SChapter) {
    require(sChapter.url.isNotBlank()) { "SChapter URL cannot be blank" }
    require(sChapter.name.isNotBlank()) { "SChapter name cannot be blank" }
    require(sChapter.chapter_number >= 0) { "SChapter chapter number cannot be negative" }
}

/**
 * Cleans up scanlator string by removing whitespace and handling empty values.
 */
private fun String.cleanupScanlator(): String? {
    return trim().takeIf { it.isNotBlank() }
}

// endregion

// region Chapter to DbChapter Conversion

/**
 * Converts a domain [Chapter] to a database [DbChapter].
 * 
 * @return A new [DbChapter] instance with data copied from this chapter
 * 
 * @throws IllegalArgumentException if required fields are invalid
 */
fun Chapter.toDbChapter(): DbChapter {
    validateChapterForDbConversion()
    
    return ChapterImpl().apply {
        id = this@toDbChapter.id
        manga_id = this@toDbChapter.mangaId
        url = this@toDbChapter.url
        name = this@toDbChapter.name
        scanlator = this@toDbChapter.scanlator
        read = this@toDbChapter.read
        bookmark = this@toDbChapter.bookmark
        last_page_read = this@toDbChapter.lastPageRead.toInt().coerceAtLeast(0)
        date_fetch = this@toDbChapter.dateFetch
        date_upload = this@toDbChapter.dateUpload
        chapter_number = this@toDbChapter.chapterNumber.toFloat()
        source_order = this@toDbChapter.sourceOrder.toInt().coerceAtLeast(0)
    }
}

/**
 * Validates that the chapter has valid data for database conversion.
 */
private fun Chapter.validateChapterForDbConversion() {
    require(id != 0L) { "Chapter ID cannot be 0 for database conversion" }
    require(mangaId > 0) { "Manga ID must be positive for database conversion" }
    require(url.isNotBlank()) { "Chapter URL cannot be blank for database conversion" }
    require(name.isNotBlank()) { "Chapter name cannot be blank for database conversion" }
    require(chapterNumber >= 0) { "Chapter number cannot be negative" }
    require(sourceOrder >= 0) { "Source order cannot be negative" }
    require(lastPageRead >= 0) { "Last page read cannot be negative" }
}

// endregion

// region DbChapter to Chapter Conversion

/**
 * Converts a database [DbChapter] to a domain [Chapter].
 * 
 * @return A new [Chapter] instance with data from the DbChapter
 */
fun DbChapter.toDomainChapter(): Chapter {
    validateDbChapterForConversion()
    
    return Chapter.create().copy(
        id = id,
        mangaId = manga_id,
        url = url.orEmpty(),
        name = name.orEmpty(),
        scanlator = scanlator,
        read = read,
        bookmark = bookmark,
        lastPageRead = last_page_read.toLong(),
        dateFetch = date_fetch,
        dateUpload = date_upload,
        chapterNumber = chapter_number.toDouble(),
        sourceOrder = source_order.toLong(),
    )
}

/**
 * Validates that the DbChapter has valid data for conversion.
 */
private fun DbChapter.validateDbChapterForConversion() {
    require(id != 0L) { "DbChapter ID cannot be 0" }
    require(manga_id > 0) { "DbChapter manga_id must be positive" }
    require(!url.isNullOrBlank()) { "DbChapter URL cannot be blank" }
    require(!name.isNullOrBlank()) { "DbChapter name cannot be blank" }
}

// endregion

// region Bulk Conversion Extensions

/**
 * Converts a collection of domain chapters to SChapters.
 */
fun Iterable<Chapter>.toSChapters(): List<SChapter> = map { it.toSChapter() }

/**
 * Converts a collection of SChapters to domain chapters with manga context.
 */
fun Iterable<SChapter>.toDomainChapters(mangaId: Long): List<Chapter> = mapIndexed { index, sChapter ->
    Chapter.create().createFromSChapter(sChapter, mangaId, index.toLong())
}

/**
 * Converts a collection of domain chapters to DbChapters.
 */
fun Iterable<Chapter>.toDbChapters(): List<DbChapter> = map { it.toDbChapter() }

/**
 * Converts a collection of DbChapters to domain chapters.
 */
fun Iterable<DbChapter>.toDomainChapters(): List<Chapter> = map { it.toDomainChapter() }

// endregion

// region Chapter Validation Extensions

/**
 * Validates that the chapter has all required fields populated.
 */
val Chapter.isValid: Boolean
    get() = id != 0L &&
        mangaId > 0 &&
        url.isNotBlank() &&
        name.isNotBlank() &&
        chapterNumber >= 0 &&
        sourceOrder >= 0 &&
        lastPageRead >= 0

/**
 * Checks if the chapter has been read (either fully read or has reading progress).
 */
val Chapter.hasBeenRead: Boolean
    get() = read || lastPageRead > 0

/**
 * Checks if the chapter number is recognized (not a special chapter).
 */
val Chapter.isRecognizedNumber: Boolean
    get() = chapterNumber >= 0

/**
 * Gets a sanitized version of the chapter name for display.
 */
val Chapter.sanitizedName: String
    get() = name.trim()

// endregion

// region Chapter Comparison Extensions

/**
 * Checks if this chapter has the same content as another chapter (excluding IDs and metadata).
 */
fun Chapter.hasSameContent(other: Chapter): Boolean {
    return url == other.url &&
        name == other.name &&
        chapterNumber == other.chapterNumber &&
        scanlator == other.scanlator
}

/**
 * Checks if this chapter has different metadata than another chapter.
 */
fun Chapter.hasDifferentMetadata(other: Chapter): Boolean {
    return read != other.read ||
        bookmark != other.bookmark ||
        lastPageRead != other.lastPageRead ||
        dateFetch != other.dateFetch ||
        sourceOrder != other.sourceOrder
}

// endregion

// region Utility Extensions

/**
 * Creates a chapter update object with only the changed fields.
 */
fun Chapter.toChapterUpdate(): ChapterUpdate {
    return ChapterUpdate(
        id = id,
        name = name,
        scanlator = scanlator,
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        dateFetch = dateFetch,
        dateUpload = dateUpload,
        chapterNumber = chapterNumber,
        sourceOrder = sourceOrder,
    )
}

/**
 * Creates a copy of the chapter with reset read status.
 */
fun Chapter.withResetReadStatus(): Chapter {
    return copy(
        read = false,
        lastPageRead = 0L
    )
}

/**
 * Creates a copy of the chapter with updated read progress.
 */
fun Chapter.withReadProgress(lastPageRead: Long, isComplete: Boolean = false): Chapter {
    return copy(
        read = isComplete,
        lastPageRead = lastPageRead.coerceAtLeast(0)
    )
}

// endregion

// Usage examples:
/*
 * // Basic conversions
 * val sChapter = domainChapter.toSChapter()
 * val domainChapter = Chapter.create().copyFromSChapter(sChapter)
 * val dbChapter = domainChapter.toDbChapter()
 * 
 * // Bulk conversions
 * val sChapters = domainChapters.toSChapters()
 * val domainChapters = dbChapters.toDomainChapters()
 * 
 * // Validation
 * if (chapter.isValid) {
 *     processChapter(chapter)
 * }
 * 
 * // Chapter updates
 * val update = chapter.toChapterUpdate()
 * val resetChapter = chapter.withResetReadStatus()
 * 
 * // Content comparison
 * if (chapter1.hasSameContent(chapter2)) {
 *     handleDuplicateChapters()
 * }
 */
