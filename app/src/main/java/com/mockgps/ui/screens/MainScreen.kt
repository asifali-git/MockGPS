package com.mockgps.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mockgps.data.FavoriteLocation
import com.mockgps.network.NominatimApi
import com.mockgps.network.NominatimResult
import com.mockgps.repository.FavoritesRepository
import com.mockgps.repository.MockProfileRepository
import com.mockgps.repository.RouteRepository
import com.mockgps.repository.SearchHistoryRepository
import com.mockgps.service.MockLocationService
import com.mockgps.ui.components.AddEditFavoriteDialog
import com.mockgps.ui.components.AppUtils
import com.mockgps.ui.components.CoordinateDisplay
import com.mockgps.ui.components.CoordinateInputDialog
import com.mockgps.ui.components.FavoritesList
import com.mockgps.ui.components.MapMarker
import com.mockgps.ui.components.MockControls
import com.mockgps.ui.components.OsmMapView
import com.mockgps.ui.components.PerAppSpoofingList
import com.mockgps.ui.components.SearchBar
import com.mockgps.ui.components.SearchHistoryList
import com.mockgps.ui.components.SearchResultsList
import com.mockgps.data.SearchHistory
import com.mockgps.util.LocationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    favoritesRepository: FavoritesRepository,
    historyRepository: SearchHistoryRepository,
    profileRepository: MockProfileRepository,
    routeRepository: RouteRepository
) {
    var showSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showSettings) {
        SettingsScreen(onBackPress = { showSettings = false })
        return
    }

    val scope = rememberCoroutineScope()
    val favorites by favoritesRepository.getAllFavorites().collectAsState(initial = emptyList())
    val history by historyRepository.getRecentHistory().collectAsState(initial = emptyList())
    val profiles by profileRepository.getAllProfiles().collectAsState(initial = emptyList())

    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var isMocking by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<NominatimResult>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) }
    var showCoordinateDialog by remember { mutableStateOf(false) }
    var showAddFavoriteDialog by remember { mutableStateOf(false) }
    var editingFavorite by remember { mutableStateOf<FavoriteLocation?>(null) }
    var speedMultiplier by remember { mutableStateOf(1f) }
    var updateInterval by remember { mutableStateOf(1000L) }
    var gpsAccuracy by remember { mutableStateOf(10f) }
    var altitudeMode by remember { mutableStateOf(0) }
    var manualAltitude by remember { mutableStateOf(0.0) }
    var useFixedAltitude by remember { mutableStateOf(false) }
    var enableRouteMode by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf(emptyList<com.mockgps.ui.components.AppInfo>()) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        installedApps = AppUtils.getInstalledApps(context.packageManager)
    }

    val nomApi = remember { NominatimApi.getInstance() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mock GPS") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OsmMapView(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                latitude = latitude,
                longitude = longitude,
                zoom = 15.0,
                onMapClick = { lat, lng ->
                    latitude = lat
                    longitude = lng
                },
                onCameraChange = { lat, lng, _ -> latitude = lat; longitude = lng },
                markers = if (latitude != 0.0 || longitude != 0.0) listOf(MapMarker(latitude, longitude, "Selected")) else emptyList()
            )

            CoordinateDisplay(
                modifier = Modifier.fillMaxWidth(),
                latitude = latitude,
                longitude = longitude,
                isMockLocation = isMocking,
                onCopyClick = {
                    val clip = ClipData.newPlainText("coords", "$latitude, $longitude")
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                },
                onNavigateClick = {
                    val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            )

            SearchBar(
                modifier = Modifier.fillMaxWidth(),
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { query ->
                    CoroutineScope(Dispatchers.IO).launch {
                        nomApi.search(query).onSuccess { results ->
                            searchResults = results
                        }
                    }
                },
                onClear = { searchResults = emptyList() },
                onVoiceInput = {},
                onCoordinateInput = { showCoordinateDialog = true }
            )

            MockControls(
                modifier = Modifier.fillMaxWidth(),
                isMocking = isMocking,
                onStartMock = {
                    val intent = android.content.Intent(context, MockLocationService::class.java)
                    intent.action = MockLocationService.ACTION_START_MOCKING
                    intent.putExtra("latitude", latitude)
                    intent.putExtra("longitude", longitude)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                    isMocking = true
                },
                onStopMock = {
                    val intent = android.content.Intent(context, MockLocationService::class.java)
                    intent.action = MockLocationService.ACTION_STOP_MOCKING
                    context.startService(intent)
                    isMocking = false
                },
                speedMultiplier = speedMultiplier,
                onSpeedChange = { speedMultiplier = it },
                updateInterval = updateInterval,
                onIntervalChange = { updateInterval = it },
                gpsAccuracy = gpsAccuracy,
                onAccuracyChange = { gpsAccuracy = it },
                onAdvancedSettings = {}
            )

            ScrollableTabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth(), edgePadding = 16.dp) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Search") }, icon = { Icon(Icons.Default.Search, "") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1; showSheet = true }, text = { Text("Favorites") }, icon = { Icon(Icons.Default.Favorite, "") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2; showSheet = true }, text = { Text("History") }, icon = { Icon(Icons.Default.History, "") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3; showSheet = true }, text = { Text("Per-App") }, icon = { Icon(Icons.Default.Apps, "") })
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp).verticalScroll(rememberScrollState())) {
                when (selectedTab) {
                    0 -> SearchResultsList(
                        modifier = Modifier.fillMaxWidth(),
                        results = searchResults,
                        onResultClick = { result ->
                            latitude = result.lat.toDouble()
                            longitude = result.lon.toDouble()
                            searchResults = emptyList()
                        },
                        onFavoriteToggle = { result, add ->
                            if (add) {
                                scope.launch {
                                    favoritesRepository.addFavorite(
                                        FavoriteLocation(
                                            name = result.display_name.split(",").firstOrNull() ?: result.display_name,
                                            latitude = result.lat.toDouble(),
                                            longitude = result.lon.toDouble(),
                                            address = result.address?.getFullAddress()
                                        )
                                    )
                                }
                            } else {
                                scope.launch {
                                    favorites.find { fav ->
                                        fav.latitude == result.lat.toDouble() && fav.longitude == result.lon.toDouble()
                                    }?.let { favoritesRepository.deleteFavorite(it.id) }
                                }
                            }
                        },
                        isFavorite = { result ->
                            favorites.any { it.latitude == result.lat.toDouble() && it.longitude == result.lon.toDouble() }
                        }
                    )
                    else -> Text("Select an option or tap items in bottom sheet")
                }
            }
        }
    }

    if (showCoordinateDialog) {
        CoordinateInputDialog(
            onDismiss = { showCoordinateDialog = false },
            onConfirm = { lat, lng -> latitude = lat; longitude = lng },
            initialLat = latitude,
            initialLng = longitude
        )
    }

    if (showAddFavoriteDialog) {
        AddEditFavoriteDialog(
            onDismiss = { showAddFavoriteDialog = false; editingFavorite = null },
            onSave = { name, lat, lng, alt, addr, cat ->
                scope.launch {
                    if (editingFavorite != null) {
                        favoritesRepository.updateFavorite(editingFavorite!!.copy(name = name, latitude = lat, longitude = lng, altitude = alt, address = addr, category = cat))
                    } else {
                        favoritesRepository.addFavorite(FavoriteLocation(name = name, latitude = lat, longitude = lng, altitude = alt, address = addr, category = cat))
                    }
                    editingFavorite = null
                }
            },
            initialName = editingFavorite?.name ?: "",
            initialLat = editingFavorite?.latitude ?: latitude,
            initialLng = editingFavorite?.longitude ?: longitude,
            initialAlt = editingFavorite?.altitude,
            initialAddress = editingFavorite?.address,
            initialCategory = editingFavorite?.category
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            sheetState = bottomSheetState,
            onDismissRequest = { showSheet = false }
        ) {
            when (selectedTab) {
                1 -> FavoritesList(
                    modifier = Modifier.padding(16.dp),
                    favorites = favorites,
                    onFavoriteClick = { fav -> latitude = fav.latitude; longitude = fav.longitude; showSheet = false },
                    onEditClick = { fav -> editingFavorite = fav; showAddFavoriteDialog = true; showSheet = false },
                    onDeleteClick = { fav -> scope.launch { favoritesRepository.deleteFavorite(fav.id) } }
                )
                2 -> SearchHistoryList(
                    modifier = Modifier.padding(16.dp),
                    history = history,
                    onItemClick = { item -> latitude = item.latitude; longitude = item.longitude; showSheet = false },
                    onDeleteClick = { id -> scope.launch { historyRepository.deleteHistoryItem(id) } },
                    onClearAll = { scope.launch { historyRepository.clearHistory() } }
                )
                3 -> PerAppSpoofingList(
                    modifier = Modifier.padding(16.dp),
                    profiles = profiles,
                    installedApps = installedApps,
                    onProfileClick = { profile -> latitude = profile.latitude; longitude = profile.longitude; showSheet = false },
                    onProfileToggle = { profile, enabled -> scope.launch { profileRepository.toggleProfileEnabled(profile.id, enabled) } },
                    onAddApp = {},
                    onEditProfile = {},
                    onDeleteProfile = { profile -> scope.launch { profileRepository.deleteProfile(profile.id) } }
                )
            }
        }
    }
}
