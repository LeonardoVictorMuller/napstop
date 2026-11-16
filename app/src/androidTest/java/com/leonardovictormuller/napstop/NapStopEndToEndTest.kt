package com.leonardovictormuller.napstop

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.leonardovictormuller.napstop.data.location.SimulatedLocationTracker
import com.leonardovictormuller.napstop.domain.usecase.CalculateEtaUseCase
import com.leonardovictormuller.napstop.domain.usecase.EvaluateAlertTriggerUseCase
import com.leonardovictormuller.napstop.ui.home.HomeScreen
import com.leonardovictormuller.napstop.ui.home.HomeTestTags
import com.leonardovictormuller.napstop.ui.home.HomeViewModel
import com.leonardovictormuller.napstop.ui.theme.NapStopTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UI test simulating a real user session with the Home screen.
 *
 * Uses [SimulatedLocationTracker] so no GPS hardware is required.
 * The simulated tracker emits a route through São Paulo that is far from
 * the preset destinations, so the app stays in Monitoring state long enough
 * to assert all UI elements.
 */
@RunWith(AndroidJUnit4::class)
class NapStopEndToEndTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun launchHomeScreen(
        vibrationMinutes: Int = 10,
        alarmMinutes: Int = 5,
    ): HomeViewModel {
        val viewModel = HomeViewModel(
            locationTracker = SimulatedLocationTracker(),
            calculateEtaUseCase = CalculateEtaUseCase(),
            evaluateAlertTriggerUseCase = EvaluateAlertTriggerUseCase(),
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
    fun startButton_isEnabledAfterValidInput() {
        launchHomeScreen()

        composeRule.onNodeWithTag(HomeTestTags.DESTINATION_NAME_INPUT)
            .performTextInput("Estação Paulista")

        composeRule.onNodeWithTag(HomeTestTags.LATITUDE_INPUT)
            .performTextInput("-23.5635")

        composeRule.onNodeWithTag(HomeTestTags.LONGITUDE_INPUT)
            .performTextInput("-46.6549")

        composeRule.onNodeWithTag(HomeTestTags.START_BUTTON).assertIsEnabled()
    }

    @Test
    fun presetSelection_fillsInputsAndEnablesStart() {
        launchHomeScreen()

        // Click the first preset "Estação Paulista"
        composeRule
            .onNodeWithTag("${HomeTestTags.PRESET_PREFIX}Estação Paulista")
            .performClick()

        composeRule.onNodeWithTag(HomeTestTags.DESTINATION_NAME_INPUT)
            .assertTextContains("Estação Paulista")

        composeRule.onNodeWithTag(HomeTestTags.START_BUTTON).assertIsEnabled()
    }

    @Test
    fun startMonitoring_showsStopButtonAndStatusText() {
        launchHomeScreen()

        // Fill in a destination far away so we stay in Monitoring for the test
        composeRule.onNodeWithTag(HomeTestTags.DESTINATION_NAME_INPUT)
            .performTextInput("Rio de Janeiro")
        composeRule.onNodeWithTag(HomeTestTags.LATITUDE_INPUT)
            .performTextInput("-22.9068")
        composeRule.onNodeWithTag(HomeTestTags.LONGITUDE_INPUT)
            .performTextInput("-43.1729")

        composeRule.onNodeWithTag(HomeTestTags.START_BUTTON).performClick()

        // After starting, Stop button should appear
        composeRule.onNodeWithTag(HomeTestTags.STOP_BUTTON).assertIsDisplayed()

        // Status should show monitoring text
        composeRule.onNodeWithTag(HomeTestTags.STATUS_TEXT).assertIsDisplayed()
    }

    @Test
    fun stopMonitoring_showsStartButtonAgain() {
        launchHomeScreen()

        composeRule.onNodeWithTag(HomeTestTags.DESTINATION_NAME_INPUT)
            .performTextInput("Rio de Janeiro")
        composeRule.onNodeWithTag(HomeTestTags.LATITUDE_INPUT)
            .performTextInput("-22.9068")
        composeRule.onNodeWithTag(HomeTestTags.LONGITUDE_INPUT)
            .performTextInput("-43.1729")

        composeRule.onNodeWithTag(HomeTestTags.START_BUTTON).performClick()
        composeRule.onNodeWithTag(HomeTestTags.STOP_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.STOP_BUTTON).performClick()

        // After stopping, Start button should reappear
        composeRule.onNodeWithTag(HomeTestTags.START_BUTTON).assertIsDisplayed()
    }

    @Test
    fun etaAndDistance_areDisplayedWhileMonitoring() {
        launchHomeScreen()

        // Use a distant destination so ETA is well above alert thresholds
        composeRule.onNodeWithTag(HomeTestTags.DESTINATION_NAME_INPUT)
            .performTextInput("Buenos Aires")
        composeRule.onNodeWithTag(HomeTestTags.LATITUDE_INPUT)
            .performTextInput("-34.6037")
        composeRule.onNodeWithTag(HomeTestTags.LONGITUDE_INPUT)
            .performTextInput("-58.3816")

        composeRule.onNodeWithTag(HomeTestTags.START_BUTTON).performClick()

        // Wait for the simulated tracker to emit and UI to update
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(HomeTestTags.ETA_TEXT).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.DISTANCE_TEXT).assertIsDisplayed()
    }

    @Test
    fun manualInput_invalidLatitude_disablesStart() {
        launchHomeScreen()

        composeRule.onNodeWithTag(HomeTestTags.DESTINATION_NAME_INPUT)
            .performTextInput("Test")
        composeRule.onNodeWithTag(HomeTestTags.LATITUDE_INPUT)
            .performTextInput("INVALID")
        composeRule.onNodeWithTag(HomeTestTags.LONGITUDE_INPUT)
            .performTextInput("-46.6")

        composeRule.onNodeWithTag(HomeTestTags.START_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun statusText_showsIdleMessageBeforeStart() {
        launchHomeScreen()
        composeRule.onNodeWithTag(HomeTestTags.STATUS_TEXT).assertTextContains("Aguardando")
    }
}
