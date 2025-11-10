package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Pins
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.source.local.isLocal

class GetEnabledSources(
    private val repository: SourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<List<Source>> = combine(
        preferences.pinnedSources().changes(),
        preferences.enabledLanguages().changes(),
        preferences.disabledSources().changes(),
        preferences.lastUsedSource().changes(),
        repository.getSources(),
    ) { pinnedIds, enabledLangs, disabledIds, lastUsedId, sources ->
        sources
            .filter { it.isEnabled(enabledLangs, disabledIds) }
            .sortedBy { it.name.lowercase() }
            .flatMap { it.toSourceWithFlags(pinnedIds, lastUsedId) }
    }.distinctUntilChanged()

    private fun Source.isEnabled(enabledLangs: Set<String>, disabledIds: Set<String>): Boolean =
        (lang in enabledLangs || isLocal()) && id.toString() !in disabledIds

    private fun Source.toSourceWithFlags(pinnedIds: Set<String>, lastUsedId: Long): List<Source> {
        val isPinned = id.toString() in pinnedIds
        val isLastUsed = id == lastUsedId
        
        return buildList {
            add(copy(pin = if (isPinned) Pins.pinned else Pins.unpinned))
            if (isLastUsed) {
                add(copy(isUsedLast = true, pin = if (isPinned) Pins.pinned - Pin.Actual else Pins.unpinned))
            }
        }
    }
}
