package eu.kanade.domain.track.interactor

import android.content.Context
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.domain.track.service.DelayedTrackingUpdateJob
import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack

class TrackChapter(
    private val getTracks: GetTracks,
    private val trackerManager: TrackerManager,
    private val insertTrack: InsertTrack,
    private val delayedTrackingStore: DelayedTrackingStore,
) {

    suspend fun await(
        context: Context,
        mangaId: Long,
        chapterNumber: Double,
        setupJobOnFailure: Boolean = true,
    ) = withNonCancellableContext {
        val tracks = getTracks.await(mangaId)
        if (tracks.isEmpty()) return@withNonCancellableContext

        tracks
            .mapNotNull { track -> createUpdateTask(track, chapterNumber, context, setupJobOnFailure) }
            .awaitAll()
            .forEach { it.exceptionOrNull()?.let(::logFailure) }
    }

    private fun TrackChapter.createUpdateTask(
        track: tachiyomi.domain.track.model.Track,
        chapterNumber: Double,
        context: Context,
        setupJobOnFailure: Boolean,
    ) = trackerManager.get(track.trackerId)?.takeIf { service ->
        service.isLoggedIn && chapterNumber > track.lastChapterRead
    }?.let { service ->
        async {
            runCatching {
                updateTrackerProgress(service, track, chapterNumber, context, setupJobOnFailure)
            }
        }
    }

    private suspend fun TrackChapter.updateTrackerProgress(
        service: eu.kanade.tachiyomi.data.track.Tracker,
        track: tachiyomi.domain.track.model.Track,
        chapterNumber: Double,
        context: Context,
        setupJobOnFailure: Boolean,
    ) {
        try {
            val updatedTrack = service.refresh(track.toDbTrack())
                .toDomainTrack(idRequired = true)!!
                .copy(lastChapterRead = chapterNumber)
            
            service.update(updatedTrack.toDbTrack(), true)
            insertTrack.await(updatedTrack)
            delayedTrackingStore.remove(track.id)
        } catch (e: Exception) {
            delayedTrackingStore.add(track.id, chapterNumber)
            if (setupJobOnFailure) {
                DelayedTrackingUpdateJob.setupTask(context)
            }
            throw e
        }
    }

    private fun logFailure(error: Throwable) {
        logcat(LogPriority.WARN, error) { "Failed to track chapter progress" }
    }
}
