package com.leonardovictormuller.napstop.domain.usecase

import com.leonardovictormuller.napstop.domain.model.AlertConfig
import com.leonardovictormuller.napstop.domain.model.TrackingState

/**
 * Pure domain use case: evaluates whether the current ETA warrants a state transition.
 *
 * State machine:
 *   Monitoring → VibrationTriggered  (when etaMinutes ≤ vibrationLeadTimeMinutes)
 *   VibrationTriggered → AlarmTriggered (when etaMinutes ≤ alarmLeadTimeMinutes)
 *
 * States are monotonically increasing in severity; once AlarmTriggered, no regression.
 * No Android dependencies — safe for unit tests and KMP.
 */
class EvaluateAlertTriggerUseCase {

    /**
     * @param currentState  The current [TrackingState] (must be Monitoring or VibrationTriggered).
     * @param etaMinutes    The freshly computed ETA in minutes.
     * @param config        The user-configured alert thresholds.
     * @return The next [TrackingState], which may be the same or an escalated one.
     */
    operator fun invoke(
        currentState: TrackingState,
        etaMinutes: Double,
        config: AlertConfig,
    ): TrackingState = when (currentState) {
        is TrackingState.Monitoring -> {
            when {
                etaMinutes <= config.alarmLeadTimeMinutes -> TrackingState.AlarmTriggered(etaMinutes)
                etaMinutes <= config.vibrationLeadTimeMinutes -> TrackingState.VibrationTriggered(etaMinutes)
                else -> currentState.copy(etaMinutes = etaMinutes)
            }
        }
        is TrackingState.VibrationTriggered -> {
            if (etaMinutes <= config.alarmLeadTimeMinutes) {
                TrackingState.AlarmTriggered(etaMinutes)
            } else {
                currentState.copy(etaMinutes = etaMinutes)
            }
        }
        // Terminal states: Idle, AlarmTriggered, Error — no further transitions
        else -> currentState
    }
}
