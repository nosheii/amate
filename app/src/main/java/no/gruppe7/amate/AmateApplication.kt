package com.example.amate

import android.app.Application
import com.example.amate.core.di.AppContainer

class AmateApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

