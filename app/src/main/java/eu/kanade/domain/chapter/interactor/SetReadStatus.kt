package eu.kanade.domain.chapter.interactor

import eu.kanade.domain.download.interactor.DeleteDownload
import kotlinx.coroutines.flow.first
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository
import kotlin.time.measureTime

/**
 * Interactor for managing chapter read status with support for download cleanup.
 * Handles marking chapters as read/unread and automatically manages downloads based on preferences.
 *
 * @property downloadPreferences Preferences for download behavior
 * @property deleteDownload Interactor for deleting downloads
 * @property mangaRepository Repository for manga data access
 * @property chapterRepository Repository for chapter data access
 */
class SetReadStatus(
    private val downloadPreferences: DownloadPreferences,
    private val deleteDownload: DeleteDownload,
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
) {

    companion object {
        private const val TAG = "SetReadStatus"
    }

    /**
     * Chapter update mapper that resets last page read when marking as unread.
     */
    private val chapterUpdateMapper: (Chapter, Boolean) -> ChapterUpdate = { chapter, read ->
        ChapterUpdate(
            id = chapter.id,
            read = read,
            lastPageRead = if (read) chapter.lastPageRead else 0, // Reset when unreading
            dateFetch = chapter.dateFetch, // Preserve existing values
            dateUpload = chapter.dateUpload,
            name = chapter.name,
            scanlator = chapter.scanlator,
            url = chapter.url,
            chapterNumber = chapter.chapterNumber,
            volumeNumber = chapter.volumeNumber,
            mangaId = chapter.mangaId,
            sourceOrder = chapter.sourceOrder
        )
    }

    /**
     * Sets read status for specific chapters with download cleanup if enabled.
     *
     * @param read The target read status (true for read, false for unread)
     * @param chapters The chapters to update
     * @return [Result] indicating the operation outcome
     */
    suspend fun await(read: Boolean, vararg chapters: Chapter): Result = withNonCancellableContext {
        if (chapters.isEmpty()) {
            return@withNonCancellableContext Result.NoChapters
        }

        val operationTime = measureTime {
            val chaptersToUpdate = filterChaptersNeedingUpdate(chapters.toList(), read)
            if (chaptersToUpdate.isEmpty()) {
                return@withNonCancellableContext Result.NoChangesNeeded
            }

            val updateResult = updateChaptersInDatabase(chaptersToUpdate, read)
            if (updateResult is Result.Failure) {
                return@withNonCancellableContext updateResult
            }

            // Handle download cleanup for read operations
            if (read) {
                handleDownloadCleanup(chaptersToUpdate)
            }
        }

        logcat(LogPriority.DEBUG, TAG) {
            "Set read status completed in ${operationTime.inWholeMilliseconds}ms for ${chapters.size} chapters"
        }

        Result.Success(chapters.size)
    }

    /**
     * Sets read status for all chapters in a manga.
     *
     * @param mangaId The manga ID whose chapters should be updated
     * @param read The target read status
     * @return [Result] indicating the operation outcome
     */
    suspend fun await(mangaId: Long, read: Boolean): Result = withNonCancellableContext {
        require(mangaId > 0) { "Manga ID must be positive" }

        val chapters = chapterRepository.getChapterByMangaId(mangaId)
        if (chapters.isEmpty()) {
            return@withNonCancellableContext Result.NoChapters
        }

        await(read, *chapters.toTypedArray())
    }

    /**
     * Sets read status for all chapters in a manga.
     *
     * @param manga The manga whose chapters should be updated
     * @param read The target read status
     * @return [Result] indicating the operation outcome
     */
    suspend fun await(manga: Manga, read: Boolean): Result {
        return await(manga.id, read)
    }

    /**
     * Toggles read status for specific chapters (read -> unread, unread -> read).
     *
     * @param chapters The chapters to toggle
     * @return [Result] indicating the operation outcome
     */
    suspend fun toggle(vararg chapters: Chapter): Result {
        if (chapters.isEmpty()) return Result.NoChapters
        
        // If any chapter is unread, mark all as read, otherwise mark all as unread
        val shouldMarkAsRead = chapters.any { !it.read }
        return await(shouldMarkAsRead, *chapters)
    }

    /**
     * Marks only the next unread chapter in a manga as read.
     *
     * @param mangaId The manga ID to find the next unread chapter
     * @return [Result] indicating the operation outcome
     */
    suspend fun markNextUnreadAsRead(mangaId: Long): Result = withNonCancellableContext {
        require(mangaId > 0) { "Manga ID must be positive" }

        val nextUnreadChapter = chapterRepository.getChapterByMangaId(mangaId)
            .filter { !it.read }
            .minByOrNull { it.chapterNumber }

        if (nextUnreadChapter == null) {
            return@withNonCancellableContext Result.NoUnreadChapters
        }

        await(read = true, nextUnreadChapter)
    }

    // region Private Helper Methods

    /**
     * Filters chapters that actually need status updates.
     */
    private fun filterChaptersNeedingUpdate(chapters: List<Chapter>, targetReadStatus: Boolean): List<Chapter> {
        return chapters.filter { chapter ->
            when (targetReadStatus) {
                true -> !chapter.read // Mark as read: only unread chapters
                false -> chapter.read || chapter.lastPageRead > 0 // Mark as unread: read chapters or those with progress
            }
        }
    }

    /**
     * Updates chapters in the database with the new read status.
     */
    private suspend fun updateChaptersInDatabase(chapters: List<Chapter>, read: Boolean): Result {
        return try {
            val updates = chapters.map { chapterUpdateMapper(it, read) }
            chapterRepository.updateAll(updates)
            Result.Success(chapters.size)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to update chapters in database" }
            Result.DatabaseError(e)
        }
    }

    /**
     * Handles download cleanup for chapters marked as read based on preferences.
     */
    private suspend fun handleDownloadCleanup(chapters: List<Chapter>) {
        val removeAfterRead = downloadPreferences.removeAfterMarkedAsRead().get()
        if (!removeAfterRead) return

        // Group by manga for efficient batch processing
        chapters.groupBy { it.mangaId }
            .forEach { (mangaId, mangaChapters) ->
                try {
                    val manga = mangaRepository.getMangaById(mangaId)
                    deleteDownload.awaitAll(manga, mangaChapters.toTypedArray())
                } catch (e: Exception) {
                    logcat(LogPriority.WARN, e) {
                        "Failed to delete downloads for manga $mangaId after marking as read"
                    }
                    // Continue with other manga even if one fails
                }
            }
    }

    // endregion

    // region Result Sealed Interface

    /**
     * Result of the set read status operation.
     */
    sealed interface Result {
        /**
         * Operation completed successfully.
         * @property chaptersAffected Number of chapters that were updated
         */
        data class Success(val chaptersAffected: Int) : Result

        /**
         * No chapters were provided or found for the operation.
         */
        object NoChapters : Result

        /**
         * No unread chapters found when trying to mark next as read.
         */
        object NoUnreadChapters : Result

        /**
         * No changes were needed as chapters already had the target status.
         */
        object NoChangesNeeded : Result

        /**
         * Database operation failed.
         * @property error The exception that occurred
         */
        data class DatabaseError(val error: Exception) : Result

        /**
         * Operation failed due to an internal error.
         * @property error The exception that occurred
         */
        data class InternalError(val error: Exception) : Result
    }

    // endregion
}

