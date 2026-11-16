package com.leonardovictormuller.napstop

import android.app.Application
import com.leonardovictormuller.napstop.di.AppContainer

/**
 * Custom [Application] class that hosts the [AppContainer].
 *
 * The ViewModel factory retrieves dependencies from here through the Activity,
 * keeping the dependency graph centralized and testable.
 */
class NapStopApplication : Application() {

    /** Application-scoped DI container. Created once, lives as long as the app process. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
