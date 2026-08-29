package com.mockgps.util

import kotlinx.serialization.json.Json

object Constants {
    const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org"
    const val OSM_TILE_URL = "https://tile.openstreetmap.org"
    const val DEFAULT_LATITUDE = 0.0
    const val DEFAULT_LONGITUDE = 0.0
    const val DEFAULT_ZOOM = 15.0
    const val MIN_ZOOM = 2.0
    const val MAX_ZOOM = 19.0
    const val MOCK_LOCATION_UPDATE_INTERVAL_MS = 1000L
    const val MAX_SEARCH_HISTORY = 50
    const val FAVORITE_CATEGORIES = "Home,Work,Favorite,Travel,Testing"
    
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowSpecialFloatingPointValues = true
    }
}