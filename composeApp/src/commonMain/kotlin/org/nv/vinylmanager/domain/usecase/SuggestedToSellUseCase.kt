package org.nv.vinylmanager.domain.usecase

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.nv.vinylmanager.domain.model.Settings
import org.nv.vinylmanager.domain.model.SuggestedReason

class SuggestedToSellUseCase {
    fun evaluate(
        averageRating: Double?,
        lastListenedAt: Instant?,
        settings: Settings,
        now: Instant = kotlin.time.Clock.System.now()
    ): Pair<Boolean, SuggestedReason?> {
        if (averageRating != null && averageRating <= settings.sellThreshold) {
            return true to SuggestedReason.LowRating
        }
        if (averageRating != null && averageRating <= 7.0) {
            val today: LocalDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
            val staleDate = today - DatePeriod(days = settings.staleDays)
            val lastDate = lastListenedAt?.toLocalDateTime(TimeZone.currentSystemDefault())?.date
            if (lastDate == null || lastDate <= staleDate) {
                return true to SuggestedReason.StaleListening
            }
        }
        return false to null
    }
}
