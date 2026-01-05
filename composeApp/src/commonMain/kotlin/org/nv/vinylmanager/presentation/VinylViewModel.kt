package org.nv.vinylmanager.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.nv.vinylmanager.domain.model.Album
import org.nv.vinylmanager.domain.model.DailyPick
import org.nv.vinylmanager.domain.model.Settings
import org.nv.vinylmanager.domain.repository.VinylRepository
import org.nv.vinylmanager.domain.usecase.CalculateAverageRatingUseCase
import org.nv.vinylmanager.domain.usecase.DailyPickManager
import org.nv.vinylmanager.domain.usecase.SuggestedToSellUseCase

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Ready(
        val today: DailyPick?,
        val onThisDay: List<Album>,
        val quickStats: QuickStats,
        val suggestedPreview: List<Album>
    ) : DashboardUiState
}

data class QuickStats(
    val total: Int,
    val unrated: Int,
    val suggestedToSell: Int,
    val lastListened: String?
)

sealed interface DashboardAction {
    data object Load : DashboardAction
    data object SkipToday : DashboardAction
    data object MarkListened : DashboardAction
}

sealed interface DashboardSideEffect {
    data class Error(val message: String) : DashboardSideEffect
}

sealed interface CollectionUiState {
    data object Loading : CollectionUiState
    data class Ready(val items: List<Album>) : CollectionUiState
}

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Result(val albums: List<Album>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Ready(val settings: Settings) : SettingsUiState
}

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Ready(val picks: List<DailyPick>) : HistoryUiState
}

class VinylViewModel(
    private val repository: VinylRepository,
    private val dailyPickManager: DailyPickManager,
    private val suggestedToSell: SuggestedToSellUseCase,
    private val averageRating: CalculateAverageRatingUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _dashboard = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val dashboard: StateFlow<DashboardUiState> = _dashboard

    private val _collection = MutableStateFlow<CollectionUiState>(CollectionUiState.Loading)
    val collection: StateFlow<CollectionUiState> = _collection

    private val _search = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val search: StateFlow<SearchUiState> = _search

    private val _settings = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val settings: StateFlow<SettingsUiState> = _settings

    private val _history = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val history: StateFlow<HistoryUiState> = _history

    init {
        scope.launch {
            repository.observeSettings().collect { settings ->
                _settings.value = SettingsUiState.Ready(settings)
            }
        }
        scope.launch {
            repository.observeDailyPicks().collect { picks ->
                _history.value = HistoryUiState.Ready(picks)
            }
        }
        scope.launch {
            combine(repository.observeAlbums(), repository.observeDailyPicks(), repository.observeSettings()) { albums, picks, settings ->
                Triple(albums, picks, settings)
            }.collect { (albums, picks, settings) ->
                _collection.value = CollectionUiState.Ready(albums)
                val today = today()
                val todaysPick = picks.firstOrNull { it.date == today }
                val onThisDay = albums.filter { it.releaseDate?.let { date -> date.month == today.month && date.dayOfMonth == today.dayOfMonth } == true }
                val suggested = albums.filter { album ->
                    val avg = averageRating(album.tracks)
                    suggestedToSell.evaluate(avg, album.lastListenedAt, settings).first
                }
                val stats = QuickStats(
                    total = albums.size,
                    unrated = albums.count { averageRating(it.tracks) == null },
                    suggestedToSell = suggested.size,
                    lastListened = albums.mapNotNull { it.lastListenedAt }.maxOrNull()?.toString()
                )
                _dashboard.value = DashboardUiState.Ready(todaysPick, onThisDay, stats, suggested.take(3))
            }
        }
    }

    fun dispatch(action: DashboardAction) {
        when (action) {
            DashboardAction.Load -> scope.launch { dailyPickManager.ensurePickForToday() }
            DashboardAction.MarkListened -> scope.launch { dailyPickManager.markListenedNow() }
            DashboardAction.SkipToday -> scope.launch { dailyPickManager.skipToday() }
        }
    }

    fun search(query: String) {
        _search.value = SearchUiState.Loading
        scope.launch {
            try {
                val results = repository.searchRemote(query)
                _search.value = SearchUiState.Result(results)
            } catch (t: Throwable) {
                _search.value = SearchUiState.Error(t.message ?: "Search failed")
            }
        }
    }

    fun importRemoteAlbum(id: String) {
        scope.launch {
            val remote = repository.fetchRemoteAlbum(id) ?: return@launch
            repository.upsertAlbum(remote, remote.tracks)
            _search.update { SearchUiState.Idle }
        }
    }

    fun saveSettings(settings: Settings) {
        scope.launch { repository.saveSettings(settings) }
    }

    private fun today(): LocalDate = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
