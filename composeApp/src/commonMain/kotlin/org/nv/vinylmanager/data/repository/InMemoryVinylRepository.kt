package org.nv.vinylmanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.nv.vinylmanager.data.remote.MusicBrainzApi
import org.nv.vinylmanager.domain.model.Album
import org.nv.vinylmanager.domain.model.DailyPick
import org.nv.vinylmanager.domain.model.ListeningSession
import org.nv.vinylmanager.domain.model.Settings
import org.nv.vinylmanager.domain.model.Track
import org.nv.vinylmanager.domain.repository.VinylRepository
import org.nv.vinylmanager.domain.usecase.CalculateAverageRatingUseCase

class InMemoryVinylRepository(
    private val api: MusicBrainzApi,
    private val averageRating: CalculateAverageRatingUseCase
) : VinylRepository {
    private val albums = MutableStateFlow<List<Album>>(emptyList())
    private val daily = MutableStateFlow<List<DailyPick>>(emptyList())
    private val settings = MutableStateFlow(Settings())

    override fun observeAlbums(): Flow<List<Album>> = albums

    override suspend fun upsertAlbum(album: Album, tracks: List<Track>) {
        albums.value = albums.value.filterNot { it.id == album.id } + album.copy(tracks = tracks)
    }

    override suspend fun updateTrackRating(trackId: String, rating: Int?, notes: String?) {
        albums.value = albums.value.map { album ->
            album.copy(tracks = album.tracks.map { track ->
                if (track.id == trackId) track.copy(rating = rating, notes = notes) else track
            })
        }
    }

    override suspend fun addListeningSession(session: ListeningSession) {
        updateLastListened(session.albumId, session.startedAt)
    }

    override suspend fun updateListeningSession(session: ListeningSession) {
        updateLastListened(session.albumId, session.endedAt ?: session.startedAt)
    }

    private fun updateLastListened(albumId: String, instant: Instant) {
        albums.value = albums.value.map {
            if (it.id == albumId) it.copy(lastListenedAt = instant) else it
        }
    }

    override suspend fun searchRemote(query: String): List<Album> = api.search(query)

    override suspend fun fetchRemoteAlbum(id: String): Album? = api.fetchRelease(id)

    override fun observeDailyPicks(): Flow<List<DailyPick>> = daily

    override suspend fun pickForDate(date: LocalDate): DailyPick {
        val album = albums.value.randomOrNull() ?: throw IllegalStateException("No albums available")
        val pick = DailyPick(date, album.id, 0, false, null)
        daily.value = daily.value.filterNot { it.date == date } + pick
        return pick
    }

    override suspend fun skipForDate(date: LocalDate): DailyPick {
        val existing = daily.value.firstOrNull { it.date == date }
        val album = albums.value.filterNot { it.id == existing?.albumId }.randomOrNull() ?: albums.value.random()
        val pick = DailyPick(date, album.id, (existing?.skipCount ?: 0) + 1, false, null)
        daily.value = daily.value.filterNot { it.date == date } + pick
        return pick
    }

    override suspend fun markListened(date: LocalDate, listenedAt: Instant) {
        daily.value = daily.value.map {
            if (it.date == date) it.copy(listened = true, listenedAt = listenedAt) else it
        }
        daily.value.firstOrNull { it.date == date }?.let { updateLastListened(it.albumId, listenedAt) }
    }

    override fun observeSettings(): Flow<Settings> = settings

    override suspend fun saveSettings(settings: Settings) {
        this.settings.value = settings
    }

    override suspend fun clearAll() {
        albums.value = emptyList()
        daily.value = emptyList()
    }
}
