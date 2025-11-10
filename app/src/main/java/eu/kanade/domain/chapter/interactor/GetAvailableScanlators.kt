package eu.kanade.domain.chapter.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import tachiyomi.domain.chapter.model.Scanlator
import tachiyomi.domain.chapter.repository.ChapterRepository
import java.util.SortedSet
import java.util.TreeSet
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Interactor for retrieving and managing available scanlators for manga.
 * Handles data cleaning, normalization, and provides both one-time and streaming access.
 */
class GetAvailableScanlators(
    private val repository: ChapterRepository,
) {

    companion object {
        private val SCANLATOR_SPLIT_REGEX = Regex("[,&/|]")
        private val MULTIPLE_WHITESPACE_REGEX = Regex("\\s+")
    }

    /**
     * Cleans and normalizes a list of scanlator strings.
     *
     * @receiver Raw scanlator strings from the repository
     * @return A sorted set of unique, cleaned scanlator names
     */
    @OptIn(ExperimentalContracts::class)
    private fun List<String>.cleanupAvailableScanlators(): SortedSet<String> {
        contract {
            returns() implies (this@cleanupAvailableScanlators != null)
        }

        if (isEmpty()) return sortedSetOf()

        return asSequence()
            .filter { it.isNotBlank() }
            .flatMap { scanlator ->
                // Split combined scanlator strings (e.g., "Group A & Group B")
                if (scanlator.contains(SCANLATOR_SPLIT_REGEX)) {
                    scanlator.split(SCANLATOR_SPLIT_REGEX)
                } else {
                    listOf(scanlator)
                }
            }
            .map { it.cleanupScanlatorName() }
            .filter { it.isNotBlank() }
            .toCollection(TreeSet(String.CASE_INSENSITIVE_ORDER))
    }

    /**
     * Cleans up an individual scanlator name.
     */
    private fun String.cleanupScanlatorName(): String {
        return trim()
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .removePrefix("[")
            .removeSuffix("]")
            .removeSurrounding("(", ")")
            .removeSurrounding("\"", "\"")
            .trim()
    }

    /**
     * Retrieves available scanlators for a manga as a one-time operation.
     *
     * @param mangaId The ID of the manga to get scanlators for
     * @return A sorted set of unique scanlator names, empty if none found
     *
     * @throws IllegalArgumentException if mangaId is invalid
     */
    suspend fun await(mangaId: Long): SortedSet<String> {
        require(mangaId > 0) { "Manga ID must be positive" }

        return repository.getScanlatorsByMangaId(mangaId)
            .cleanupAvailableScanlators()
    }

    /**
     * Subscribes to available scanlators for a manga with real-time updates.
     *
     * @param mangaId The ID of the manga to monitor scanlators for
     * @return A flow emitting sorted sets of unique scanlator names
     *
     * @throws IllegalArgumentException if mangaId is invalid
     */
    fun subscribe(mangaId: Long): Flow<SortedSet<String>> {
        require(mangaId > 0) { "Manga ID must be positive" }

        return repository.getScanlatorsByMangaIdAsFlow(mangaId)
            .map { it.cleanupAvailableScanlators() }
            .distinctUntilChanged() // Only emit when the actual set changes
    }

    /**
     * Retrieves available scanlators for multiple manga as a one-time operation.
     *
     * @param mangaIds Collection of manga IDs to get scanlators for
     * @return A map where keys are manga IDs and values are sorted sets of scanlators
     */
    suspend fun awaitMultiple(mangaIds: Collection<Long>): Map<Long, SortedSet<String>> {
        require(mangaIds.isNotEmpty()) { "Manga IDs collection cannot be empty" }
        require(mangaIds.all { it > 0 }) { "All manga IDs must be positive" }

        return repository.getScanlatorsByMangaIds(mangaIds)
            .mapValues { (_, scanlators) -> scanlators.cleanupAvailableScanlators() }
    }

    /**
     * Finds common scanlators across multiple manga.
     *
     * @param mangaIds Collection of manga IDs to find common scanlators for
     * @return A sorted set of scanlators common to all specified manga
     */
    suspend fun findCommonScanlators(mangaIds: Collection<Long>): SortedSet<String> {
        require(mangaIds.isNotEmpty()) { "Manga IDs collection cannot be empty" }

        val allScanlators = awaitMultiple(mangaIds).values
        if (allScanlators.isEmpty()) return sortedSetOf()

        return allScanlators.reduce { common, current ->
            common.intersect(current).toSortedSet(String.CASE_INSENSITIVE_ORDER)
        }
    }

    /**
     * Searches scanlators by name pattern for a specific manga.
     *
     * @param mangaId The ID of the manga to search within
     * @param query The search query (case-insensitive)
     * @return A sorted set of matching scanlator names
     */
    suspend fun search(mangaId: Long, query: String): SortedSet<String> {
        require(mangaId > 0) { "Manga ID must be positive" }
        require(query.isNotBlank()) { "Search query cannot be blank" }

        val allScanlators = await(mangaId)
        return allScanlators.filter { scanlator ->
            scanlator.contains(query, ignoreCase = true)
        }.toSortedSet(String.CASE_INSENSITIVE_ORDER)
    }

    /**
     * Gets scanlators grouped by first letter for UI sections.
     *
     * @param mangaId The ID of the manga to get grouped scanlators for
     * @return A map where keys are section headers and values are sorted scanlator lists
     */
    suspend fun getGroupedBySection(mangaId: Long): Map<String, List<String>> {
        val scanlators = await(mangaId)
        
        return scanlators.groupBy { scanlator ->
            when {
                scanlator.isEmpty() -> "#"
                scanlator[0].isLetter() -> scanlator[0].uppercase()
                else -> "#"
            }
        }.mapValues { (_, sectionScanlators) ->
            sectionScanlators.sortedWith(String.CASE_INSENSITIVE_ORDER)
        }
    }

    /**
     * Gets the count of available scanlators for a manga.
     *
     * @param mangaId The ID of the manga to count scanlators for
     * @return The number of unique scanlators
     */
    suspend fun getCount(mangaId: Long): Int {
        require(mangaId > 0) { "Manga ID must be positive" }
        
        return await(mangaId).size
    }

    /**
     * Checks if a manga has multiple scanlators available.
     *
     * @param mangaId The ID of the manga to check
     * @return true if there are multiple scanlators, false otherwise
     */
    suspend fun hasMultipleScanlators(mangaId: Long): Boolean {
        return getCount(mangaId) > 1
    }
}

