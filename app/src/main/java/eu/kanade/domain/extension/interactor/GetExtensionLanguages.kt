package eu.kanade.domain.extension.interactor

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GetExtensionLanguages(
    private val preferences: SourcePreferences,
    private val extensionManager: ExtensionManager,
) {

    fun subscribe(): Flow<List<String>> = combine(
        preferences.enabledLanguages().changes(),
        extensionManager.availableExtensionsFlow,
    ) { enabledLanguages, extensions ->
        extensions
            .getAllLanguages()
            .distinct()
            .sortedWithLanguagesFirst(enabledLanguages)
    }

    private fun List<ExtensionManager.ExtensionItem>.getAllLanguages(): List<String> = 
        flatMap { ext -> 
            if (ext.sources.isEmpty()) listOf(ext.lang) 
            else ext.sources.map { it.lang } 
        }

    private fun List<String>.sortedWithLanguagesFirst(enabledLanguages: Set<String>) = 
        sortedWith(
            compareBy<String> { it !in enabledLanguages }
                .then(LocaleHelper.comparator)
        )
}
