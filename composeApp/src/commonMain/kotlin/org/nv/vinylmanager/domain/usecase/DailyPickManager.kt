package org.nv.vinylmanager.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.nv.vinylmanager.domain.model.DailyPick
import org.nv.vinylmanager.domain.repository.VinylRepository
import kotlin.time.Clock

class DailyPickManager(
    private val repository: VinylRepository
) {
    private fun today(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    suspend fun ensurePickForToday(): DailyPick {
        val date = today()
        val existing = repository.observeDailyPicks().first().firstOrNull { it.date == date }
        if (existing != null) return existing
        return repository.pickForDate(date)
    }

    suspend fun skipToday(): DailyPick {
        val date = today()
        return repository.skipForDate(date)
    }

    suspend fun markListenedNow() {
        repository.markListened(today(), Clock.System.now())
    }
}
