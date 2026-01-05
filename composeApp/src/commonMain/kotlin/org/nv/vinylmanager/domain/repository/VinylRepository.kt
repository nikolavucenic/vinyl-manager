package org.nv.vinylmanager.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.nv.vinylmanager.domain.model.Album
import org.nv.vinylmanager.domain.model.DailyPick
import org.nv.vinylmanager.domain.model.ListeningSession
import org.nv.vinylmanager.domain.model.Settings
import org.nv.vinylmanager.domain.model.Track

interface VinylRepository {
    fun observeAlbums(): Flow<List<Album>>
    suspend fun upsertAlbum(album: Album, tracks: List<Track>)
    suspend fun updateTrackRating(trackId: String, rating: Int?, notes: String?)
    suspend fun addListeningSession(session: ListeningSession)
    suspend fun updateListeningSession(session: ListeningSession)
    suspend fun searchRemote(query: String): List<Album>
    suspend fun fetchRemoteAlbum(id: String): Album?
    fun observeDailyPicks(): Flow<List<DailyPick>>
    suspend fun pickForDate(date: LocalDate): DailyPick
    suspend fun skipForDate(date: LocalDate): DailyPick
    suspend fun markListened(date: LocalDate, listenedAt: Instant)
    fun observeSettings(): Flow<Settings>
    suspend fun saveSettings(settings: Settings)
    suspend fun clearAll()
}