// Extension functions for additional utility

/**
 * Extension function to check if all chapters in a collection are read.
 */
val Collection<Chapter>.allRead: Boolean
    get() = all { it.read }

/**
 * Extension function to check if all chapters in a collection are unread.
 */
val Collection<Chapter>.allUnread: Boolean
    get() = all { !it.read }

/**
 * Extension function to get the read percentage of chapters.
 */
val Collection<Chapter>.readPercentage: Double
    get() = if (isEmpty()) 0.0 else count { it.read }.toDouble() / size.toDouble() * 100

/**
 * Extension function to find the last read chapter.
 */
fun Collection<Chapter>.findLastReadChapter(): Chapter? {
    return filter { it.read }
        .maxByOrNull { it.dateRead ?: 0L }
}

// Usage examples:
/*
 * val setReadStatus = SetReadStatus(downloadPreferences, deleteDownload, mangaRepository, chapterRepository)
 *
 * // Mark specific chapters as read
 * val result = setReadStatus.await(read = true, chapter1, chapter2, chapter3)
 * when (result) {
 *   is SetReadStatus.Result.Success -> println("Updated ${result.chaptersAffected} chapters")
 *   SetReadStatus.Result.NoChapters -> println("No chapters to update")
 *   is SetReadStatus.Result.DatabaseError -> showError(result.error)
 * }
 *
 * // Mark all chapters in manga as unread
 * setReadStatus.await(mangaId, read = false)
 *
 * // Toggle chapter status
 * setReadStatus.toggle(chapter1, chapter2)
 *
 * // Mark next unread chapter
 * setReadStatus.markNextUnreadAsRead(mangaId)
 *
 * // Check chapter collection status
 * if (chapters.allRead) {
 *   showCompletionBadge()
 * }
 */package eu.kanade.domain.chapter.interactor

