package com.mockgps.util

import android.location.Location

object LocationUtils {
    fun formatCoordinate(coord: Double, isLatitude: Boolean): String {
        val direction = when {
            isLatitude && coord >= 0 -> "N"
            isLatitude -> "S"
            !isLatitude && coord >= 0 -> "E"
            else -> "W"
        }
        val absCoord = kotlin.math.abs(coord)
        val degrees = absCoord.toInt()
        val minutes = ((absCoord - degrees) * 60).toInt()
        val seconds = ((absCoord - degrees - minutes / 60.0) * 3600)
        return String.format("%d°%02d'%05.2f\"%s", degrees, minutes, seconds, direction)
    }

    fun formatDecimalCoordinate(coord: Double, isLatitude: Boolean, decimals: Int = 6): String {
        val format = "%.${decimals}f"
        return String.format(format, coord)
    }

    fun parseCoordinate(input: String): Pair<Double, Double>? {
        try {
            val parts = input.split(",").map { it.trim() }
            if (parts.size == 2) {
                val lat = parts[0].toDouble()
                val lng = parts[1].toDouble()
                if (lat in -90.0..90.0 && lng in -180.0..180.0) {
                    return lat to lng
                }
            }
        } catch (e: Exception) {
            // Invalid format
        }
        return null
    }

    fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun bearingBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        // Calculate bearing
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        
        val y = Math.sin(dLon) * Math.cos(lat2Rad)
        val x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon)
        val bearing = Math.toDegrees(Math.atan2(y, x))
        return (bearing + 360) % 360
    }

    fun interpolateLocation(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double,
        fraction: Double
    ): Pair<Double, Double> {
        val lat = lat1 + (lat2 - lat1) * fraction
        val lon = lon1 + (lon2 - lon1) * fraction
        return lat to lon
    }

    fun isValidCoordinate(lat: Double, lng: Double): Boolean {
        return lat in -90.0..90.0 && lng in -180.0..180.0
    }
}