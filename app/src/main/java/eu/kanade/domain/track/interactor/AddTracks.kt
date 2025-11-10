package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.lang.convertEpochMillisZone
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.interactor.InsertTrack
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.ZoneOffset

class AddTracks(
    private val insertTrack: InsertTrack,
    private val syncChapterProgressWithTrack: SyncChapterProgressWithTrack,
    private val getChaptersByMangaId: GetChaptersByMangaId,
    private val trackerManager: TrackerManager,
) {

    suspend fun bind(tracker: Tracker, item: Track, mangaId: Long) = withNonCancellableContext {
        withIOContext {
            val track = performBinding(tracker, item, mangaId)
            syncChapterProgressWithTrack.await(mangaId, track, tracker)
        }
    }

    suspend fun bindEnhancedTrackers(manga: Manga, source: Source) = withNonCancellableContext {
        withIOContext {
            trackerManager.loggedInTrackers()
                .filterIsInstance<EnhancedTracker>()
                .filter { it.accept(source) }
                .forEach { service ->
                    try {
                        service.match(manga)?.let { track ->
                            track.manga_id = manga.id
                            (service as Tracker).bind(track)
                            val domainTrack = track.toDomainTrack(idRequired = false)!!
                            insertTrack.await(domainTrack)
                            syncChapterProgressWithTrack.await(manga.id, domainTrack, service)
                        }
                    } catch (e: Exception) {
                        logcat(LogPriority.WARN, e) { 
                            "Could not match manga: ${manga.title} with service $service" 
                        }
                    }
                }
        }
    }

    private suspend fun performBinding(tracker: Tracker, item: Track, mangaId: Long): tachiyomi.domain.track.model.Track {
        val allChapters = getChaptersByMangaId.await(mangaId)
        val hasReadChapters = allChapters.any { it.read }
        
        tracker.bind(item, hasReadChapters)

        var track = item.toDomainTrack(idRequired = false) ?: 
            throw IllegalStateException("Failed to convert track to domain model")

        insertTrack.await(track)

        if (hasReadChapters) {
            track = updateTrackProgress(track, allChapters, tracker)
            track = updateTrackStartDate(track, mangaId, tracker)
        }

        return track
    }

    private suspend fun updateTrackProgress(
        track: tachiyomi.domain.track.model.Track,
        chapters: List<tachiyomi.domain.chapter.model.Chapter>,
        tracker: Tracker,
    ): tachiyomi.domain.track.model.Track {
        val latestLocalReadChapterNumber = chapters
            .sortedBy { it.chapterNumber }
            .takeWhile { it.read }
            .lastOrNull()
            ?.chapterNumber ?: -1.0

        return if (latestLocalReadChapterNumber > track.lastChapterRead) {
            val updatedTrack = track.copy(lastChapterRead = latestLocalReadChapterNumber)
            tracker.setRemoteLastChapterRead(updatedTrack.toDbTrack(), latestLocalReadChapterNumber.toInt())
            updatedTrack
        } else {
            track
        }
    }

    private suspend fun updateTrackStartDate(
        track: tachiyomi.domain.track.model.Track,
        mangaId: Long,
        tracker: Tracker,
    ): tachiyomi.domain.track.model.Track {
        if (track.startDate > 0) return track

        val firstReadChapterDate = Injekt.get<GetHistory>().await(mangaId)
            .minByOrNull { it.readAt }
            ?.readAt

        return firstReadChapterDate?.let { readAt ->
            val startDate = readAt.time.convertEpochMillisZone(
                ZoneOffset.systemDefault(),
                ZoneOffset.UTC,
            )
            val updatedTrack = track.copy(startDate = startDate)
            tracker.setRemoteStartDate(updatedTrack.toDbTrack(), startDate)
            updatedTrack
        } ?: track
    }
}
