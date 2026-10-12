package no.gruppe7.amate

import android.app.Application
import no.gruppe7.amate.core.di.AppContainer

class AmateApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

