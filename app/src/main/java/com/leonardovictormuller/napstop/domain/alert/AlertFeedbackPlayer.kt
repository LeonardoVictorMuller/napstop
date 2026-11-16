package com.leonardovictormuller.napstop.domain.alert

/**
 * Pure domain interface to trigger physical alert feedback (haptics and audio).
 *
 * Free of any Android framework dependencies, adhering to Clean Architecture
 * and Dependency Inversion.
 */
interface AlertFeedbackPlayer {
    /**
     * Triggers vibration pre-alert pattern.
     */
    fun triggerVibration()

    /**
     * Triggers audible alarm and continuous vibration.
     */
    fun triggerAlarm()

    /**
     * Immediately stops any ongoing vibration and alarm audio.
     */
    fun stopAll()
}
