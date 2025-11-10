package eu.kanade.domain.track.store

import android.content.Context
import androidx.core.content.edit
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

class DelayedTrackingStore(context: Context) {

    /**
     * Preference file where queued tracking updates are stored.
     */
    private val preferences = context.getSharedPreferences("tracking_queue", Context.MODE_PRIVATE)

    fun add(trackId: Long, lastChapterRead: Double) {
        val previousLastChapterRead = preferences.getFloat(trackId.toString(), 0f)
        if (lastChapterRead > previousLastChapterRead) {
            logcat(LogPriority.DEBUG) { "Queuing track item: $trackId, last chapter read: $lastChapterRead" }
            preferences.edit {
                putFloat(trackId.toString(), lastChapterRead.toFloat())
            }
        }
    }

    fun remove(trackId: Long) {
        preferences.edit {
            remove(trackId.toString())
        }
    }

    fun removeAll(trackIds: Collection<Long>) {
        preferences.edit {
            trackIds.forEach { remove(it.toString()) }
        }
    }

    fun getItems(): List<DelayedTrackingItem> = preferences.all.mapNotNull { (key, value) ->
        DelayedTrackingItem(
            trackId = key.toLongOrNull() ?: return@mapNotNull null,
            lastChapterRead = (value as? Float) ?: return@mapNotNull null,
        )
    }

    fun getItem(trackId: Long): DelayedTrackingItem? = preferences.getFloat(trackId.toString(), -1f)
        .takeIf { it >= 0 }
        ?.let { DelayedTrackingItem(trackId, it) }

    fun clear() {
        preferences.edit { clear() }
    }

    val isEmpty: Boolean
        get() = preferences.all.isEmpty()

    val size: Int
        get() = preferences.all.size

    data class DelayedTrackingItem(
        val trackId: Long,
        val lastChapterRead: Float,
    ) {
        val isValid: Boolean
            get() = trackId > 0 && lastChapterRead >= 0
    }
}
    data class DelayedTrackingItem(
        val trackId: Long,
        val lastChapterRead: Float,
    )
}
