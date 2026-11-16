package com.leonardovictormuller.napstop.di

import android.content.Context
import com.leonardovictormuller.napstop.data.alert.AndroidAlertFeedbackPlayer
import com.leonardovictormuller.napstop.data.location.AndroidLocationTracker
import com.leonardovictormuller.napstop.data.location.SimulatedLocationTracker
import com.leonardovictormuller.napstop.domain.alert.AlertFeedbackPlayer
import com.leonardovictormuller.napstop.domain.repository.LocationTracker
import com.leonardovictormuller.napstop.domain.usecase.CalculateEtaUseCase
import com.leonardovictormuller.napstop.domain.usecase.EvaluateAlertTriggerUseCase

/**
 * Manual Dependency Injection container.
 *
 * This is the single source of truth for all singleton-scoped dependencies.
 * Provides the real [AndroidLocationTracker] and [AndroidAlertFeedbackPlayer] by default,
 * while allowing [SimulatedLocationTracker] to be selected for testing or preview purposes.
 */
class AppContainer(
    context: Context,
    useSimulatedLocation: Boolean = false,
) {
    private val appContext = context.applicationContext

    // Data layer: Real Android GPS Tracker by default; Simulated for preview/testing
    val locationTracker: LocationTracker by lazy {
        if (useSimulatedLocation) {
            SimulatedLocationTracker()
        } else {
            AndroidLocationTracker(appContext)
        }
    }

    val alertFeedbackPlayer: AlertFeedbackPlayer by lazy {
        AndroidAlertFeedbackPlayer(appContext)
    }

    // Domain use cases (pure Kotlin, stateless)
    val calculateEtaUseCase: CalculateEtaUseCase by lazy { CalculateEtaUseCase() }
    val evaluateAlertTriggerUseCase: EvaluateAlertTriggerUseCase by lazy { EvaluateAlertTriggerUseCase() }
}
