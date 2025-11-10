package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.repository.SourceRepository
import java.util.SortedMap

class GetLanguagesWithSources(
    private val repository: SourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<SortedMap<String, List<Source>>> = combine(
        preferences.enabledLanguages().changes(),
        preferences.disabledSources().changes(),
        repository.getOnlineSources(),
    ) { enabledLangs, disabledIds, sources ->
        sources
            .sortedWith(compareSourcePriority(disabledIds))
            .groupBy { it.lang }
            .toSortedMap(compareLanguagePriority(enabledLangs))
    }

    private fun compareSourcePriority(disabledIds: Set<String>) = 
        compareBy<Source> { it.id.toString() in disabledIds }
            .thenBy { it.name.lowercase() }

    private fun compareLanguagePriority(enabledLangs: Set<String>) = 
        compareBy<String> { it !in enabledLangs }
            .then(LocaleHelper.comparator)
}
