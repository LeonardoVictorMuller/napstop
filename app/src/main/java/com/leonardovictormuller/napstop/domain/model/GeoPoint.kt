package com.leonardovictormuller.napstop.domain.model

/**
 * Represents a geographic coordinate.
 * Validation is enforced in the init block (defensive programming).
 * This is a pure Kotlin data class with no Android dependencies,
 * facilitating future Kotlin Multiplatform (KMP) migration.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in -90.0..90.0) {
            "Invalid latitude: $latitude. Must be in range [-90, 90]."
        }
        require(longitude in -180.0..180.0) {
            "Invalid longitude: $longitude. Must be in range [-180, 180]."
        }
    }
}
