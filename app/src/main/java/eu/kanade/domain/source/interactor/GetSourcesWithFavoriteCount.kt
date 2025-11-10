package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import tachiyomi.core.common.util.lang.compareToWithCollator
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.source.local.isLocal

class GetSourcesWithFavoriteCount(
    private val repository: SourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<List<Pair<Source, Long>>> = combine(
        preferences.migrationSortingDirection().changes(),
        preferences.migrationSortingMode().changes(),
        repository.getSourcesWithFavoriteCount(),
    ) { direction, mode, sources ->
        sources
            .filterNot { (source, _) -> source.isLocal() }
            .sortedWith(createComparator(direction, mode))
    }

    private fun createComparator(
        direction: SetMigrateSorting.Direction,
        mode: SetMigrateSorting.Mode,
    ): Comparator<Pair<Source, Long>> {
        val baseComparator = when (mode) {
            SetMigrateSorting.Mode.ALPHABETICAL -> compareBy<Pair<Source, Long>> { (source, _) ->
                when {
                    source.isStub -> ""
                    else -> source.name.lowercase()
                }
            }.thenComparator { a, b ->
                when {
                    a.first.isStub && !b.first.isStub -> -1
                    b.first.isStub && !a.first.isStub -> 1
                    else -> a.first.name.compareToWithCollator(b.first.name)
                }
            }
            
            SetMigrateSorting.Mode.TOTAL -> compareBy<Pair<Source, Long>> { (source, _) ->
                !source.isStub
            }.thenBy { (_, count) -> count }
        }

        return when (direction) {
            SetMigrateSorting.Direction.ASCENDING -> baseComparator
            SetMigrateSorting.Direction.DESCENDING -> baseComparator.reversed()
        }
    }
}
