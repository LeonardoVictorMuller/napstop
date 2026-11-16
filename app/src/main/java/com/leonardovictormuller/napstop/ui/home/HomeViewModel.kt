package com.leonardovictormuller.napstop.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leonardovictormuller.napstop.di.AppContainer
import com.leonardovictormuller.napstop.domain.alert.AlertFeedbackPlayer
import com.leonardovictormuller.napstop.domain.model.AlertConfig
import com.leonardovictormuller.napstop.domain.model.GeoPoint
import com.leonardovictormuller.napstop.domain.model.StopDestination
import com.leonardovictormuller.napstop.domain.model.TrackingState
import com.leonardovictormuller.napstop.domain.repository.LocationTracker
import com.leonardovictormuller.napstop.domain.usecase.CalculateEtaUseCase
import com.leonardovictormuller.napstop.domain.usecase.EvaluateAlertTriggerUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 *
 * - Depends ONLY on domain interfaces and pure use cases (no Android framework types stored).
 * - Exposes a single [StateFlow<HomeUiState>] consumed by the UI.
 * - Processes [HomeEvent]s to mutate state.
 * - Controls physical alerts through [AlertFeedbackPlayer].
 */
class HomeViewModel(
    private val locationTracker: LocationTracker,
    private val calculateEtaUseCase: CalculateEtaUseCase,
    private val evaluateAlertTriggerUseCase: EvaluateAlertTriggerUseCase,
    private val alertFeedbackPlayer: AlertFeedbackPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var monitoringJob: Job? = null

    // Fallback/reference speed in km/h for ETA calculations when speed is not available
    private val defaultSpeedKmh = 30.0

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LatitudeChanged -> updateInput { copy(latitudeInput = event.value) }
            is HomeEvent.LongitudeChanged -> updateInput { copy(longitudeInput = event.value) }
            is HomeEvent.DestinationNameChanged -> updateInput {
                copy(destinationNameInput = StopDestination.sanitizeName(event.value))
            }
            is HomeEvent.VibrationLeadTimeChanged -> updateInput {
                copy(vibrationLeadTimeMinutes = event.minutes)
            }
            is HomeEvent.AlarmLeadTimeChanged -> updateInput {
                copy(alarmLeadTimeMinutes = event.minutes)
            }
            is HomeEvent.PresetSelected -> _uiState.update {
                it.copy(
                    destinationNameInput = event.name,
                    latitudeInput = event.lat.toString(),
                    longitudeInput = event.lon.toString(),
                ).withStartEnabled()
            }
            is HomeEvent.StartMonitoring -> startMonitoring()
            is HomeEvent.StopMonitoring -> stopMonitoring()
            is HomeEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun updateInput(transform: HomeUiState.() -> HomeUiState) {
        _uiState.update { it.transform().withStartEnabled() }
    }

    private fun HomeUiState.withStartEnabled(): HomeUiState {
        val latOk = latitudeInput.toDoubleOrNull()?.let { it in -90.0..90.0 } == true
        val lonOk = longitudeInput.toDoubleOrNull()?.let { it in -180.0..180.0 } == true
        val nameOk = destinationNameInput.isNotBlank()
        val timesOk = vibrationLeadTimeMinutes > alarmLeadTimeMinutes
        return copy(isStartEnabled = latOk && lonOk && nameOk && timesOk)
    }

    private fun startMonitoring() {
        val state = _uiState.value
        val lat = state.latitudeInput.toDoubleOrNull()
        val lon = state.longitudeInput.toDoubleOrNull()

        if (lat == null || lon == null) {
            _uiState.update { it.copy(errorMessage = "Coordenadas inválidas. Verifique latitude e longitude.") }
            return
        }

        val destination: StopDestination = try {
            StopDestination(
                name = StopDestination.sanitizeName(state.destinationNameInput),
                location = GeoPoint(lat, lon),
            )
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message ?: "Destino inválido.") }
            return
        }

        val alertConfig: AlertConfig = try {
            AlertConfig(
                vibrationLeadTimeMinutes = state.vibrationLeadTimeMinutes,
                alarmLeadTimeMinutes = state.alarmLeadTimeMinutes,
            )
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = e.message ?: "Configuração de alerta inválida.") }
            return
        }

        stopMonitoring()

        monitoringJob = viewModelScope.launch {
            locationTracker.observeLocation(intervalMs = 3_000L)
                .catch { e ->
                    alertFeedbackPlayer.stopAll()
                    _uiState.update {
                        it.copy(trackingState = TrackingState.Error("Erro de localização: ${e.message}"))
                    }
                }
                .collect { currentLocation ->
                    val currentTrackingState = _uiState.value.trackingState

                    // Terminal check
                    if (currentTrackingState is TrackingState.AlarmTriggered ||
                        currentTrackingState is TrackingState.Error
                    ) return@collect

                    val etaResult = calculateEtaUseCase(
                        current = currentLocation,
                        destination = destination.location,
                        speedKmh = defaultSpeedKmh,
                    )

                    val baseState = if (currentTrackingState is TrackingState.Idle) {
                        TrackingState.Monitoring(
                            etaMinutes = etaResult.etaMinutes,
                            distanceKm = etaResult.distanceKm,
                            speedKmh = etaResult.effectiveSpeedKmh,
                        )
                    } else {
                        currentTrackingState
                    }

                    val nextState = evaluateAlertTriggerUseCase(
                        currentState = baseState,
                        etaMinutes = etaResult.etaMinutes,
                        config = alertConfig,
                    )

                    if (nextState is TrackingState.AlarmTriggered &&
                        currentTrackingState !is TrackingState.AlarmTriggered
                    ) {
                        alertFeedbackPlayer.triggerAlarm()
                    } else if (nextState is TrackingState.VibrationTriggered &&
                        currentTrackingState !is TrackingState.VibrationTriggered
                    ) {
                        alertFeedbackPlayer.triggerVibration()
                    }

                    _uiState.update { it.copy(trackingState = nextState) }
                }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        alertFeedbackPlayer.stopAll()
        _uiState.update { it.copy(trackingState = TrackingState.Idle) }
    }

    override fun onCleared() {
        super.onCleared()
        monitoringJob?.cancel()
        alertFeedbackPlayer.stopAll()
    }

    /** Factory for manual DI wiring via [AppContainer]. */
    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(
                    locationTracker = container.locationTracker,
                    calculateEtaUseCase = container.calculateEtaUseCase,
                    evaluateAlertTriggerUseCase = container.evaluateAlertTriggerUseCase,
                    alertFeedbackPlayer = container.alertFeedbackPlayer,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