// Extension functions for additional utility

/**
 * Extension function to check if scanlator set contains a specific scanlator (case-insensitive).
 */
fun Set<String>.containsScanlator(scanlator: String): Boolean {
    return any { it.equals(scanlator, ignoreCase = true) }
}

/**
 * Extension function to find similar scanlators based on fuzzy matching.
 */
fun Set<String>.findSimilarScanlators(query: String, threshold: Double = 0.7): Set<String> {
    // Simple implementation - could be enhanced with proper fuzzy matching library
    return filter { scanlator ->
        val similarity = calculateStringSimilarity(scanlator, query)
        similarity >= threshold
    }.toSet()
}

/**
 * Calculates simple string similarity (0.0 to 1.0).
 * For production use, consider using a proper string similarity algorithm.
 */
private fun calculateStringSimilarity(s1: String, s2: String): Double {
    if (s1.equals(s2, ignoreCase = true)) return 1.0
    
    val longer = if (s1.length > s2.length) s1 else s2
    val shorter = if (s1.length > s2.length) s2 else s1
    
    return if (longer.contains(shorter, ignoreCase = true)) {
        shorter.length.toDouble() / longer.length
    } else {
        0.0
    }
}

// Usage examples:
/*
 * val interactor = GetAvailableScanlators(chapterRepository)
 * 
 * // Basic usage
 * val scanlators = interactor.await(mangaId)
 * 
 * // Real-time updates
 * interactor.subscribe(mangaId)
 *     .collect { scanlators ->
 *         updateScanlatorFilter(scanlators)
 *     }
 * 
 * // Multiple manga
 * val multipleScanlators = interactor.awaitMultiple(listOf(1L, 2L, 3L))
 * 
 * // Common scanlators
 * val common = interactor.findCommonScanlators(listOf(1L, 2L))
 * 
 * // Search functionality
 * val results = interactor.search(mangaId, "manga")
 * 
 * // Grouped for UI
 * val grouped = interactor.getGroupedBySection(mangaId)
 * 
 * // Utility checks
 * if (interactor.hasMultipleScanlators(mangaId)) {
 *     showScanlatorSelector()
 * }
 */
