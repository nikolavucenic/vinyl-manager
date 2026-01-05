package org.nv.vinylmanager.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.nv.vinylmanager.di.appModule
import org.nv.vinylmanager.domain.model.Album
import org.nv.vinylmanager.presentation.CollectionUiState
import org.nv.vinylmanager.presentation.DashboardAction
import org.nv.vinylmanager.presentation.DashboardUiState
import org.nv.vinylmanager.presentation.HistoryUiState
import org.nv.vinylmanager.presentation.SearchUiState
import org.nv.vinylmanager.presentation.SettingsUiState
import org.nv.vinylmanager.presentation.VinylViewModel

@Composable
fun VinylApp() {
    KoinApplication(application = {
        modules(appModule())
    }) {
        val viewModel = koinInject<VinylViewModel>()
        VinylScaffold(viewModel)
    }
}

private enum class Screen { Dashboard, Collection, Search, Settings, History }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VinylScaffold(viewModel: VinylViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
    val dashboard by viewModel.dashboard.collectAsState()
    val collection by viewModel.collection.collectAsState()
    val search by viewModel.search.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val history by viewModel.history.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.values().forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = {
                            val icon = when (screen) {
                                Screen.Dashboard -> Icons.Default.Home
                                Screen.Collection -> Icons.Default.History
                                Screen.Search -> Icons.Default.Search
                                Screen.Settings -> Icons.Default.Settings
                                Screen.History -> Icons.Default.History
                            }
                            Icon(icon, contentDescription = screen.name)
                        },
                        label = { Text(screen.name) }
                    )
                }
            }
        }
    ) { padding ->
        when (currentScreen) {
            Screen.Dashboard -> DashboardScreen(dashboard, onAction = viewModel::dispatch, padding)
            Screen.Collection -> CollectionScreen(collection, padding)
            Screen.Search -> SearchScreen(search, onSearch = viewModel::search, onAdd = viewModel::importRemoteAlbum, padding)
            Screen.Settings -> SettingsScreen(settings, onSave = viewModel::saveSettings, padding)
            Screen.History -> HistoryScreen(history, padding)
        }
    }
}

@Composable
private fun DashboardScreen(state: DashboardUiState, onAction: (DashboardAction) -> Unit, padding: PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        when (state) {
            DashboardUiState.Loading -> Text("Loading dashboard...")
            is DashboardUiState.Ready -> {
                Text("Today's vinyl")
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(state.today?.albumId ?: "Pick pending")
                        Button(onClick = { onAction(DashboardAction.SkipToday) }) { Text("Skip") }
                        Button(onClick = { onAction(DashboardAction.MarkListened) }) { Text("Mark listened") }
                    }
                }
                Text("On this day")
                state.onThisDay.forEach { album -> Text("${album.title} - ${album.year ?: "?"}") }
                Text("Quick stats: total ${state.quickStats.total}, unrated ${state.quickStats.unrated}, suggested ${state.quickStats.suggestedToSell}")
                Text("Suggested to sell preview")
                state.suggestedPreview.forEach { Text(it.title) }
            }
        }
    }
}

@Composable
private fun CollectionScreen(state: CollectionUiState, padding: PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        when (state) {
            CollectionUiState.Loading -> Text("Loading collection...")
            is CollectionUiState.Ready -> LazyColumn { items(state.items) { album -> AlbumRow(album) } }
        }
    }
}

@Composable
private fun AlbumRow(album: Album) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(album.title, style = MaterialTheme.typography.titleMedium)
            Text(album.artist, style = MaterialTheme.typography.bodyMedium)
            Text("Tracks: ${album.tracks.size}")
        }
    }
}

@Composable
private fun SearchScreen(state: SearchUiState, onSearch: (String) -> Unit, onAdd: (String) -> Unit, padding: PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        var query by remember { mutableStateOf("") }
        Button(onClick = { onSearch(query) }) { Text("Search MusicBrainz") }
        when (state) {
            SearchUiState.Idle -> Text("Enter a query")
            SearchUiState.Loading -> Text("Searching...")
            is SearchUiState.Error -> Text("Error: ${state.message}")
            is SearchUiState.Result -> LazyColumn {
                items(state.albums) { album ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(album.title)
                            Text(album.artist)
                            Button(onClick = { onAdd(album.id) }) { Text("Add") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: SettingsUiState, onSave: (org.nv.vinylmanager.domain.model.Settings) -> Unit, padding: PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        when (state) {
            SettingsUiState.Loading -> Text("Loading settings")
            is SettingsUiState.Ready -> {
                Text("Rating scale: ${state.settings.ratingScale}")
                Text("Sell threshold: ${state.settings.sellThreshold}")
                Text("Stale days: ${state.settings.staleDays}")
                Button(onClick = { onSave(state.settings) }) { Text("Save") }
            }
        }
    }
}

@Composable
private fun HistoryScreen(state: HistoryUiState, padding: PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        when (state) {
            HistoryUiState.Loading -> Text("Loading history")
            is HistoryUiState.Ready -> LazyColumn { items(state.picks) { pick -> Text("${pick.date}: ${pick.albumId} skips=${pick.skipCount}") } }
        }
    }
}
