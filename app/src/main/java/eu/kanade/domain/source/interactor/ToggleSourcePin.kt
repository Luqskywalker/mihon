package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.source.model.Source

class ToggleSourcePin(
    private val preferences: SourcePreferences,
) {

    fun toggle(source: Source) {
        preferences.pinnedSources().getAndSet { pinned ->
            if ("${source.id}" in pinned) pinned - "${source.id}" else pinned + "${source.id}"
        }
    }

    fun pin(source: Source) {
        preferences.pinnedSources().getAndSet { it + "${source.id}" }
    }

    fun pin(sources: List<Source>) {
        val sourceIds = sources.map { "${it.id}" }
        preferences.pinnedSources().getAndSet { it + sourceIds }
    }

    fun unpin(source: Source) {
        preferences.pinnedSources().getAndSet { it - "${source.id}" }
    }

    fun unpin(sources: List<Source>) {
        val sourceIds = sources.map { "${it.id}" }
        preferences.pinnedSources().getAndSet { it - sourceIds }
    }

    fun isPinned(source: Source): Boolean = "${source.id}" in preferences.pinnedSources().get()

    fun clearAll() {
        preferences.pinnedSources().delete()
    }
}
