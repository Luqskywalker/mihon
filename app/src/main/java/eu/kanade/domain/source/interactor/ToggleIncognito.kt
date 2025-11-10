package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.getAndSet

class ToggleIncognito(
    private val preferences: SourcePreferences,
) {
    
    fun toggle(packageName: String) {
        preferences.incognitoExtensions().getAndSet { extensions ->
            if (packageName in extensions) extensions - packageName else extensions + packageName
        }
    }

    fun enable(packageName: String) {
        preferences.incognitoExtensions().getAndSet { it + packageName }
    }

    fun disable(packageName: String) {
        preferences.incognitoExtensions().getAndSet { it - packageName }
    }

    fun set(packageName: String, enabled: Boolean) {
        if (enabled) enable(packageName) else disable(packageName)
    }
}
