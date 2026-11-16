package com.leonardovictormuller.napstop.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.leonardovictormuller.napstop.domain.model.GeoPoint
import com.leonardovictormuller.napstop.domain.repository.LocationTracker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Real GPS location tracker implementation using Google Play Services'
 * [FusedLocationProviderClient].
 *
 * Emits updates every 3 seconds (or custom interval) with high accuracy.
 * Uses [callbackFlow] and cleans up updates via [awaitClose] to prevent
 * memory leaks and unnecessary battery drain.
 */
class AndroidLocationTracker(
    context: Context,
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext),
) : LocationTracker {

    @SuppressLint("MissingPermission")
    override fun observeLocation(intervalMs: Long): Flow<GeoPoint> = callbackFlow {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMs,
        )
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    try {
                        trySend(
                            GeoPoint(
                                latitude = location.latitude,
                                longitude = location.longitude,
                            )
                        )
                    } catch (_: IllegalArgumentException) {
                        // Ignores any out-of-range anomalies safely
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper(),
            )
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
