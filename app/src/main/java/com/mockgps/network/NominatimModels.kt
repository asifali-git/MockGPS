package com.mockgps.network

import kotlinx.serialization.Serializable

@Serializable
data class NominatimResult(
    val place_id: Long,
    val licence: String,
    val osm_type: String,
    val osm_id: Long,
    val lat: String,
    val lon: String,
    val display_name: String,
    val address: Address?,
    val boundingbox: List<String>?,
    val class: String,
    val type: String,
    val importance: Double,
    val icon: String? = null
) {
    fun toLatLng(): Pair<Double, Double> = lat.toDouble() to lon.toDouble()
}

@Serializable
data class Address(
    val house_number: String? = null,
    val road: String? = null,
    val neighbourhood: String? = null,
    val suburb: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val municipality: String? = null,
    val county: String? = null,
    val state: String? = null,
    val region: String? = null,
    val country: String? = null,
    val country_code: String? = null,
    val postcode: String? = null,
    val postal_code: String? = null,
    val district: String? = null,
    val state_district: String? = null,
    val city_district: String? = null,
    val suburb_name: String? = null,
    val hamlet: String? = null,
    val croft: String? = null,
    val isolated_dwelling: String? = null,
    val farm: String? = null,
    val allotments: String? = null
) {
    fun getCityName(): String? = city ?: town ?: village ?: municipality ?: county ?: state_district ?: district
    
    fun getSubLocalities(): List<String> {
        val list = mutableListOf<String>()
        neighbourhood?.let { list.add(it) }
        suburb?.let { list.add(it) }
        suburb_name?.let { list.add(it) }
        district?.let { list.add(it) }
        city_district?.let { list.add(it) }
        hamlet?.let { list.add(it) }
        return list.distinct()
    }
    
    fun getFullAddress(): String {
        val parts = mutableListOf<String>()
        house_number?.let { parts.add(it) }
        road?.let { parts.add(it) }
        getCityName()?.let { parts.add(it) }
        state?.let { parts.add(it) }
        country?.let { parts.add(it) }
        postcode?.let { parts.add(it) }
        return parts.joinToString(", ")
    }
}

@Serializable
data class ReverseGeocodeResult(
    val place_id: Long,
    val licence: String,
    val osm_type: String,
    val osm_id: Long,
    val lat: String,
    val lon: String,
    val display_name: String,
    val address: Address?,
    val boundingbox: List<String>?
)