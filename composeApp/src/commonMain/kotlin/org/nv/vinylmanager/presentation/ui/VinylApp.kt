package org.nv.vinylmanager.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.ImeAction
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

private enum class Screen(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Dashboard("Home", Icons.Filled.Home),
    Collection("Collection", Icons.Filled.History),
    Search("Search", Icons.Filled.Search),
    Settings("Settings", Icons.Filled.Settings),
    History("History", Icons.Filled.History);
}

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
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                title = {
                    Column {
                        Text(
                            text = currentScreen.label,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Vinyl Manager",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Screen.values().forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (state) {
            DashboardUiState.Loading -> ModernPlaceholder("Loading dashboard...")
            is DashboardUiState.Ready -> {
                SectionCard(title = "Today's pick") {
                    Text(
                        text = state.today?.albumId ?: "Pick pending",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    RowButtons(
                        onSkip = { onAction(DashboardAction.SkipToday) },
                        onMarkListened = { onAction(DashboardAction.MarkListened) }
                    )
                }

                SectionCard(title = "On this day") {
                    if (state.onThisDay.isEmpty()) {
                        ModernPlaceholder("No anniversaries today")
                    } else {
                        state.onThisDay.forEach { album ->
                            Text("${album.title} • ${album.year ?: "?"}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                SectionCard(title = "Quick stats") {
                    StatsRow(label = "Collection", value = state.quickStats.total.toString())
                    StatsRow(label = "Unrated", value = state.quickStats.unrated.toString())
                    StatsRow(label = "Suggest to sell", value = state.quickStats.suggestedToSell.toString())
                }

                SectionCard(title = "Suggested to sell preview") {
                    if (state.suggestedPreview.isEmpty()) {
                        ModernPlaceholder("No suggestions yet")
                    } else {
                        state.suggestedPreview.forEach { Text(it.title, style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionScreen(state: CollectionUiState, padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state) {
            CollectionUiState.Loading -> ModernPlaceholder("Loading collection...")
            is CollectionUiState.Ready -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.items) { album -> AlbumRow(album) }
            }
        }
    }
}

@Composable
private fun AlbumRow(album: Album) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(album.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(album.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            AssistChip(onClick = {}, label = { Text("Tracks: ${album.tracks.size}") })
        }
    }
}

@Composable
private fun SearchScreen(state: SearchUiState, onSearch: (String) -> Unit, onAdd: (String) -> Unit, padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        var query by rememberSaveable { mutableStateOf("") }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            placeholder = { Text("Search artists or albums") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) })
        )

        FilledTonalButton(
            onClick = { onSearch(query) },
            enabled = query.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Search MusicBrainz")
        }

        when (state) {
            SearchUiState.Idle -> ModernPlaceholder("Enter a query to start searching")
            SearchUiState.Loading -> ModernPlaceholder("Searching...")
            is SearchUiState.Error -> ModernPlaceholder("Error: ${state.message}")
            is SearchUiState.Result -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.albums) { album ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(album.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                album.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(8.dp))
                            FilledTonalButton(onClick = { onAdd(album.id) }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("Add to library", modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: SettingsUiState, onSave: (org.nv.vinylmanager.domain.model.Settings) -> Unit, padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state) {
            SettingsUiState.Loading -> ModernPlaceholder("Loading settings")
            is SettingsUiState.Ready -> {
                SectionCard(title = "Rating scale") { Text("${state.settings.ratingScale}") }
                SectionCard(title = "Sell threshold") { Text("${state.settings.sellThreshold}") }
                SectionCard(title = "Stale days") { Text("${state.settings.staleDays}") }
                FilledTonalButton(onClick = { onSave(state.settings) }, modifier = Modifier.align(Alignment.End)) {
                    Text("Save changes")
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(state: HistoryUiState, padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state) {
            HistoryUiState.Loading -> ModernPlaceholder("Loading history")
            is HistoryUiState.Ready -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.picks) { pick ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = pick.date.toString(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("Album: ${pick.albumId}")
                            Text("Skips: ${pick.skipCount}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun StatsRow(label: String, value: String) {
    RowContainer {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ModernPlaceholder(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 16.dp, horizontal = 12.dp)
    )
}

@Composable
private fun RowButtons(onSkip: () -> Unit, onMarkListened: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
            Text("Skip")
        }
        Button(onClick = onMarkListened, modifier = Modifier.weight(1f)) {
            Text("Mark listened")
        }
    }
}

@Composable
private fun RowContainer(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) { content() }
}
