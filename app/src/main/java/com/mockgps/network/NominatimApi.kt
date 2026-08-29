package com.mockgps.network

import com.mockgps.util.Constants
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class NominatimApi private constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        .build()

    companion object {
        @Volatile private var INSTANCE: NominatimApi? = null
        fun getInstance(): NominatimApi = INSTANCE ?: synchronized(this) {
            NominatimApi().also { INSTANCE = it }
        }
    }

    suspend fun search(query: String, limit: Int = 20): Result<List<NominatimResult>> {
        val encodedQuery = query.replace(" ", "+")
        val url = "${Constants.NOMINATIM_BASE_URL}/search?q=$encodedQuery&format=json&limit=$limit&addressdetails=1&extratags=1&namedetails=1&accept-language=en"
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MockGPS/1.0 (Android)")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val results = Constants.json.decodeFromString<List<NominatimResult>>(body)
                    Result.success(results)
                } else {
                    Result.failure(Exception("Search failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): Result<NominatimResult?> {
        val url = "${Constants.NOMINATIM_BASE_URL}/reverse?lat=$lat&lon=$lon&format=json&addressdetails=1&extratags=1&namedetails=1&accept-language=en"
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MockGPS/1.0 (Android)")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val result = Constants.json.decodeFromString<NominatimResult>(body)
                    Result.success(result)
                } else {
                    Result.failure(Exception("Reverse geocode failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchByPostalCode(postalCode: String, countryCode: String = ""): Result<List<NominatimResult>> {
        val query = if (countryCode.isNotBlank()) "$postalCode, $countryCode" else postalCode
        return search(query, 10)
    }
}