package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.source.model.Source

class ToggleSource(
    private val preferences: SourcePreferences,
) {

    fun enable(source: Source) = set(source.id, enabled = true)
    fun enable(sourceId: Long) = set(sourceId, enabled = true)
    fun enable(sourceIds: List<Long>) = set(sourceIds, enabled = true)

    fun disable(source: Source) = set(source.id, enabled = false)
    fun disable(sourceId: Long) = set(sourceId, enabled = false)
    fun disable(sourceIds: List<Long>) = set(sourceIds, enabled = false)

    fun toggle(source: Source) = set(source.id, enabled = !isEnabled(source.id))
    fun toggle(sourceId: Long) = set(sourceId, enabled = !isEnabled(sourceId))

    fun set(source: Source, enabled: Boolean) = set(source.id, enabled)
    fun set(sourceId: Long, enabled: Boolean) {
        preferences.disabledSources().getAndSet { disabled ->
            if (enabled) disabled - "$sourceId" else disabled + "$sourceId"
        }
    }

    fun set(sourceIds: List<Long>, enabled: Boolean) {
        val ids = sourceIds.map { it.toString() }
        preferences.disabledSources().getAndSet { disabled ->
            if (enabled) disabled - ids else disabled + ids
        }
    }

    fun isEnabled(source: Source): Boolean = isEnabled(source.id)
    fun isEnabled(sourceId: Long): Boolean = "$sourceId" !in preferences.disabledSources().get()
}
