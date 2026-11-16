package com.leonardovictormuller.napstop.ui.home

/**
 * All user intents that can mutate [HomeUiState].
 * The UI dispatches events; the ViewModel is the sole owner of business logic.
 */
sealed interface HomeEvent {
    data class LatitudeChanged(val value: String) : HomeEvent
    data class LongitudeChanged(val value: String) : HomeEvent
    data class DestinationNameChanged(val value: String) : HomeEvent
    data class VibrationLeadTimeChanged(val minutes: Int) : HomeEvent
    data class AlarmLeadTimeChanged(val minutes: Int) : HomeEvent
    data class PresetSelected(val name: String, val lat: Double, val lon: Double) : HomeEvent
    data object StartMonitoring : HomeEvent
    data object StopMonitoring : HomeEvent
    data object DismissError : HomeEvent
}
