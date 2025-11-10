package eu.kanade.domain.manga.interactor

import tachiyomi.data.DatabaseHandler

class SetExcludedScanlators(
    private val handler: DatabaseHandler,
) {

    suspend fun await(mangaId: Long, excludedScanlators: Set<String>) {
        handler.await(inTransaction = true) {
            val currentExcluded = excluded_scanlatorsQueries
                .getExcludedScanlatorsByMangaId(mangaId)
                .executeAsList()
                .toSet()

            val toAdd = excludedScanlators - currentExcluded
            toAdd.forEach { scanlator ->
                excluded_scanlatorsQueries.insert(mangaId, scanlator)
            }

            val toRemove = currentExcluded - excludedScanlators
            excluded_scanlatorsQueries.remove(mangaId, toRemove)
        }
    }
}
