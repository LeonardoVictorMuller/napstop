package com.leonardovictormuller.napstop.di

import com.leonardovictormuller.napstop.data.location.SimulatedLocationTracker
import com.leonardovictormuller.napstop.domain.repository.LocationTracker
import com.leonardovictormuller.napstop.domain.usecase.CalculateEtaUseCase
import com.leonardovictormuller.napstop.domain.usecase.EvaluateAlertTriggerUseCase

/**
 * Manual Dependency Injection container.
 *
 * This is the single source of truth for all singleton-scoped dependencies.
 * The UI layer receives this container through the [NapStopApplication] and
 * uses it to construct ViewModels — it never instantiates dependencies directly.
 *
 * Design rationale:
 * - No Android Context references stored here (privacy + KMP readiness).
 * - Easy to swap [SimulatedLocationTracker] for the real GPS tracker in one line.
 * - Easy to replace with Hilt/Koin in the future without changing domain or UI code.
 */
class AppContainer {

    // Data layer: swap this for FusedLocationTracker when GPS is ready
    val locationTracker: LocationTracker by lazy { SimulatedLocationTracker() }

    // Domain use cases (stateless, so single instances are safe)
    val calculateEtaUseCase: CalculateEtaUseCase by lazy { CalculateEtaUseCase() }
    val evaluateAlertTriggerUseCase: EvaluateAlertTriggerUseCase by lazy { EvaluateAlertTriggerUseCase() }
}
