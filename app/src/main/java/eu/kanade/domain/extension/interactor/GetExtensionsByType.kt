package eu.kanade.domain.extension.interactor

import eu.kanade.domain.extension.model.Extensions
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GetExtensionsByType(
    private val preferences: SourcePreferences,
    private val extensionManager: ExtensionManager,
) {

    fun subscribe(): Flow<Extensions> = combine(
        preferences.enabledLanguages().changes(),
        extensionManager.installedExtensionsFlow,
        extensionManager.untrustedExtensionsFlow,
        extensionManager.availableExtensionsFlow,
    ) { enabledLanguages, installed, untrusted, available ->
        val showNsfw = preferences.showNsfwSource().get()
        
        Extensions(
            updates = installed.filterExtensions(showNsfw).partition { it.hasUpdate }.first,
            installed = installed.filterExtensions(showNsfw).sortedByTypeAndName(),
            available = available.filterAvailableExtensions(installed, untrusted, enabledLanguages, showNsfw),
            untrusted = untrusted.sortedBy { it.name.lowercase() },
        )
    }

    private fun List<Extension.Installed>.filterExtensions(showNsfw: Boolean) =
        filter { showNsfw || !it.isNsfw }

    private fun List<Extension.Installed>.sortedByTypeAndName() =
        sortedWith(compareBy({ !it.isObsolete }, { it.name.lowercase() }))

    private fun List<Extension.Available>.filterAvailableExtensions(
        installed: List<Extension.Installed>,
        untrusted: List<Extension.Untrusted>,
        enabledLanguages: Set<String>,
        showNsfw: Boolean,
    ) = filter { available ->
        available.pkgName !in installed.map { it.pkgName } &&
            available.pkgName !in untrusted.map { it.pkgName } &&
            (showNsfw || !available.isNsfw)
    }.flatMap { it.toLanguageSpecificExtensions(enabledLanguages) }
     .sortedBy { it.name.lowercase() }

    private fun Extension.Available.toLanguageSpecificExtensions(enabledLanguages: Set<String>): List<Extension.Available> =
        if (sources.isEmpty()) {
            if (lang in enabledLanguages) listOf(this) else emptyList()
        } else {
            sources.filter { it.lang in enabledLanguages }.map { source ->
                copy(
                    name = source.name,
                    lang = source.lang,
                    pkgName = "$pkgName-${source.id}",
                    sources = listOf(source),
                )
            }
        }
}
}
