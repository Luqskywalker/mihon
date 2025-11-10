package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences

class SetMigrateSorting(
    private val preferences: SourcePreferences,
) {

    suspend fun set(mode: Mode, direction: Direction) {
        preferences.migrationSortingMode().set(mode)
        preferences.migrationSortingDirection().set(direction)
    }

    suspend fun toggleDirection() {
        val current = preferences.migrationSortingDirection().get()
        val newDirection = if (current == Direction.ASCENDING) Direction.DESCENDING else Direction.ASCENDING
        preferences.migrationSortingDirection().set(newDirection)
    }

    suspend fun cycleMode() {
        val current = preferences.migrationSortingMode().get()
        val modes = Mode.entries
        val currentIndex = modes.indexOf(current)
        val nextMode = modes[(currentIndex + 1) % modes.size]
        preferences.migrationSortingMode().set(nextMode)
    }

    enum class Mode {
        ALPHABETICAL, TOTAL;

        companion object {
            val default = ALPHABETICAL
        }
    }

    enum class Direction {
        ASCENDING, DESCENDING;

        companion object {
            val default = ASCENDING
        }
    }
}
