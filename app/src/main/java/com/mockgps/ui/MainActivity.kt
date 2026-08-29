package com.mockgps.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.mockgps.data.AppDatabase
import com.mockgps.repository.FavoritesRepository
import com.mockgps.repository.MockProfileRepository
import com.mockgps.repository.RouteRepository
import com.mockgps.repository.SearchHistoryRepository
import com.mockgps.ui.screens.MainScreen
import com.mockgps.ui.theme.Theme

class MainActivity : ComponentActivity() {
    private val db: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    private val favoritesRepository: FavoritesRepository by lazy { FavoritesRepository(db) }
    private val historyRepository: SearchHistoryRepository by lazy { SearchHistoryRepository(db) }
    private val profileRepository: MockProfileRepository by lazy { MockProfileRepository(db) }
    private val routeRepository: RouteRepository by lazy { RouteRepository(db) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            Theme {
                MainScreen(
                    favoritesRepository = favoritesRepository,
                    historyRepository = historyRepository,
                    profileRepository = profileRepository,
                    routeRepository = routeRepository
                )
            }
        }
    }
}
