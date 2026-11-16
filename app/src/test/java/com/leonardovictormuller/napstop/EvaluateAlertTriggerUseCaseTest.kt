package com.leonardovictormuller.napstop

import com.leonardovictormuller.napstop.domain.model.AlertConfig
import com.leonardovictormuller.napstop.domain.model.TrackingState
import com.leonardovictormuller.napstop.domain.usecase.EvaluateAlertTriggerUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EvaluateAlertTriggerUseCaseTest {

    private lateinit var useCase: EvaluateAlertTriggerUseCase
    private val config = AlertConfig(vibrationLeadTimeMinutes = 10, alarmLeadTimeMinutes = 5)

    @Before
    fun setUp() {
        useCase = EvaluateAlertTriggerUseCase()
    }

    private fun monitoringState(eta: Double = 20.0) = TrackingState.Monitoring(
        etaMinutes = eta,
        distanceKm = 10.0,
        speedKmh = 30.0,
    )

    @Test
    fun `monitoring stays monitoring when eta is above vibration threshold`() {
        val result = useCase(monitoringState(eta = 15.0), etaMinutes = 15.0, config = config)
        assertTrue(result is TrackingState.Monitoring)
    }

    @Test
    fun `monitoring transitions to vibration when eta equals vibration threshold`() {
        val result = useCase(monitoringState(eta = 10.0), etaMinutes = 10.0, config = config)
        assertTrue(result is TrackingState.VibrationTriggered)
    }

    @Test
    fun `monitoring transitions to vibration when eta is below vibration threshold`() {
        val result = useCase(monitoringState(eta = 8.0), etaMinutes = 8.0, config = config)
        assertTrue(result is TrackingState.VibrationTriggered)
    }

    @Test
    fun `monitoring skips vibration and goes directly to alarm when eta is at alarm threshold`() {
        val result = useCase(monitoringState(eta = 5.0), etaMinutes = 5.0, config = config)
        assertTrue(result is TrackingState.AlarmTriggered)
    }

    @Test
    fun `monitoring goes to alarm when eta is below alarm threshold`() {
        val result = useCase(monitoringState(eta = 3.0), etaMinutes = 3.0, config = config)
        assertTrue(result is TrackingState.AlarmTriggered)
    }

    @Test
    fun `vibration stays vibration when eta is above alarm threshold`() {
        val vibrating = TrackingState.VibrationTriggered(etaMinutes = 8.0)
        val result = useCase(vibrating, etaMinutes = 7.0, config = config)
        assertTrue(result is TrackingState.VibrationTriggered)
    }

    @Test
    fun `vibration transitions to alarm when eta is at alarm threshold`() {
        val vibrating = TrackingState.VibrationTriggered(etaMinutes = 8.0)
        val result = useCase(vibrating, etaMinutes = 5.0, config = config)
        assertTrue(result is TrackingState.AlarmTriggered)
        assertEquals(5.0, (result as TrackingState.AlarmTriggered).etaMinutes, 0.001)
    }

    @Test
    fun `alarm state does not regress when use case is called again`() {
        val alarmed = TrackingState.AlarmTriggered(etaMinutes = 3.0)
        val result = useCase(alarmed, etaMinutes = 1.0, config = config)
        assertTrue(result is TrackingState.AlarmTriggered)
    }

    @Test
    fun `idle state is not changed by use case`() {
        val result = useCase(TrackingState.Idle, etaMinutes = 1.0, config = config)
        assertEquals(TrackingState.Idle, result)
    }

    @Test
    fun `AlertConfig throws when vibration lead time is not greater than alarm lead time`() {
        try {
            AlertConfig(vibrationLeadTimeMinutes = 5, alarmLeadTimeMinutes = 5)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("vibrationLeadTimeMinutes") == true)
        }
    }

    @Test
    fun `AlertConfig throws when alarm lead time is zero`() {
        try {
            AlertConfig(vibrationLeadTimeMinutes = 10, alarmLeadTimeMinutes = 0)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("positive") == true)
        }
    }
}
