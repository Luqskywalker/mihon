package eu.kanade.domain.download.interactor

import eu.kanade.tachiyomi.data.download.DownloadManager
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

class DeleteDownload(
    private val sourceManager: SourceManager,
    private val downloadManager: DownloadManager,
) {

    suspend fun awaitAll(manga: Manga, vararg chapters: Chapter) = withNonCancellableContext {
        sourceManager.getOrNull(manga.source)?.let { source ->
            downloadManager.deleteChapters(chapters.toList(), manga, source)
        }
    }

    suspend fun await(chapter: Chapter, manga: Manga) = awaitAll(manga, chapter)

    suspend fun awaitAll(manga: Manga, chapters: List<Chapter>) = withNonCancellableContext {
        sourceManager.getOrNull(manga.source)?.let { source ->
            downloadManager.deleteChapters(chapters, manga, source)
        }
    }
}
