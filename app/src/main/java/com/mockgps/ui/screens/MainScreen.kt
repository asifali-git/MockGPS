package com.mockgps.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberPermissionState
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mockgps.R
import com.mockgps.data.FavoriteLocation
import com.mockgps.data.MockProfile
import com.mockgps.data.SearchHistory
import com.mockgps.network.NominatimResult
import com.mockgps.repository.FavoritesRepository
import com.mockgps.repository.MockProfileRepository
import com.mockgps.repository.RouteRepository
import com.mockgps.repository.SearchHistoryRepository
import com.mockgps.service.MockLocationService
import com.mockgps.ui.components.CoordinateDisplay
import com.mockgps.ui.components.MockControls
import com.mockgps.ui.components.OsmMapView
import com.mockgps.ui.components.PerAppSpoofingList
import com.mockgps.ui.components.SearchBar
import com.mockgps.ui.components.SearchResultsList
import com.mockgps.ui.components.SearchHistoryList
import com.mockgps.ui.components.FavoritesList
import com.mockgps.ui.components.AddEditFavoriteDialog
import com.mockgps.ui.components.CoordinateInputDialog
import com.mockgps.ui.components.AdvancedSettingsDialog
import com.mockgps.util.LocationUtils
import com.mockgps.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    favoritesRepository: FavoritesRepository,
    historyRepository: SearchHistoryRepository,
    profileRepository: MockProfileRepository,
    routeRepository: RouteRepository,
    mockLocationService: MockLocationService
) {
    val navController = rememberNavController()
    var showSettings by mutableStateOf(false)
    
    NavHost(navController, "main") {
        composable("main") {
            MainScreenContent(
                favoritesRepository = favoritesRepository,
                historyRepository = historyRepository,
                profileRepository = profileRepository,
                routeRepository = routeRepository,
                mockLocationService = mockLocationService,
                onSettingsClick = { showSettings = true }
            )
        }
        composable("settings") {
            SettingsScreen(onBackPress = { showSettings = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    favoritesRepository: FavoritesRepository,
    historyRepository: SearchHistoryRepository,
    profileRepository: MockProfileRepository,
    routeRepository: RouteRepository,
    mockLocationService: MockLocationService,
    onSettingsClick: () -> Unit
) {
    // State
    var latitude by mutableStateOf(0.0)
    var longitude by mutableStateOf(0.0)
    var altitude by mutableStateOf<Double?>(null)
    var speed by mutableStateOf(0f)
    var heading by mutableStateOf(0f)
    var accuracy by mutableStateOf(0f)
    var zoom by mutableStateOf(15.0)
    var isMocking by mutableStateOf(false)
    var searchResults by mutableStateOf<List<NominatimResult>>(emptyList())
    var showSearchResults by mutableStateOf(false)
    var showCoordinateDialog by mutableStateOf(false)
    var showAdvancedSettings by mutableStateOf(false)
    var showAddFavoriteDialog by mutableStateOf(false)
    var editingFavorite: FavoriteLocation? by mutableStateOf(null)
    var selectedTab by mutableStateOf(0)
    var speedMultiplier by mutableStateOf(1f)
    var updateInterval by mutableStateOf(1000L)
    var gpsAccuracy by mutableStateOf(10f)
    var altitudeMode by mutableStateOf(0)
    var manualAltitude by mutableStateOf(0.0)
    var useFixedAltitude by mutableStateOf(false)
    var enableRouteMode by mutableStateOf(false)
    var currentAddress by mutableStateOf<String>("")
    var showFavoritesSheet by mutableStateOf(false)
    var showHistorySheet by mutableStateOf(false)
    var showPerAppSheet by mutableStateOf(false)
    var installedApps by mutableStateOf<List<com.mockgps.ui.components.AppInfo>>(emptyList())
    
    val bottomSheetState = rememberModalBottomSheetState(initialValue = false)
    val scrollState = rememberScrollState()
    
    // Permissions
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val backgroundPermissionState = rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    
    // Load installed apps
    LaunchedEffect(Unit) {
        installedApps = com.mockgps.ui.components.AppUtils.getInstalledApps(LocalContext.current.packageManager)
    }

    // Request permissions
    LaunchedEffect(locationPermissionState, backgroundPermissionState) {
        if (locationPermissionState.status.isGranted && !backgroundPermissionState.status.isGranted) {
            // Request background permission after foreground granted
        }
    }

    // Get current location
    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            getCurrentLocation()
        }
    }

    // Observe mock location service
    LaunchedEffect(Unit) {
        // TODO: Observe mock location service state
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mock GPS") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                // Map
                OsmMapView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    latitude = latitude,
                    longitude = longitude,
                    zoom = zoom,
                    onMapClick = { lat, lng ->
                        latitude = lat
                        longitude = lng
                        reverseGeocode(lat, lng)
                    },
                    onCameraChange = { lat, lng, z ->
                        latitude = lat
                        longitude = lng
                        zoom = z
                    },
                    markers = buildMapMarkers(),
                    showCurrentLocation = !isMocking,
                    followLocation = !isMocking
                )

                // Coordinate display
                CoordinateDisplay(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    latitude = latitude,
                    longitude = longitude,
                    altitude = altitude,
                    speed = speed,
                    heading = heading,
                    accuracy = accuracy,
                    isMockLocation = isMocking,
                    onCopyClick = { copyToClipboard("$latitude, $longitude") },
                    onNavigateClick = { /* Open in maps */ }
                )

                // Search bar
                SearchBar(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    query = "",
                    onQueryChange = { query ->
                        if (query.length >= 2) {
                            search(query)
                        } else {
                            searchResults = emptyList()
                            showSearchResults = false
                        }
                    },
                    onSearch = { query ->
                        search(query)
                    },
                    onClear = { searchResults = emptyList(); showSearchResults = false },
                    onVoiceInput = { /* Voice search */ },
                    onCoordinateInput = { showCoordinateDialog = true }
                )

                // Mock controls
                MockControls(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    isMocking = isMocking,
                    onStartMock = { startMocking() },
                    onStopMock = { stopMocking() },
                    speedMultiplier = speedMultiplier,
                    onSpeedChange = { speedMultiplier = it },
                    updateInterval = updateInterval,
                    onIntervalChange = { updateInterval = it },
                    gpsAccuracy = gpsAccuracy,
                    onAccuracyChange = { gpsAccuracy = it },
                    onAdvancedSettings = { showAdvancedSettings = true }
                )

                // Bottom tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 16.dp
                ) { tabs ->
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = "Search",
                        icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Search, "") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; showFavoritesSheet = true },
                        text = "Favorites",
                        icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Favorite, "") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2; showHistorySheet = true },
                        text = "History",
                        icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.History, "") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3; showPerAppSheet = true },
                        text = "Per-App",
                        icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Apps, "") }
                    )
                }

                // Content based on selected tab
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                ) {
                    when (selectedTab) {
                        0 -> SearchResultsList(
                            modifier = Modifier.fillMaxWidth(),
                            results = searchResults,
                            onResultClick = { result ->
                                latitude = result.lat.toDouble()
                                longitude = result.lon.toDouble()
                                zoom = 18.0
                                showSearchResults = false
                                reverseGeocode(latitude, longitude)
                            },
                            onFavoriteToggle = { result, add ->
                                if (add) addToFavorites(result) else removeFromFavorites(result)
                            },
                            isFavorite = { isFavorite(it) }
                        )
                        1 -> FavoritesList(
                            modifier = Modifier.fillMaxWidth(),
                            favorites = favoritesRepository.getAllFavorites().collectAsState().value ?: emptyList(),
                            onFavoriteClick = { fav ->
                                latitude = fav.latitude
                                longitude = fav.longitude
                                zoom = 18.0
                                selectedTab = 0
                            },
                            onEditClick = { fav ->
                                editingFavorite = fav
                                showAddFavoriteDialog = true
                            },
                            onDeleteClick = { fav ->
                                favoritesRepository.deleteFavorite(fav.id)
                            }
                        )
                        2 -> SearchHistoryList(
                            modifier = Modifier.fillMaxWidth(),
                            history = historyRepository.getRecentHistory().collectAsState().value ?: emptyList(),
                            onItemClick = { item ->
                                latitude = item.latitude
                                longitude = item.longitude
                                zoom = 18.0
                                selectedTab = 0
                            },
                            onDeleteClick = { id ->
                                historyRepository.deleteHistoryItem(id)
                            },
                            onClearAll = { historyRepository.clearHistory() }
                        )
                        3 -> PerAppSpoofingList(
                            modifier = Modifier.fillMaxWidth(),
                            profiles = profileRepository.getAllProfiles().collectAsState().value ?: emptyList(),
                            installedApps = installedApps,
                            onProfileClick = { profile ->
                                latitude = profile.latitude
                                longitude = profile.longitude
                                zoom = 18.0
                                selectedTab = 0
                            },
                            onProfileToggle = { profile, enabled ->
                                profileRepository.toggleProfileEnabled(profile.id, enabled)
                                if (enabled) {
                                    mockLocationService.addProfile(profile)
                                } else {
                                    mockLocationService.removeProfile(profile.packageName)
                                }
                            },
                            onAddApp = { /* Show app picker */ },
                            onEditProfile = { profile ->
                                // Navigate to edit profile screen
                            },
                            onDeleteProfile = { profile ->
                                profileRepository.deleteProfile(profile.id)
                                mockLocationService.removeProfile(profile.packageName)
                            }
                        )
                    }
                }
            }
        }

        // Search results overlay
        if (showSearchResults && searchResults.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { showSearchResults = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(16.dp)
                ) {
                    SearchResultsList(
                        modifier = Modifier.fillMaxSize(),
                        results = searchResults,
                        onResultClick = { result ->
                            latitude = result.lat.toDouble()
                            longitude = result.lon.toDouble()
                            zoom = 18.0
                            showSearchResults = false
                            reverseGeocode(latitude, longitude)
                        },
                        onFavoriteToggle = { result, add ->
                            if (add) addToFavorites(result) else removeFromFavorites(result)
                        },
                        isFavorite = { isFavorite(it) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCoordinateDialog) {
        CoordinateInputDialog(
            onDismiss = { showCoordinateDialog = false },
            onConfirm = { lat, lng ->
                latitude = lat
                longitude = lng
                zoom = 18.0
                reverseGeocode(lat, lng)
            }
        )
    }

    if (showAdvancedSettings) {
        AdvancedSettingsDialog(
            onDismiss = { showAdvancedSettings = false },
            altitudeMode = altitudeMode,
            onAltitudeModeChange = { altitudeMode = it },
            manualAltitude = manualAltitude,
            onManualAltitudeChange = { manualAltitude = it },
            useFixedAltitude = useFixedAltitude,
            onFixedAltitudeChange = { useFixedAltitude = it },
            enableRouteMode = enableRouteMode,
            onRouteModeChange = { enableRouteMode = it }
        )
    }

    if (showAddFavoriteDialog) {
        AddEditFavoriteDialog(
            onDismiss = { showAddFavoriteDialog = false; editingFavorite = null },
            onSave = { name, lat, lng, alt, addr, cat ->
                if (editingFavorite != null) {
                    favoritesRepository.updateFavorite(editingFavorite!!.copy(
                        name = name, latitude = lat, longitude = lng,
                        altitude = alt, address = addr, category = cat
                    ))
                } else {
                    favoritesRepository.addFavorite(FavoriteLocation(
                        name = name, latitude = lat, longitude = lng,
                        altitude = alt, address = addr, category = cat
                    ))
                }
                editingFavorite = null
            },
            initialName = editingFavorite?.name ?: "",
            initialLat = editingFavorite?.latitude ?: latitude,
            initialLng = editingFavorite?.longitude ?: longitude,
            initialAlt = editingFavorite?.altitude,
            initialAddress = editingFavorite?.address,
            initialCategory = editingFavorite?.category
        )
    }

    // Bottom sheets for favorites, history, per-app
    ModalBottomSheet(
        sheetState = bottomSheetState,
        sheetContent = {
            when {
                showFavoritesSheet -> FavoritesList(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    favorites = favoritesRepository.getAllFavorites().collectAsState().value ?: emptyList(),
                    onFavoriteClick = { fav ->
                        latitude = fav.latitude
                        longitude = fav.longitude
                        zoom = 18.0
                        showFavoritesSheet = false
                        bottomSheetState.hide()
                    },
                    onEditClick = { fav ->
                        editingFavorite = fav
                        showAddFavoriteDialog = true
                        showFavoritesSheet = false
                    },
                    onDeleteClick = { fav ->
                        favoritesRepository.deleteFavorite(fav.id)
                    }
                )
                showHistorySheet -> SearchHistoryList(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    history = historyRepository.getRecentHistory().collectAsState().value ?: emptyList(),
                    onItemClick = { item ->
                        latitude = item.latitude
                        longitude = item.longitude
                        zoom = 18.0
                        showHistorySheet = false
                        bottomSheetState.hide()
                    },
                    onDeleteClick = { id ->
                        historyRepository.deleteHistoryItem(id)
                    },
                    onClearAll = { historyRepository.clearHistory() }
                )
                showPerAppSheet -> PerAppSpoofingList(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    profiles = profileRepository.getAllProfiles().collectAsState().value ?: emptyList(),
                    installedApps = installedApps,
                    onProfileClick = { profile ->
                        latitude = profile.latitude
                        longitude = profile.longitude
                        zoom = 18.0
                        showPerAppSheet = false
                        bottomSheetState.hide()
                    },
                    onProfileToggle = { profile, enabled ->
                        profileRepository.toggleProfileEnabled(profile.id, enabled)
                        if (enabled) mockLocationService.addProfile(profile)
                        else mockLocationService.removeProfile(profile.packageName)
                    },
                    onAddApp = { /* Show app picker */ },
                    onEditProfile = { /* Edit profile */ },
                    onDeleteProfile = { profile ->
                        profileRepository.deleteProfile(profile.id)
                        mockLocationService.removeProfile(profile.packageName)
                    }
                )
            }
        }
    )
}

// Helper functions
private fun getCurrentLocation() {
    // TODO: Implement location fetching
}

private fun reverseGeocode(lat: Double, lng: Double) {
    // TODO: Implement reverse geocoding
}

private fun search(query: String) {
    // TODO: Implement search
}

private fun startMocking() {
    // TODO: Start mock location service
}

private fun stopMocking() {
    // TODO: Stop mock location service
}

private fun addToFavorites(result: NominatimResult) {
    // TODO: Add to favorites
}

private fun removeFromFavorites(result: NominatimResult) {
    // TODO: Remove from favorites
}

private fun isFavorite(result: NominatimResult): Boolean {
    return false // TODO: Check database
}

private fun buildMapMarkers(): List<com.mockgps.ui.components.MapMarker> {
    return emptyList() // TODO: Build markers
}

private fun copyToClipboard(text: String) {
    // TODO: Copy to clipboard
}

@Composable
fun LocalContext.current(): Context = androidx.compose.ui.platform.LocalContext.current