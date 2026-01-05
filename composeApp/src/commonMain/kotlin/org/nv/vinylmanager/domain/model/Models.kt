package org.nv.vinylmanager.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val year: Int?,
    val releaseDate: LocalDate?,
    val coverUrl: String?,
    val addedAt: Instant,
    val lastListenedAt: Instant?,
    val tracks: List<Track> = emptyList()
)

data class Track(
    val id: String,
    val albumId: String,
    val title: String,
    val position: Int,
    val durationSeconds: Int?,
    val rating: Int?,
    val notes: String?
)

data class ListeningSession(
    val id: String,
    val albumId: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val note: String?
)

data class DailyPick(
    val date: LocalDate,
    val albumId: String,
    val skipCount: Int,
    val listened: Boolean,
    val listenedAt: Instant?
)

data class Settings(
    val ratingScale: Int = 10,
    val sellThreshold: Double = 6.5,
    val staleDays: Int = 180
)

sealed interface SuggestedReason {
    data object LowRating : SuggestedReason
    data object StaleListening : SuggestedReason
}
