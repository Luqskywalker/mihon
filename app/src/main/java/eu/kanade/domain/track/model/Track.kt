package eu.kanade.domain.track.model

import tachiyomi.domain.track.model.Track
import eu.kanade.tachiyomi.data.database.models.Track as DbTrack

/**
 * Extension functions for converting between domain and database track models.
 */

// Constants for default values
private const val DEFAULT_TRACK_ID = -1L

fun Track.copyPersonalFrom(other: Track): Track = copy(
    lastChapterRead = other.lastChapterRead,
    score = other.score,
    status = other.status,
    startDate = other.startDate,
    finishDate = other.finishDate,
    private = other.private,
)

fun Track.toDbTrack(): DbTrack = DbTrack.create(trackerId).apply {
    id = this@toDbTrack.id
    manga_id = this@toDbTrack.mangaId
    remote_id = this@toDbTrack.remoteId
    library_id = this@toDbTrack.libraryId
    title = this@toDbTrack.title
    last_chapter_read = this@toDbTrack.lastChapterRead
    total_chapters = this@toDbTrack.totalChapters
    status = this@toDbTrack.status
    score = this@toDbTrack.score
    tracking_url = this@toDbTrack.remoteUrl
    started_reading_date = this@toDbTrack.startDate
    finished_reading_date = this@toDbTrack.finishDate
    private = this@toDbTrack.private
}

fun DbTrack.toDomainTrack(idRequired: Boolean = true): Track? {
    val trackId = id ?: if (idRequired) return null else DEFAULT_TRACK_ID
    
    return Track(
        id = trackId,
        mangaId = manga_id,
        trackerId = tracker_id,
        remoteId = remote_id,
        libraryId = library_id,
        title = title,
        lastChapterRead = last_chapter_read,
        totalChapters = total_chapters,
        status = status,
        score = score,
        remoteUrl = tracking_url,
        startDate = started_reading_date,
        finishDate = finished_reading_date,
        private = private,
    )
}

// Extension properties for validation
val Track.isValid: Boolean
    get() = id != DEFAULT_TRACK_ID && mangaId > 0 && trackerId > 0

val Track.hasProgress: Boolean
    get() = lastChapterRead > 0

val Track.isCompleted: Boolean
    get() = status == TrackStatus.COMPLETED.statusId

// Bulk conversion extensions
fun List<Track>.toDbTracks(): List<DbTrack> = map { it.toDbTrack() }

fun List<DbTrack>.toDomainTracks(idRequired: Boolean = true): List<Track> = 
    mapNotNull { it.toDomainTrack(idRequired) }

// Utility extension for creating track updates
fun Track.toUpdate(): Track = copy(
    // Add any transformation logic needed for updates
    // For example, ensure dates are within valid ranges
    startDate = startDate.coerceAtLeast(0),
    finishDate = finishDate?.coerceAtLeast(0),
)
