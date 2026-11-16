package com.leonardovictormuller.napstop

import com.leonardovictormuller.napstop.domain.model.GeoPoint
import com.leonardovictormuller.napstop.domain.model.TrackingState
import com.leonardovictormuller.napstop.domain.repository.LocationTracker
import com.leonardovictormuller.napstop.domain.usecase.CalculateEtaUseCase
import com.leonardovictormuller.napstop.domain.usecase.EvaluateAlertTriggerUseCase
import com.leonardovictormuller.napstop.ui.home.HomeEvent
import com.leonardovictormuller.napstop.ui.home.HomeViewModel
import io.mockk.every
import io.mockk.mockk
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
        // The fixed location is very close to the destination, so it could be any alert state
        assertTrue(
            "Expected active tracking state, got $state",
            state is TrackingState.Monitoring ||
                state is TrackingState.VibrationTriggered ||
                state is TrackingState.AlarmTriggered,
        )
    }

    @Test
    fun `stop monitoring returns to Idle state`() = runTest {
        val fixedLocation = GeoPoint(-23.5614, -46.6557)
        every { locationTracker.observeLocation(any()) } returns flowOf(fixedLocation)

        viewModel.onEvent(HomeEvent.PresetSelected("Test", -23.5000, -46.6000))
        viewModel.onEvent(HomeEvent.StartMonitoring)
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.StopMonitoring)

        assertEquals(TrackingState.Idle, viewModel.uiState.value.trackingState)
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
