package com.leonardovictormuller.napstop.domain.model

/**
 * Sealed interface representing the current state of the monitoring lifecycle.
 * Using a sealed interface (not sealed class) to allow value class implementations
 * in the future and to maintain KMP compatibility.
 */
sealed interface TrackingState {

    /** No monitoring session is active. */
    data object Idle : TrackingState

    /**
     * Actively monitoring location. ETA is updated continuously.
     * @param etaMinutes Estimated minutes until arrival at destination.
     * @param distanceKm Remaining distance in kilometers.
     * @param speedKmh   Current speed in km/h.
     */
    data class Monitoring(
        val etaMinutes: Double,
        val distanceKm: Double,
        val speedKmh: Double,
    ) : TrackingState

    /**
     * Vibration pre-alert has been triggered.
     * @param etaMinutes Remaining ETA at the moment of trigger.
     */
    data class VibrationTriggered(val etaMinutes: Double) : TrackingState

    /**
     * Audible alarm has been triggered. User must dismiss the alarm.
     * @param etaMinutes Remaining ETA at the moment of trigger.
     */
    data class AlarmTriggered(val etaMinutes: Double) : TrackingState

    /**
     * An unrecoverable error occurred during monitoring.
     * @param message Human-readable description (must NOT contain location coordinates).
     */
    data class Error(val message: String) : TrackingState
}
