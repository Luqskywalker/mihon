package eu.kanade.domain.manga.interactor

import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository

class SetMangaViewerFlags(
    private val mangaRepository: MangaRepository,
) {

    suspend fun setReadingMode(id: Long, mode: ReadingMode) = setFlag(id, mode.flags, ReadingMode.MASK)

    suspend fun setOrientation(id: Long, orientation: ReaderOrientation) = setFlag(id, orientation.flags, ReaderOrientation.MASK)

    private suspend fun setFlag(id: Long, flag: Long, mask: Long) {
        val manga = mangaRepository.getMangaById(id)
        mangaRepository.update(
            MangaUpdate(
                id = id,
                viewerFlags = manga.viewerFlags.setFlag(flag, mask),
            )
        )
    }

    private fun Long.setFlag(flag: Long, mask: Long): Long = (this and mask.inv()) or (flag and mask)
}
