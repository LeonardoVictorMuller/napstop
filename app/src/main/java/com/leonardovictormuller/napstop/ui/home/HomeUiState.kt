package com.leonardovictormuller.napstop.ui.home

import com.leonardovictormuller.napstop.domain.model.TrackingState

/**
 * Immutable UI state for the Home screen.
 * All fields are [val] to enforce UDF (Unidirectional Data Flow).
 * The ViewModel exposes this via [StateFlow] and only updates it in response to [HomeEvent]s.
 */
data class HomeUiState(
    /** Raw text input for destination latitude */
    val latitudeInput: String = "",
    /** Raw text input for destination longitude */
    val longitudeInput: String = "",
    /** Raw text input for destination name */
    val destinationNameInput: String = "",
    /** Minutes of vibration lead time (slider value) */
    val vibrationLeadTimeMinutes: Int = 10,
    /** Minutes of alarm lead time (slider value) */
    val alarmLeadTimeMinutes: Int = 5,
    /** Current tracking lifecycle state */
    val trackingState: TrackingState = TrackingState.Idle,
    /** Validation or runtime error message to display, null if none */
    val errorMessage: String? = null,
    /** Whether the "Start Monitoring" button should be enabled */
    val isStartEnabled: Boolean = false,
)
