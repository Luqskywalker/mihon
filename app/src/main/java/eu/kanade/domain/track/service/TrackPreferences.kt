package eu.kanade.domain.track.service

import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class TrackPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun trackUsername(tracker: Tracker) = preferenceStore.getString(
        key = "track_username_${tracker.id}",
        defaultValue = "",
    )

    fun trackPassword(tracker: Tracker) = preferenceStore.getString(
        key = "track_password_${tracker.id}",
        defaultValue = "",
    )

    fun trackAuthExpired(tracker: Tracker) = preferenceStore.getBoolean(
        key = "track_auth_expired_${tracker.id}",
        defaultValue = false,
    )

    fun setCredentials(tracker: Tracker, username: String, password: String) {
        trackUsername(tracker).set(username)
        trackPassword(tracker).set(password)
        trackAuthExpired(tracker).set(false)
    }

    fun clearCredentials(tracker: Tracker) {
        trackUsername(tracker).delete()
        trackPassword(tracker).delete()
        trackAuthExpired(tracker).delete()
        trackToken(tracker).delete()
    }

    fun trackToken(tracker: Tracker) = preferenceStore.getString(
        key = "track_token_${tracker.id}",
        defaultValue = "",
    )

    fun anilistScoreType() = preferenceStore.getString(
        key = "anilist_score_type",
        defaultValue = Anilist.POINT_10,
    )

    fun autoUpdateTrack() = preferenceStore.getBoolean(
        key = "auto_update_track",
        defaultValue = true,
    )

    fun autoUpdateTrackOnMarkRead() = preferenceStore.getEnum(
        key = "auto_update_track_on_mark_read",
        defaultValue = AutoTrackState.ALWAYS,
    )

    fun isLoggedIn(tracker: Tracker): Boolean = trackToken(tracker).get().isNotEmpty() ||
        (trackUsername(tracker).get().isNotEmpty() && trackPassword(tracker).get().isNotEmpty())
}
