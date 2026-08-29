package com.mockgps.repository

import com.mockgps.data.AppDatabase
import com.mockgps.data.FavoriteLocation
import com.mockgps.data.MockProfile
import com.mockgps.data.RouteWaypoint
import com.mockgps.data.SavedRoute
import com.mockgps.data.SearchHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesRepository(private val db: AppDatabase) {
    fun getAllFavorites(): Flow<List<FavoriteLocation>> = db.favoriteDao().getAllFavorites()

    suspend fun addFavorite(favorite: FavoriteLocation): Long = db.favoriteDao().insert(favorite)

    suspend fun updateFavorite(favorite: FavoriteLocation) = db.favoriteDao().update(favorite)

    suspend fun deleteFavorite(id: Long) = db.favoriteDao().deleteById(id)

    suspend fun isFavorite(lat: Double, lng: Double): Boolean = db.favoriteDao().getByCoordinates(lat, lng) != null
}

class SearchHistoryRepository(private val db: AppDatabase) {
    fun getRecentHistory(): Flow<List<SearchHistory>> = db.searchHistoryDao().getRecentHistory()

    suspend fun addHistory(history: SearchHistory) = db.searchHistoryDao().insert(history)

    suspend fun clearHistory() = db.searchHistoryDao().clearHistory()

    suspend fun deleteHistoryItem(id: Long) = db.searchHistoryDao().deleteById(id)
}

class MockProfileRepository(private val db: AppDatabase) {
    fun getAllProfiles(): Flow<List<MockProfile>> = db.mockProfileDao().getAllProfiles()

    fun getEnabledProfiles(): Flow<List<MockProfile>> = db.mockProfileDao().getEnabledProfiles()

    suspend fun getProfileForPackage(packageName: String): MockProfile? = db.mockProfileDao().getByPackageName(packageName)

    suspend fun addOrUpdateProfile(profile: MockProfile): Long = db.mockProfileDao().insert(profile)

    suspend fun updateProfile(profile: MockProfile) = db.mockProfileDao().update(profile)

    suspend fun deleteProfile(id: Long) {
        val profile = db.mockProfileDao().getAllProfiles().first().find { it.id == id }
        profile?.let { db.mockProfileDao().delete(it) }
    }

    suspend fun toggleProfileEnabled(profileId: Long, enabled: Boolean) {
        db.mockProfileDao().getAllProfiles().first().find { it.id == profileId }?.let { profile ->
            db.mockProfileDao().update(profile.copy(isEnabled = enabled))
        }
    }
}

class RouteRepository(private val db: AppDatabase) {
    fun getAllRoutes(): Flow<List<SavedRoute>> = db.savedRouteDao().getAllRoutes()

    fun getRouteWithWaypoints(routeId: Long): Flow<Pair<SavedRoute, List<RouteWaypoint>>> = 
        db.savedRouteDao().getById(routeId)?.let { route ->
            db.routeWaypointDao().getWaypointsForRoute(routeId).map { waypoints ->
                route to waypoints
            }
        } ?: emptyFlow()

    suspend fun createRoute(route: SavedRoute): Long = db.savedRouteDao().insert(route)

    suspend fun updateRoute(route: SavedRoute) = db.savedRouteDao().update(route)

    suspend fun deleteRoute(routeId: Long) {
        db.routeWaypointDao().deleteWaypointsForRoute(routeId)
        db.savedRouteDao().getById(routeId)?.let { db.savedRouteDao().delete(it) }
    }

    suspend fun saveWaypoints(routeId: Long, waypoints: List<RouteWaypoint>) {
        db.routeWaypointDao().deleteWaypointsForRoute(routeId)
        db.routeWaypointDao().insertAll(waypoints)
    }
}