package eu.kanade.domain.chapter.interactor

import eu.kanade.domain.chapter.model.copyFromSChapter
import eu.kanade.domain.chapter.model.toSChapter
import eu.kanade.domain.manga.interactor.GetExcludedScanlators
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.flow.first
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.chapter.ChapterSanitizer
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.ShouldUpdateDbChapter
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.chapter.model.toChapterUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.chapter.service.ChapterRecognition
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.isLocal
import java.time.ZonedDateTime
import java.util.TreeSet
import kotlin.math.max
import kotlin.time.measureTime

/**
 * Interactor for synchronizing local chapter data with source chapters.
 * Handles chapter addition, removal, updates, and metadata synchronization.
 */
class SyncChaptersWithSource(
    private val downloadManager: DownloadManager,
    private val downloadProvider: DownloadProvider,
    private val chapterRepository: ChapterRepository,
    private val shouldUpdateDbChapter: ShouldUpdateDbChapter,
    private val updateManga: UpdateManga,
    private val updateChapter: UpdateChapter,
    private val getChaptersByMangaId: GetChaptersByMangaId,
    private val getExcludedScanlators: GetExcludedScanlators,
    private val libraryPreferences: LibraryPreferences,
) {

    companion object {
        private const val TAG = "SyncChaptersWithSource"
        
        // Library preference constants for readability
        private const val MARK_DUPLICATE_READ_NEW = "new"
    }

    /**
     * Result of chapter synchronization operation.
     */
    sealed interface SyncResult {
        data class Success(
            val newChapters: List<Chapter>,
            val updatedChapters: List<Chapter>,
            val removedChapters: List<Chapter>,
            val totalProcessed: Int,
        ) : SyncResult

        data object NoChanges : SyncResult
        data class Error(val exception: Throwable) : SyncResult
    }

    /**
     * Data class holding synchronization statistics.
     */
    data class SyncStats(
        val sourceChaptersCount: Int,
        val dbChaptersCount: Int,
        val newChaptersCount: Int,
        val updatedChaptersCount: Int,
        val removedChaptersCount: Int,
        val processingTimeMs: Long,
    )

    /**
     * Synchronizes database chapters with source chapters.
     *
     * @param rawSourceChapters The chapters retrieved from the source
     * @param manga The manga the chapters belong to
     * @param source The source the manga belongs to
     * @param manualFetch Whether this is a manual fetch operation
     * @param fetchWindow The fetch window for update scheduling
     * @return [SyncResult] containing the operation outcome and statistics
     */
    suspend fun sync(
        rawSourceChapters: List<SChapter>,
        manga: Manga,
        source: Source,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
    ): SyncResult {
        return try {
            val stats = measureTime {
                performSync(rawSourceChapters, manga, source, manualFetch, fetchWindow)
            }
            
            logcat(LogPriority.INFO, TAG) {
                "Sync completed for manga '${manga.title}' in ${stats.inWholeMilliseconds}ms"
            }
            
            stats
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to sync chapters for manga '${manga.title}'" }
            SyncResult.Error(e)
        }
    }

    /**
     * Performs the actual chapter synchronization.
     */
    private suspend fun performSync(
        rawSourceChapters: List<SChapter>,
        manga: Manga,
        source: Source,
        manualFetch: Boolean,
        fetchWindow: Pair<Long, Long>,
    ): SyncResult {
        // Validate input
        validateInput(rawSourceChapters, source)

        val now = ZonedDateTime.now()
        val nowMillis = now.toInstant().toEpochMilli()

        // Process source chapters
        val processedSourceChapters = processSourceChapters(rawSourceChapters, manga, source, nowMillis)
        val dbChapters = getChaptersByMangaId.await(manga.id)

        // Calculate changes
        val changes = calculateChapterChanges(processedSourceChapters, dbChapters, manga, source)
        
        if (changes.isEmpty) {
            handleNoChanges(manga, now, manualFetch, fetchWindow)
            return SyncResult.NoChanges
        }

        // Apply changes to database
        val syncResult = applyChangesToDatabase(changes, dbChapters, manga, now, fetchWindow)
        
        // Update manga metadata
        updateMangaMetadata(manga, now, fetchWindow)
        
        return syncResult
    }

    // region Processing Methods

    /**
     * Validates input parameters.
     */
    private fun validateInput(rawSourceChapters: List<SChapter>, source: Source) {
        if (rawSourceChapters.isEmpty() && !source.isLocal()) {
            throw NoChaptersException()
        }
    }

    /**
     * Processes raw source chapters into domain chapters.
     */
    private suspend fun processSourceChapters(
        rawSourceChapters: List<SChapter>,
        manga: Manga,
        source: Source,
        nowMillis: Long,
    ): List<Chapter> {
        return rawSourceChapters
            .distinctBy { it.url }
            .mapIndexed { index, sChapter ->
                createChapterFromSource(sChapter, manga, source, index, nowMillis)
            }
    }

    /**
     * Creates a domain chapter from a source chapter.
     */
    private suspend fun createChapterFromSource(
        sChapter: SChapter,
        manga: Manga,
        source: Source,
        index: Int,
        nowMillis: Long,
    ): Chapter {
        var chapter = Chapter.create()
            .copyFromSChapter(sChapter)
            .copy(
                name = with(ChapterSanitizer) { sChapter.name.sanitize(manga.title) },
                mangaId = manga.id,
                sourceOrder = index.toLong()
            )

        // Enhance chapter metadata for HTTP sources
        if (source is HttpSource) {
            chapter = enhanceChapterMetadata(chapter, manga, source)
        }

        // Recognize chapter number
        chapter = recognizeChapterNumber(chapter, manga)

        return chapter
    }

    /**
     * Enhances chapter metadata for HTTP sources.
     */
    private suspend fun enhanceChapterMetadata(chapter: Chapter, manga: Manga, source: HttpSource): Chapter {
        val sChapter = chapter.toSChapter()
        source.prepareNewChapter(sChapter, manga.toSManga())
        return chapter.copyFromSChapter(sChapter)
    }

    /**
     * Recognizes and updates chapter number.
     */
    private fun recognizeChapterNumber(chapter: Chapter, manga: Manga): Chapter {
        val chapterNumber = ChapterRecognition.parseChapterNumber(
            manga.title,
            chapter.name,
            chapter.chapterNumber
        )
        return chapter.copy(chapterNumber = chapterNumber)
    }

    // endregion

    // region Change Calculation

    /**
     * Data class holding all chapter changes.
     */
    private data class ChapterChanges(
        val newChapters: List<Chapter>,
        val updatedChapters: List<Chapter>,
        val removedChapters: List<Chapter>,
        val isEmpty: Boolean
    )

    /**
     * Calculates changes between source and database chapters.
     */
    private suspend fun calculateChapterChanges(
        sourceChapters: List<Chapter>,
        dbChapters: List<Chapter>,
        manga: Manga,
        source: Source,
    ): ChapterChanges {
        val removedChapters = findRemovedChapters(sourceChapters, dbChapters)
        val (newChapters, updatedChapters) = findNewAndUpdatedChapters(sourceChapters, dbChapters, manga, source)

        val isEmpty = newChapters.isEmpty() && updatedChapters.isEmpty() && removedChapters.isEmpty()
        
        return ChapterChanges(newChapters, updatedChapters, removedChapters, isEmpty)
    }

    /**
     * Finds chapters that exist in DB but not in source (removed chapters).
     */
    private fun findRemovedChapters(sourceChapters: List<Chapter>, dbChapters: List<Chapter>): List<Chapter> {
        val sourceUrls = sourceChapters.map { it.url }.toSet()
        return dbChapters.filterNot { it.url in sourceUrls }
    }

    /**
     * Finds new and updated chapters.
     */
    private suspend fun findNewAndUpdatedChapters(
        sourceChapters: List<Chapter>,
        dbChapters: List<Chapter>,
        manga: Manga,
        source: Source,
    ): Pair<List<Chapter>, List<Chapter>> {
        val newChapters = mutableListOf<Chapter>()
        val updatedChapters = mutableListOf<Chapter>()
        var maxSeenUploadDate = 0L

        for (sourceChapter in sourceChapters) {
            val dbChapter = dbChapters.find { it.url == sourceChapter.url }

            if (dbChapter == null) {
                val chapterWithUploadDate = ensureUploadDate(sourceChapter, maxSeenUploadDate)
                maxSeenUploadDate = max(maxSeenUploadDate, chapterWithUploadDate.dateUpload)
                newChapters.add(chapterWithUploadDate)
            } else {
                val updatedChapter = handleExistingChapter(dbChapter, sourceChapter, manga, source)
                updatedChapter?.let { updatedChapters.add(it) }
            }
        }

        return newChapters to updatedChapters
    }

    /**
     * Ensures chapter has a valid upload date.
     */
    private fun ensureUploadDate(chapter: Chapter, maxSeenUploadDate: Long): Chapter {
        return if (chapter.dateUpload == 0L) {
            val altDateUpload = if (maxSeenUploadDate == 0L) System.currentTimeMillis() else maxSeenUploadDate
            chapter.copy(dateUpload = altDateUpload)
        } else {
            chapter
        }
    }

    /**
     * Handles existing chapter updates.
     */
    private suspend fun handleExistingChapter(
        dbChapter: Chapter,
        sourceChapter: Chapter,
        manga: Manga,
        source: Source,
    ): Chapter? {
        if (!shouldUpdateDbChapter.await(dbChapter, sourceChapter)) {
            return null
        }

        // Handle download renaming if needed
        handleDownloadRenaming(dbChapter, sourceChapter, manga, source)

        return createUpdatedChapter(dbChapter, sourceChapter)
    }

    /**
     * Handles download renaming if chapter directory name changed.
     */
    private suspend fun handleDownloadRenaming(
        dbChapter: Chapter,
        sourceChapter: Chapter,
        manga: Manga,
        source: Source,
    ) {
        val shouldRename = downloadProvider.isChapterDirNameChanged(dbChapter, sourceChapter) &&
            downloadManager.isChapterDownloaded(
                dbChapter.name,
                dbChapter.scanlator,
                dbChapter.url,
                manga.title,
                manga.source,
            )

        if (shouldRename) {
            downloadManager.renameChapter(source, manga, dbChapter, sourceChapter)
        }
    }

    /**
     * Creates an updated chapter from existing and source data.
     */
    private fun createUpdatedChapter(dbChapter: Chapter, sourceChapter: Chapter): Chapter {
        return dbChapter.copy(
            name = sourceChapter.name,
            chapterNumber = sourceChapter.chapterNumber,
            scanlator = sourceChapter.scanlator,
            sourceOrder = sourceChapter.sourceOrder,
            dateUpload = if (sourceChapter.dateUpload != 0L) sourceChapter.dateUpload else dbChapter.dateUpload,
        )
    }

    // endregion

    // region Database Operations

    /**
     * Applies calculated changes to the database.
     */
    private suspend fun applyChangesToDatabase(
        changes: ChapterChanges,
        dbChapters: List<Chapter>,
        manga: Manga,
        now: ZonedDateTime,
        fetchWindow: Pair<Long, Long>,
    ): SyncResult.Success {
        val (newChapters, updatedChapters, removedChapters) = changes

        // Process new chapters with read status handling
        val processedNewChapters = processNewChapters(newChapters, dbChapters, removedChapters, manga.id)

        // Execute database operations
        executeDatabaseOperations(removedChapters, processedNewChapters, updatedChapters)

        // Filter out excluded scanlators
        val filteredNewChapters = filterExcludedScanlators(processedNewChapters, manga.id)

        return SyncResult.Success(
            newChapters = filteredNewChapters,
            updatedChapters = updatedChapters,
            removedChapters = removedChapters,
            totalProcessed = newChapters.size + updatedChapters.size + removedChapters.size
        )
    }

    /**
     * Processes new chapters with read status and date handling.
     */
    private suspend fun processNewChapters(
        newChapters: List<Chapter>,
        dbChapters: List<Chapter>,
        removedChapters: List<Chapter>,
        mangaId: Long,
    ): List<Chapter> {
        if (newChapters.isEmpty()) return emptyList()

        val readChapterNumbers = dbChapters
            .filter { it.read && it.isRecognizedNumber }
            .map { it.chapterNumber }
            .toSet()

        val markDuplicateAsRead = libraryPreferences.markDuplicateReadChapterAsRead().first()
            .contains(MARK_DUPLICATE_READ_NEW)

        val deletedChapterNumbers = removedChapters.map { it.chapterNumber }.toSet()
        val deletedReadChapterNumbers = removedChapters.filter { it.read }.map { it.chapterNumber }.toSet()
        val deletedBookmarkedChapterNumbers = removedChapters.filter { it.bookmark }.map { it.chapterNumber }.toSet()
        val deletedChapterNumberDateFetchMap = removedChapters
            .sortedByDescending { it.dateFetch }
            .associate { it.chapterNumber to it.dateFetch }

        var itemCount = newChapters.size
        val nowMillis = System.currentTimeMillis()

        return newChapters.map { chapter ->
            var processedChapter = chapter.copy(dateFetch = nowMillis + itemCount--)
            val isDuplicate = chapter.isRecognizedNumber && chapter.chapterNumber in deletedChapterNumbers

            if (isDuplicate) {
                processedChapter = handleDuplicateChapter(
                    processedChapter,
                    deletedReadChapterNumbers,
                    deletedBookmarkedChapterNumbers,
                    deletedChapterNumberDateFetchMap
                )
            } else if (markDuplicateAsRead && chapter.chapterNumber in readChapterNumbers) {
                processedChapter = processedChapter.copy(read = true)
            }

            processedChapter
        }
    }

    /**
     * Handles duplicate chapter processing.
     */
    private fun handleDuplicateChapter(
        chapter: Chapter,
        deletedReadChapterNumbers: Set<Double>,
        deletedBookmarkedChapterNumbers: Set<Double>,
        deletedChapterNumberDateFetchMap: Map<Double, Long>,
    ): Chapter {
        var processedChapter = chapter.copy(
            read = chapter.chapterNumber in deletedReadChapterNumbers,
            bookmark = chapter.chapterNumber in deletedBookmarkedChapterNumbers,
        )

        // Preserve original fetch date if available
        deletedChapterNumberDateFetchMap[chapter.chapterNumber]?.let { originalDateFetch ->
            processedChapter = processedChapter.copy(dateFetch = originalDateFetch)
        }

        return processedChapter
    }

    /**
     * Executes database operations in sequence.
     */
    private suspend fun executeDatabaseOperations(
        removedChapters: List<Chapter>,
        newChapters: List<Chapter>,
        updatedChapters: List<Chapter>,
    ) {
        // Remove chapters first
        if (removedChapters.isNotEmpty()) {
            val toDeleteIds = removedChapters.map { it.id }
            chapterRepository.removeChaptersWithIds(toDeleteIds)
        }

        // Add new chapters
        val addedChapters = if (newChapters.isNotEmpty()) {
            chapterRepository.addAll(newChapters)
        } else {
            emptyList()
        }

        // Update existing chapters
        if (updatedChapters.isNotEmpty()) {
            val chapterUpdates = updatedChapters.map { it.toChapterUpdate() }
            updateChapter.awaitAll(chapterUpdates)
        }
    }

    /**
     * Filters out chapters from excluded scanlators.
     */
    private suspend fun filterExcludedScanlators(chapters: List<Chapter>, mangaId: Long): List<Chapter> {
        val excludedScanlators = getExcludedScanlators.await(mangaId).toHashSet()
        return chapters.filterNot { it.scanlator in excludedScanlators }
    }

    // endregion

    // region Manga Update Methods

    /**
     * Handles case when no changes are detected.
     */
    private suspend fun handleNoChanges(
        manga: Manga,
        now: ZonedDateTime,
        manualFetch: Boolean,
        fetchWindow: Pair<Long, Long>,
    ) {
        if (manualFetch || manga.fetchInterval == 0 || manga.nextUpdate < fetchWindow.first) {
            updateManga.awaitUpdateFetchInterval(manga, now, fetchWindow)
        }
    }

    /**
     * Updates manga metadata after sync.
     */
    private suspend fun updateMangaMetadata(
        manga: Manga,
        now: ZonedDateTime,
        fetchWindow: Pair<Long, Long>,
    ) {
        updateManga.awaitUpdateFetchInterval(manga, now, fetchWindow)
        updateManga.awaitUpdateLastUpdate(manga.id)
    }

    // endregion

    /**
     * @deprecated Use sync() instead for better error handling and results
     */
    @Deprecated("Use sync() method instead", ReplaceWith("sync(rawSourceChapters, manga, source, manualFetch, fetchWindow)"))
    suspend fun await(
        rawSourceChapters: List<SChapter>,
        manga: Manga,
        source: Source,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
    ): List<Chapter> {
        val result = sync(rawSourceChapters, manga, source, manualFetch, fetchWindow)
        return when (result) {
            is SyncResult.Success -> result.newChapters
            SyncResult.NoChanges -> emptyList()
            is SyncResult.Error -> throw result.exception
        }
    }
}
