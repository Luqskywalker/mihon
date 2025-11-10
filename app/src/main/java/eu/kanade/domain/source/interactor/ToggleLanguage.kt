package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.getAndSet

class ToggleLanguage(
    private val preferences: SourcePreferences,
) {

    fun toggle(language: String) {
        preferences.enabledLanguages().getAndSet { languages ->
            if (language in languages) languages - language else languages + language
        }
    }

    fun enable(language: String) {
        preferences.enabledLanguages().getAndSet { it + language }
    }

    fun enableAll(languages: Collection<String>) {
        preferences.enabledLanguages().getAndSet { it + languages }
    }

    fun disable(language: String) {
        preferences.enabledLanguages().getAndSet { it - language }
    }

    fun disableAll(languages: Collection<String>) {
        preferences.enabledLanguages().getAndSet { it - languages }
    }

    fun set(language: String, enabled: Boolean) {
        if (enabled) enable(language) else disable(language)
    }

    fun setAll(languages: Collection<String>, enabled: Boolean) {
        if (enabled) enableAll(languages) else disableAll(languages)
    }

    fun clear() {
        preferences.enabledLanguages().delete()
    }
}
