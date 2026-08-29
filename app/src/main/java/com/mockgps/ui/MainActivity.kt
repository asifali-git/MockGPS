package com.mockgps.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.Composable
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mockgps.R
import com.mockgps.data.AppDatabase
import com.mockgps.repository.FavoritesRepository
import com.mockgps.repository.MockProfileRepository
import com.mockgps.repository.RouteRepository
import com.mockgps.repository.SearchHistoryRepository
import com.mockgps.service.MockLocationService
import com.mockgps.ui.screens.MainScreen
import com.mockgps.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val db: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    private val favoritesRepository: FavoritesRepository by lazy { FavoritesRepository(db) }
    private val historyRepository: SearchHistoryRepository by lazy { SearchHistoryRepository(db) }
    private val profileRepository: MockProfileRepository by lazy { MockProfileRepository(db) }
    private val routeRepository: RouteRepository by lazy { RouteRepository(db) }
    private val mockLocationService: MockLocationService by lazy { MockLocationService() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            com.mockgps.ui.theme.Theme.MockGPS {
                MainScreen(
                    favoritesRepository = favoritesRepository,
                    historyRepository = historyRepository,
                    profileRepository = profileRepository,
                    routeRepository = routeRepository,
                    mockLocationService = mockLocationService
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop mock location service
    }
}