import eu.kanade.domain.download.interactor.DeleteDownload
import kotlinx.coroutines.flow.first
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository
import kotlin.time.measureTime

/**
 * Interactor for managing chapter read status with support for download cleanup.
 * Handles marking chapters as read/unread and automatically manages downloads based on preferences.
 *
 * @property downloadPreferences Preferences for download behavior
 * @property deleteDownload Interactor for deleting downloads
 * @property mangaRepository Repository for manga data access
 * @property chapterRepository Repository for chapter data access
 */
class SetReadStatus(
    private val downloadPreferences: DownloadPreferences,
    private val deleteDownload: DeleteDownload,
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
) {

    companion object {
        private const val TAG = "SetReadStatus"
    }

    /**
     * Chapter update mapper that resets last page read when marking as unread.
     */
    private val chapterUpdateMapper: (Chapter, Boolean) -> ChapterUpdate = { chapter, read ->
        ChapterUpdate(
            id = chapter.id,
            read = read,
            lastPageRead = if (read) chapter.lastPageRead else 0, // Reset when unreading
            dateFetch = chapter.dateFetch, // Preserve existing values
            dateUpload = chapter.dateUpload,
            name = chapter.name,
            scanlator = chapter.scanlator,
            url = chapter.url,
            chapterNumber = chapter.chapterNumber,
            volumeNumber = chapter.volumeNumber,
            mangaId = chapter.mangaId,
            sourceOrder = chapter.sourceOrder
        )
    }

    /**
     * Sets read status for specific chapters with download cleanup if enabled.
     *
     * @param read The target read status (true for read, false for unread)
     * @param chapters The chapters to update
     * @return [Result] indicating the operation outcome
     */
    suspend fun await(read: Boolean, vararg chapters: Chapter): Result = withNonCancellableContext {
        if (chapters.isEmpty()) {
            return@withNonCancellableContext Result.NoChapters
        }

        val operationTime = measureTime {
            val chaptersToUpdate = filterChaptersNeedingUpdate(chapters.toList(), read)
            if (chaptersToUpdate.isEmpty()) {
                return@withNonCancellableContext Result.NoChangesNeeded
            }

            val updateResult = updateChaptersInDatabase(chaptersToUpdate, read)
            if (updateResult is Result.Failure) {
                return@withNonCancellableContext updateResult
            }

            // Handle download cleanup for read operations
            if (read) {
                handleDownloadCleanup(chaptersToUpdate)
            }
        }

        logcat(LogPriority.DEBUG, TAG) {
            "Set read status completed in ${operationTime.inWholeMilliseconds}ms for ${chapters.size} chapters"
        }

        Result.Success(chapters.size)
    }

    /**
     * Sets read status for all chapters in a manga.
     *
     * @param mangaId The manga ID whose chapters should be updated
     * @param read The target read status
     * @return [Result] indicating the operation outcome
     */
    suspend fun await(mangaId: Long, read: Boolean): Result = withNonCancellableContext {
        require(mangaId > 0) { "Manga ID must be positive" }

        val chapters = chapterRepository.getChapterByMangaId(mangaId)
        if (chapters.isEmpty()) {
            return@withNonCancellableContext Result.NoChapters
        }

        await(read, *chapters.toTypedArray())
    }

    /**
     * Sets read status for all chapters in a manga.
     *
     * @param manga The manga whose chapters should be updated
     * @param read The target read status
     * @return [Result] indicating the operation outcome
     */
    suspend fun await(manga: Manga, read: Boolean): Result {
        return await(manga.id, read)
    }

    /**
     * Toggles read status for specific chapters (read -> unread, unread -> read).
     *
     * @param chapters The chapters to toggle
     * @return [Result] indicating the operation outcome
     */
    suspend fun toggle(vararg chapters: Chapter): Result {
        if (chapters.isEmpty()) return Result.NoChapters
        
        // If any chapter is unread, mark all as read, otherwise mark all as unread
        val shouldMarkAsRead = chapters.any { !it.read }
        return await(shouldMarkAsRead, *chapters)
    }

    /**
     * Marks only the next unread chapter in a manga as read.
     *
     * @param mangaId The manga ID to find the next unread chapter
     * @return [Result] indicating the operation outcome
     */
    suspend fun markNextUnreadAsRead(mangaId: Long): Result = withNonCancellableContext {
        require(mangaId > 0) { "Manga ID must be positive" }

        val nextUnreadChapter = chapterRepository.getChapterByMangaId(mangaId)
            .filter { !it.read }
            .minByOrNull { it.chapterNumber }

        if (nextUnreadChapter == null) {
            return@withNonCancellableContext Result.NoUnreadChapters
        }

        await(read = true, nextUnreadChapter)
    }

    // region Private Helper Methods

    /**
     * Filters chapters that actually need status updates.
     */
    private fun filterChaptersNeedingUpdate(chapters: List<Chapter>, targetReadStatus: Boolean): List<Chapter> {
        return chapters.filter { chapter ->
            when (targetReadStatus) {
                true -> !chapter.read // Mark as read: only unread chapters
                false -> chapter.read || chapter.lastPageRead > 0 // Mark as unread: read chapters or those with progress
            }
        }
    }

    /**
     * Updates chapters in the database with the new read status.
     */
    private suspend fun updateChaptersInDatabase(chapters: List<Chapter>, read: Boolean): Result {
        return try {
            val updates = chapters.map { chapterUpdateMapper(it, read) }
            chapterRepository.updateAll(updates)
            Result.Success(chapters.size)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to update chapters in database" }
            Result.DatabaseError(e)
        }
    }

    /**
     * Handles download cleanup for chapters marked as read based on preferences.
     */
    private suspend fun handleDownloadCleanup(chapters: List<Chapter>) {
        val removeAfterRead = downloadPreferences.removeAfterMarkedAsRead().get()
        if (!removeAfterRead) return

        // Group by manga for efficient batch processing
        chapters.groupBy { it.mangaId }
            .forEach { (mangaId, mangaChapters) ->
                try {
                    val manga = mangaRepository.getMangaById(mangaId)
                    deleteDownload.awaitAll(manga, mangaChapters.toTypedArray())
                } catch (e: Exception) {
                    logcat(LogPriority.WARN, e) {
                        "Failed to delete downloads for manga $mangaId after marking as read"
                    }
                    // Continue with other manga even if one fails
                }
            }
    }

    // endregion

    // region Result Sealed Interface

    /**
     * Result of the set read status operation.
     */
    sealed interface Result {
        /**
         * Operation completed successfully.
         * @property chaptersAffected Number of chapters that were updated
         */
        data class Success(val chaptersAffected: Int) : Result

        /**
         * No chapters were provided or found for the operation.
         */
        object NoChapters : Result

        /**
         * No unread chapters found when trying to mark next as read.
         */
        object NoUnreadChapters : Result

        /**
         * No changes were needed as chapters already had the target status.
         */
        object NoChangesNeeded : Result

        /**
         * Database operation failed.
         * @property error The exception that occurred
         */
        data class DatabaseError(val error: Exception) : Result

        /**
         * Operation failed due to an internal error.
         * @property error The exception that occurred
         */
        data class InternalError(val error: Exception) : Result
    }

    // endregion
}

