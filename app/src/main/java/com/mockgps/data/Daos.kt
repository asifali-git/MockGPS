package com.mockgps.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteLocation): Long

    @Update
    suspend fun update(favorite: FavoriteLocation)

    @Delete
    suspend fun delete(favorite: FavoriteLocation)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteLocation>>

    @Query("SELECT * FROM favorites WHERE id = :id")
    suspend fun getById(id: Long): FavoriteLocation?

    @Query("SELECT * FROM favorites WHERE latitude = :lat AND longitude = :lng LIMIT 1")
    suspend fun getByCoordinates(lat: Double, lng: Double): FavoriteLocation?
}

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SearchHistory): Long

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<SearchHistory>>

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MockProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: MockProfile): Long

    @Update
    suspend fun update(profile: MockProfile)

    @Delete
    suspend fun delete(profile: MockProfile)

    @Query("SELECT * FROM mock_profiles ORDER BY timestamp DESC")
    fun getAllProfiles(): Flow<List<MockProfile>>

    @Query("SELECT * FROM mock_profiles WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): MockProfile?

    @Query("SELECT * FROM mock_profiles WHERE isEnabled = 1")
    fun getEnabledProfiles(): Flow<List<MockProfile>>
}

@Dao
interface RouteWaypointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(waypoint: RouteWaypoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(waypoints: List<RouteWaypoint>)

    @Query("SELECT * FROM route_waypoints WHERE routeId = :routeId ORDER BY orderIndex ASC")
    fun getWaypointsForRoute(routeId: Long): Flow<List<RouteWaypoint>>

    @Query("DELETE FROM route_waypoints WHERE routeId = :routeId")
    suspend fun deleteWaypointsForRoute(routeId: Long)

    @Query("DELETE FROM route_waypoints WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SavedRouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: SavedRoute): Long

    @Update
    suspend fun update(route: SavedRoute)

    @Delete
    suspend fun delete(route: SavedRoute)

    @Query("SELECT * FROM saved_routes ORDER BY timestamp DESC")
    fun getAllRoutes(): Flow<List<SavedRoute>>

    @Query("SELECT * FROM saved_routes WHERE id = :id")
    suspend fun getById(id: Long): SavedRoute?
}