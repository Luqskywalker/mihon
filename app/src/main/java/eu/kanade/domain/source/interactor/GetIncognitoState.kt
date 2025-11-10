package eu.kanade.domain.source.interactor

import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class GetIncognitoState(
    private val basePreferences: BasePreferences,
    private val sourcePreferences: SourcePreferences,
    private val extensionManager: ExtensionManager,
) {

    fun await(sourceId: Long?): Boolean = when {
        basePreferences.incognitoMode().get() -> true
        sourceId == null -> false
        else -> extensionManager.getExtensionPackage(sourceId) in sourcePreferences.incognitoExtensions().get()
    }

    fun subscribe(sourceId: Long?): Flow<Boolean> = when (sourceId) {
        null -> basePreferences.incognitoMode().changes()
        else -> combine(
            basePreferences.incognitoMode().changes(),
            sourcePreferences.incognitoExtensions().changes(),
            extensionManager.getExtensionPackageAsFlow(sourceId),
        ) { incognito, extensions, pkg ->
            incognito || pkg in extensions
        }.distinctUntilChanged()
    }
}
