package com.leonardovictormuller.napstop.data.location

import com.leonardovictormuller.napstop.domain.model.GeoPoint
import com.leonardovictormuller.napstop.domain.repository.LocationTracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Simulated [LocationTracker] for development and testing.
 *
 * Emits a predefined route through São Paulo's urban transit corridor,
 * gradually moving from a start point toward the user's destination area.
 *
 * PRIVACY: This tracker never reads real GPS hardware. No real user
 * coordinates are processed or emitted. Safe for CI and demos.
 *
 * Replace with [FusedLocationTracker] in a future sprint when GPS integration is needed.
 */
class SimulatedLocationTracker : LocationTracker {

    // Simulated route: São Paulo - Av. Paulista area, moving north
    private val simulatedRoute = listOf(
        GeoPoint(latitude = -23.5614, longitude = -46.6557), // Trianon-MASP
        GeoPoint(latitude = -23.5600, longitude = -46.6549),
        GeoPoint(latitude = -23.5585, longitude = -46.6540),
        GeoPoint(latitude = -23.5570, longitude = -46.6530),
        GeoPoint(latitude = -23.5555, longitude = -46.6520),
        GeoPoint(latitude = -23.5540, longitude = -46.6510),
        GeoPoint(latitude = -23.5525, longitude = -46.6500),
        GeoPoint(latitude = -23.5510, longitude = -46.6490),
        GeoPoint(latitude = -23.5495, longitude = -46.6480),
        GeoPoint(latitude = -23.5480, longitude = -46.6470), // Near Consolação
    )

    private var routeIndex = 0

    override fun observeLocation(intervalMs: Long): Flow<GeoPoint> = flow {
        routeIndex = 0
        while (true) {
            val point = simulatedRoute[routeIndex % simulatedRoute.size]
            emit(point)
            routeIndex++
            delay(intervalMs)
        }
    }
}
