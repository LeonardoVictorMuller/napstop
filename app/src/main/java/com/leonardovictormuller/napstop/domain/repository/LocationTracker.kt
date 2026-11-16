package com.leonardovictormuller.napstop.domain.repository

import com.leonardovictormuller.napstop.domain.model.GeoPoint
import kotlinx.coroutines.flow.Flow

/**
 * Pure domain interface for observing the device's current location.
 *
 * Implementations live in the data layer and may use GPS, network,
 * or simulated sources. The domain layer depends ONLY on this interface,
 * never on Android Location or FusedLocationProviderClient directly.
 *
 * The [Flow] emits [GeoPoint] updates as they arrive. The implementor
 * is responsible for requesting permissions and handling errors upstream.
 */
interface LocationTracker {
    /**
     * Returns a cold [Flow] that starts emitting [GeoPoint] updates
     * when collected and stops when the collector is cancelled.
     *
     * @param intervalMs Desired interval between updates in milliseconds.
     */
    fun observeLocation(intervalMs: Long = 2_000L): Flow<GeoPoint>
}
