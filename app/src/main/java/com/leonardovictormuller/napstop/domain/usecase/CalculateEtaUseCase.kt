package com.leonardovictormuller.napstop.domain.usecase

import com.leonardovictormuller.napstop.domain.model.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure domain use case: calculates the Haversine distance between two [GeoPoint]s
 * and estimates the ETA in minutes based on the current speed.
 *
 * No Android dependencies — safe for unit tests and KMP.
 */
class CalculateEtaUseCase {

    companion object {
        const val EARTH_RADIUS_KM = 6_371.0
        const val FALLBACK_SPEED_KMH = 25.0  // Safe fallback when speed ≤ 0 (stopped/traffic)
        private const val MIN_SPEED_THRESHOLD_KMH = 0.5
    }

    /**
     * @param current      Current device location.
     * @param destination  Target stop location.
     * @param speedKmh     Current speed reported by GPS (may be 0 when stopped).
     * @return [EtaResult] with distance and ETA.
     */
    operator fun invoke(
        current: GeoPoint,
        destination: GeoPoint,
        speedKmh: Double,
    ): EtaResult {
        val distanceKm = haversineDistanceKm(current, destination)
        val effectiveSpeed = if (speedKmh < MIN_SPEED_THRESHOLD_KMH) FALLBACK_SPEED_KMH else speedKmh
        val etaMinutes = (distanceKm / effectiveSpeed) * 60.0
        return EtaResult(
            distanceKm = distanceKm,
            etaMinutes = etaMinutes,
            effectiveSpeedKmh = effectiveSpeed,
        )
    }

    /**
     * Haversine formula: calculates great-circle distance between two points on Earth.
     */
    fun haversineDistanceKm(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)

        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * atan2(sqrt(h), sqrt(1 - h))
    }

    data class EtaResult(
        val distanceKm: Double,
        val etaMinutes: Double,
        val effectiveSpeedKmh: Double,
    )
}
