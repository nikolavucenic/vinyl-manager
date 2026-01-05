package org.nv.vinylmanager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.nv.vinylmanager.domain.model.Album
import org.nv.vinylmanager.domain.model.DailyPick
import org.nv.vinylmanager.domain.model.ListeningSession
import org.nv.vinylmanager.domain.model.Settings
import org.nv.vinylmanager.domain.model.Track
import org.nv.vinylmanager.domain.repository.VinylRepository
import org.nv.vinylmanager.domain.usecase.CalculateAverageRatingUseCase
import org.nv.vinylmanager.domain.usecase.DailyPickManager
import org.nv.vinylmanager.domain.usecase.SuggestedToSellUseCase
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UseCaseTests {
    @Test
    fun averageRating_usesRatedTracksOnly() {
        val useCase = CalculateAverageRatingUseCase()
        val tracks = listOf(
            Track("1", "a", "t1", 1, null, 8, null),
            Track("2", "a", "t2", 2, null, null, null),
            Track("3", "a", "t3", 3, null, 10, null)
        )
        val avg = useCase(tracks)
        assertEquals(9.0, avg)
    }

    @Test
    fun suggestedToSell_checksThresholdAndStale() {
        val useCase = SuggestedToSellUseCase()
        val settings = Settings(sellThreshold = 6.5, staleDays = 180)
        val lowRating = useCase.evaluate(6.0, null, settings)
        assertTrue(lowRating.first)
        val stale = useCase.evaluate(7.0, Instant.fromEpochMilliseconds(0), settings, now = Instant.fromEpochMilliseconds(200_000_000_000))
        assertTrue(stale.first)
    }

    @Test
    fun dailyPickManager_onePickPerDayAndSkip() = runTest {
        val repo = FakeRepository()
        val manager = DailyPickManager(repo)
        val first = manager.ensurePickForToday()
        val second = manager.ensurePickForToday()
        assertEquals(first.albumId, second.albumId)
        val skipped = manager.skipToday()
        assertTrue(skipped.skipCount >= 1)
        manager.markListenedNow()
        assertTrue(repo.picks.value.first().listened)
    }
}

private class FakeRepository : VinylRepository {
    private val albumsFlow = MutableStateFlow(listOf(
        Album("a", "Album", "Artist", null, null, null, Clock.System.now(), null, emptyList())
    ))
    val picks = MutableStateFlow<List<DailyPick>>(emptyList())
    override fun observeAlbums(): Flow<List<Album>> = albumsFlow
    override suspend fun upsertAlbum(album: Album, tracks: List<Track>) { albumsFlow.value = listOf(album.copy(tracks = tracks)) }
    override suspend fun updateTrackRating(trackId: String, rating: Int?, notes: String?) {}
    override suspend fun addListeningSession(session: ListeningSession) {}
    override suspend fun updateListeningSession(session: ListeningSession) {}
    override suspend fun searchRemote(query: String): List<Album> = emptyList()
    override suspend fun fetchRemoteAlbum(id: String): Album? = null
    override fun observeDailyPicks(): Flow<List<DailyPick>> = picks
    override suspend fun pickForDate(date: LocalDate): DailyPick {
        val pick = DailyPick(date, albumsFlow.value.first().id, 0, false, null)
        picks.value = listOf(pick)
        return pick
    }
    override suspend fun skipForDate(date: LocalDate): DailyPick {
        val pick = picks.value.first()
        val updated = pick.copy(skipCount = pick.skipCount + 1)
        picks.value = listOf(updated)
        return updated
    }
    override suspend fun markListened(date: LocalDate, listenedAt: Instant) {
        picks.value = picks.value.map { it.copy(listened = true, listenedAt = listenedAt) }
    }
    override fun observeSettings(): Flow<Settings> = MutableStateFlow(Settings())
    override suspend fun saveSettings(settings: Settings) {}
    override suspend fun clearAll() {}
}
