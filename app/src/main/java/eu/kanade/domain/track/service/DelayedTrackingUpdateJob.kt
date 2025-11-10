package eu.kanade.domain.track.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import eu.kanade.domain.track.interactor.TrackChapter
import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.tachiyomi.util.system.workManager
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.interactor.GetTracks
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class DelayedTrackingUpdateJob(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val getTracks: GetTracks by lazy { Injekt.get() }
    private val trackChapter: TrackChapter by lazy { Injekt.get() }
    private val delayedTrackingStore: DelayedTrackingStore by lazy { Injekt.get() }

    override suspend fun doWork(): Result = when {
        runAttemptCount > MAX_RETRY_ATTEMPTS -> Result.failure()
        else -> processDelayedTrackingUpdates()
    }

    private suspend fun processDelayedTrackingUpdates(): Result = withIOContext {
        val items = delayedTrackingStore.getItems()
        if (items.isEmpty()) return@withIOContext Result.success()

        items.forEach { item ->
            processTrackingItem(item)
        }

        if (delayedTrackingStore.getItems().isEmpty()) Result.success() else Result.retry()
    }

    private suspend fun processTrackingItem(item: DelayedTrackingStore.DelayedTrackingItem) {
        val track = getTracks.awaitOne(item.trackId) ?: run {
            delayedTrackingStore.remove(item.trackId)
            return
        }

        logcat(LogPriority.DEBUG) {
            "Updating delayed track: manga=${track.mangaId}, chapter=${item.lastChapterRead}"
        }

        trackChapter.await(
            context = context,
            mangaId = track.mangaId,
            chapterNumber = item.lastChapterRead.toDouble(),
            setupJobOnFailure = false,
        )
    }

    companion object {
        private const val TAG = "DelayedTrackingUpdate"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BACKOFF_DELAY_MINUTES = 5L

        fun setupTask(context: Context) {
            val constraints = Constraints(
                requiredNetworkType = NetworkType.CONNECTED,
            )

            val request = OneTimeWorkRequestBuilder<DelayedTrackingUpdateJob>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
                .addTag(TAG)
                .build()

            context.workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
