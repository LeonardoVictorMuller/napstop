package com.leonardovictormuller.napstop

import com.leonardovictormuller.napstop.domain.alert.AlertFeedbackPlayer
import com.leonardovictormuller.napstop.domain.model.GeoPoint
import com.leonardovictormuller.napstop.domain.model.TrackingState
import com.leonardovictormuller.napstop.domain.repository.LocationTracker
import com.leonardovictormuller.napstop.domain.usecase.CalculateEtaUseCase
import com.leonardovictormuller.napstop.domain.usecase.EvaluateAlertTriggerUseCase
import com.leonardovictormuller.napstop.ui.home.HomeEvent
import com.leonardovictormuller.napstop.ui.home.HomeViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val locationTracker = mockk<LocationTracker>()
    private val alertFeedbackPlayer = mockk<AlertFeedbackPlayer>(relaxed = true)
    private val calculateEtaUseCase = CalculateEtaUseCase()
    private val evaluateAlertTriggerUseCase = EvaluateAlertTriggerUseCase()

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeViewModel(
            locationTracker = locationTracker,
            calculateEtaUseCase = calculateEtaUseCase,
            evaluateAlertTriggerUseCase = evaluateAlertTriggerUseCase,
            alertFeedbackPlayer = alertFeedbackPlayer,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is idle with empty inputs`() {
        val state = viewModel.uiState.value
        assertEquals(TrackingState.Idle, state.trackingState)
        assertEquals("", state.latitudeInput)
        assertEquals("", state.longitudeInput)
        assertTrue(!state.isStartEnabled)
    }

    @Test
    fun `latitude change updates state`() {
        viewModel.onEvent(HomeEvent.LatitudeChanged("-23.5"))
        assertEquals("-23.5", viewModel.uiState.value.latitudeInput)
    }

    @Test
    fun `longitude change updates state`() {
        viewModel.onEvent(HomeEvent.LongitudeChanged("-46.6"))
        assertEquals("-46.6", viewModel.uiState.value.longitudeInput)
    }

    @Test
    fun `preset selection fills all inputs and enables start`() {
        viewModel.onEvent(HomeEvent.PresetSelected("Test", -23.5, -46.6))
        val state = viewModel.uiState.value
        assertEquals("Test", state.destinationNameInput)
        assertEquals("-23.5", state.latitudeInput)
        assertEquals("-46.6", state.longitudeInput)
        assertTrue(state.isStartEnabled)
    }

    @Test
    fun `start is disabled when vibration lead time equals alarm lead time`() {
        viewModel.onEvent(HomeEvent.PresetSelected("Test", -23.5, -46.6))
        viewModel.onEvent(HomeEvent.VibrationLeadTimeChanged(5))
        viewModel.onEvent(HomeEvent.AlarmLeadTimeChanged(5))
        assertTrue(!viewModel.uiState.value.isStartEnabled)
    }

    @Test
    fun `start monitoring transitions to Monitoring state`() = runTest {
        val fixedLocation = GeoPoint(-23.5614, -46.6557)
        every { locationTracker.observeLocation(any()) } returns flowOf(fixedLocation)

        viewModel.onEvent(HomeEvent.LatitudeChanged("-23.5000"))
        viewModel.onEvent(HomeEvent.LongitudeChanged("-46.6000"))
        viewModel.onEvent(HomeEvent.DestinationNameChanged("Destination"))
        viewModel.onEvent(HomeEvent.VibrationLeadTimeChanged(10))
        viewModel.onEvent(HomeEvent.AlarmLeadTimeChanged(5))
        viewModel.onEvent(HomeEvent.StartMonitoring)

        advanceUntilIdle()

        val state = viewModel.uiState.value.trackingState
        assertTrue(
            "Expected active tracking state, got $state",
            state is TrackingState.Monitoring ||
                state is TrackingState.VibrationTriggered ||
                state is TrackingState.AlarmTriggered,
        )
    }

    @Test
    fun `stop monitoring stops feedback and returns to Idle state`() = runTest {
        val fixedLocation = GeoPoint(-23.5614, -46.6557)
        every { locationTracker.observeLocation(any()) } returns flowOf(fixedLocation)

        viewModel.onEvent(HomeEvent.PresetSelected("Test", -23.5000, -46.6000))
        viewModel.onEvent(HomeEvent.StartMonitoring)
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.StopMonitoring)

        assertEquals(TrackingState.Idle, viewModel.uiState.value.trackingState)
        verify(atLeast = 1) { alertFeedbackPlayer.stopAll() }
    }

    @Test
    fun `vibration alert is triggered when approaching destination within vibration lead time`() = runTest {
        // Destination at (0.0, 0.0). Vibration lead time = 10 min.
        // At 30 km/h, 10 min corresponds to 5.0 km distance.
        // Point around 4 km away (~0.036 degrees lat) gives ETA ~8 min -> vibration trigger
        val vibrationRangeLocation = GeoPoint(0.036, 0.0)
        every { locationTracker.observeLocation(any()) } returns flowOf(vibrationRangeLocation)

        viewModel.onEvent(HomeEvent.LatitudeChanged("0.0"))
        viewModel.onEvent(HomeEvent.LongitudeChanged("0.0"))
        viewModel.onEvent(HomeEvent.DestinationNameChanged("Origin"))
        viewModel.onEvent(HomeEvent.VibrationLeadTimeChanged(10))
        viewModel.onEvent(HomeEvent.AlarmLeadTimeChanged(5))
        viewModel.onEvent(HomeEvent.StartMonitoring)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.trackingState is TrackingState.VibrationTriggered)
        verify(exactly = 1) { alertFeedbackPlayer.triggerVibration() }
    }

    @Test
    fun `alarm is triggered when approaching destination within alarm lead time`() = runTest {
        // Destination at (0.0, 0.0). Alarm lead time = 5 min.
        // At 30 km/h, 5 min corresponds to 2.5 km.
        // Point around 1 km away (~0.009 degrees lat) gives ETA ~2 min -> alarm trigger
        val alarmRangeLocation = GeoPoint(0.009, 0.0)
        every { locationTracker.observeLocation(any()) } returns flowOf(alarmRangeLocation)

        viewModel.onEvent(HomeEvent.LatitudeChanged("0.0"))
        viewModel.onEvent(HomeEvent.LongitudeChanged("0.0"))
        viewModel.onEvent(HomeEvent.DestinationNameChanged("Origin"))
        viewModel.onEvent(HomeEvent.VibrationLeadTimeChanged(10))
        viewModel.onEvent(HomeEvent.AlarmLeadTimeChanged(5))
        viewModel.onEvent(HomeEvent.StartMonitoring)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.trackingState is TrackingState.AlarmTriggered)
        verify(exactly = 1) { alertFeedbackPlayer.triggerAlarm() }
    }

    @Test
    fun `invalid coordinates show error message`() = runTest {
        viewModel.onEvent(HomeEvent.LatitudeChanged("INVALID"))
        viewModel.onEvent(HomeEvent.LongitudeChanged("-46.6"))
        viewModel.onEvent(HomeEvent.DestinationNameChanged("Test"))
        viewModel.onEvent(HomeEvent.StartMonitoring)

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `dismiss error clears error message`() = runTest {
        viewModel.onEvent(HomeEvent.LatitudeChanged("INVALID"))
        viewModel.onEvent(HomeEvent.StartMonitoring)
        viewModel.onEvent(HomeEvent.DismissError)

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `destination name is sanitized on input`() {
        viewModel.onEvent(HomeEvent.DestinationNameChanged("  Test\u0001Name  "))
        // Control char removed, whitespace trimmed
        assertEquals("TestName", viewModel.uiState.value.destinationNameInput)
    }
}
