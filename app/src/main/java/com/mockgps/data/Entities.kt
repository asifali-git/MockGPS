package com.mockgps.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "favorites")
@Serializable
data class FavoriteLocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val address: String? = null,
    val category: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
@Serializable
data class SearchHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "mock_profiles")
@Serializable
data class MockProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val packageName: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val speed: Float = 0f,
    val heading: Float = 0f,
    val accuracy: Float = 10f,
    val updateInterval: Long = 1000,
    val isEnabled: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "route_waypoints")
@Serializable
data class RouteWaypoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val orderIndex: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_routes")
@Serializable
data class SavedRoute(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val speedMultiplier: Float = 1f,
    val loopRoute: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)