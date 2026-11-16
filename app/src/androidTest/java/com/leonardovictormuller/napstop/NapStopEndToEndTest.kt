package com.leonardovictormuller.napstop

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.leonardovictormuller.napstop.data.location.SimulatedLocationTracker
import com.leonardovictormuller.napstop.domain.alert.AlertFeedbackPlayer
import com.leonardovictormuller.napstop.domain.model.GeoPoint
import com.leonardovictormuller.napstop.domain.model.TrackingState
import com.leonardovictormuller.napstop.domain.repository.LocationTracker
import com.leonardovictormuller.napstop.domain.usecase.CalculateEtaUseCase
import com.leonardovictormuller.napstop.domain.usecase.EvaluateAlertTriggerUseCase
import com.leonardovictormuller.napstop.ui.home.HomeEvent
import com.leonardovictormuller.napstop.ui.home.HomeScreen
import com.leonardovictormuller.napstop.ui.home.HomeTestTags
import com.leonardovictormuller.napstop.ui.home.HomeViewModel
import com.leonardovictormuller.napstop.ui.theme.NapStopTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Controllable LocationTracker implementation for testing GPS progress.
 */
class ControllableLocationTracker : LocationTracker {
    val locationFlow = MutableSharedFlow<GeoPoint>(replay = 1)
    override fun observeLocation(intervalMs: Long): Flow<GeoPoint> = locationFlow.asSharedFlow()
}

/**
 * Fake in-memory AlertFeedbackPlayer recording trigger calls.
 */
class FakeAlertFeedbackPlayer : AlertFeedbackPlayer {
    var vibrationTriggerCount = 0
    var alarmTriggerCount = 0
    var stopCount = 0

    override fun triggerVibration() {
        vibrationTriggerCount++
    }

    override fun triggerAlarm() {
        alarmTriggerCount++
    }

    override fun stopAll() {
        stopCount++
    }
}

/**
 * End-to-end UI tests simulating user interactions, GPS approaches, and alerts.
 */
@RunWith(AndroidJUnit4::class)
class NapStopEndToEndTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun launchHomeScreen(
        locationTracker: LocationTracker = SimulatedLocationTracker(),
        alertFeedbackPlayer: AlertFeedbackPlayer = FakeAlertFeedbackPlayer(),
    ): HomeViewModel {
        val viewModel = HomeViewModel(
            locationTracker = locationTracker,
            calculateEtaUseCase = CalculateEtaUseCase(),
            evaluateAlertTriggerUseCase = EvaluateAlertTriggerUseCase(),
            alertFeedbackPlayer = alertFeedbackPlayer,
        )
        composeRule.setContent {
            NapStopTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
        return viewModel
    }

    @Test
    fun screen_displaysAllInputElements() {
        launchHomeScreen()
        composeRule.onNodeWithTag(HomeTestTags.SCREEN_ROOT).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.DESTINATION_NAME_INPUT).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.LATITUDE_INPUT).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.LONGITUDE_INPUT).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.VIBRATION_SLIDER).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.ALARM_SLIDER).assertIsDisplayed()
    }

    @Test
    fun startButton_isDisabledInitially() {
        launchHomeScreen()
        composeRule.onNodeWithTag(HomeTestTags.START_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun presetSelection_fillsInputsAndEnablesStart() {
        launchHomeScreen()

        composeRule
            .onNodeWithTag("${HomeTestTags.PRESET_PREFIX}Estação Paulista")
            .performClick()

        composeRule.onNodeWithTag(HomeTestTags.DESTINATION_NAME_INPUT)
            .assertTextContains("Estação Paulista")

        composeRule.onNodeWithTag(HomeTestTags.START_BUTTON).assertIsEnabled()
    }

    @Test
    fun stopMonitoring_showsStartButtonAgain() {
        val viewModel = launchHomeScreen()

        composeRule.onNodeWithTag(HomeTestTags.DESTINATION_NAME_INPUT)
            .performTextInput("Rio de Janeiro")
        composeRule.onNodeWithTag(HomeTestTags.LATITUDE_INPUT)
            .performTextInput("-22.9068")
        composeRule.onNodeWithTag(HomeTestTags.LONGITUDE_INPUT)
            .performTextInput("-43.1729")

        // Directly trigger start to bypass system permission popup in instrumentation
        viewModel.onEvent(HomeEvent.StartMonitoring)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(HomeTestTags.STOP_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.STOP_BUTTON).performClick()

        composeRule.onNodeWithTag(HomeTestTags.START_BUTTON).assertIsDisplayed()
    }

    @Test
    fun fullApproachJourney_progressesFromFarToVibrationAndAlarm() {
        val controllableTracker = ControllableLocationTracker()
        val fakeFeedbackPlayer = FakeAlertFeedbackPlayer()

        val viewModel = launchHomeScreen(
            locationTracker = controllableTracker,
            alertFeedbackPlayer = fakeFeedbackPlayer,
        )

        // 1. Configure destination at (0.0, 0.0) with 10 min vibration and 5 min alarm
        viewModel.onEvent(HomeEvent.DestinationNameChanged("Ponto Central"))
        viewModel.onEvent(HomeEvent.LatitudeChanged("0.0"))
        viewModel.onEvent(HomeEvent.LongitudeChanged("0.0"))
        viewModel.onEvent(HomeEvent.VibrationLeadTimeChanged(10))
        viewModel.onEvent(HomeEvent.AlarmLeadTimeChanged(5))

        // 2. Start monitoring
        viewModel.onEvent(HomeEvent.StartMonitoring)
        composeRule.waitForIdle()

        // 3a. Emit far location: ~30 km away (ETA ~60 min at 30 km/h) -> Monitoring state
        controllableTracker.locationFlow.tryEmit(GeoPoint(0.27, 0.0))
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(HomeTestTags.STATUS_TEXT).assertTextContains("Monitorando")
        composeRule.onNodeWithTag(HomeTestTags.ETA_TEXT).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.DISTANCE_TEXT).assertIsDisplayed()
        assertEquals(0, fakeFeedbackPlayer.vibrationTriggerCount)
        assertEquals(0, fakeFeedbackPlayer.alarmTriggerCount)

        // 3b. Emit coordinate in vibration zone: ~4 km away (ETA ~8 min) -> VibrationTriggered
        controllableTracker.locationFlow.tryEmit(GeoPoint(0.036, 0.0))
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(HomeTestTags.STATUS_TEXT).assertTextContains("Vibração")
        assertTrue(viewModel.uiState.value.trackingState is TrackingState.VibrationTriggered)
        assertEquals(1, fakeFeedbackPlayer.vibrationTriggerCount)
        assertEquals(0, fakeFeedbackPlayer.alarmTriggerCount)

        // 3c. Emit coordinate in alarm zone: ~1 km away (ETA ~2 min) -> AlarmTriggered
        controllableTracker.locationFlow.tryEmit(GeoPoint(0.009, 0.0))
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(HomeTestTags.STATUS_TEXT).assertTextContains("ALARME")
        assertTrue(viewModel.uiState.value.trackingState is TrackingState.AlarmTriggered)
        assertEquals(1, fakeFeedbackPlayer.alarmTriggerCount)

        // 4. Stop monitoring and ensure physical alerts stop
        composeRule.onNodeWithTag(HomeTestTags.STOP_BUTTON).performClick()
        composeRule.waitForIdle()
        assertTrue(fakeFeedbackPlayer.stopCount >= 1)
        assertEquals(TrackingState.Idle, viewModel.uiState.value.trackingState)
    }
}