// Extension functions for additional utility

/**
 * Extension function to check if all chapters in a collection are read.
 */
val Collection<Chapter>.allRead: Boolean
    get() = all { it.read }

/**
 * Extension function to check if all chapters in a collection are unread.
 */
val Collection<Chapter>.allUnread: Boolean
    get() = all { !it.read }

/**
 * Extension function to get the read percentage of chapters.
 */
val Collection<Chapter>.readPercentage: Double
    get() = if (isEmpty()) 0.0 else count { it.read }.toDouble() / size.toDouble() * 100

/**
 * Extension function to find the last read chapter.
 */
fun Collection<Chapter>.findLastReadChapter(): Chapter? {
    return filter { it.read }
        .maxByOrNull { it.dateRead ?: 0L }
}

// Usage examples:
/*
 * val setReadStatus = SetReadStatus(downloadPreferences, deleteDownload, mangaRepository, chapterRepository)
 *
 * // Mark specific chapters as read
 * val result = setReadStatus.await(read = true, chapter1, chapter2, chapter3)
 * when (result) {
 *   is SetReadStatus.Result.Success -> println("Updated ${result.chaptersAffected} chapters")
 *   SetReadStatus.Result.NoChapters -> println("No chapters to update")
 *   is SetReadStatus.Result.DatabaseError -> showError(result.error)
 * }
 *
 * // Mark all chapters in manga as unread
 * setReadStatus.await(mangaId, read = false)
 *
 * // Toggle chapter status
 * setReadStatus.toggle(chapter1, chapter2)
 *
 * // Mark next unread chapter
 * setReadStatus.markNextUnreadAsRead(mangaId)
 *
 * // Check chapter collection status
 * if (chapters.allRead) {
 *   showCompletionBadge()
 * }
 */
