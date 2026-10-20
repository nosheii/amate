package no.gruppe7.amate

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import no.gruppe7.amate.core.di.AppContainer

// Android starter Application før skjermene våre. Det er et felles startpunkt for hele appen.
class AmateApplication : Application() {
    // by lazy lager containeren ved første bruk og beholder den samme videre.
    val container: AppContainer by lazy { AppContainer(this) }

    // Lever like lenge som app-prosessen, ikke bare så lenge én skjerm er åpen.
    // SupervisorJob hindrer at en feil i én coroutine automatisk stopper de andre i dette scopet.
    // Dispatchers.IO brukes til venting/databasearbeid, så skjermtråden ikke blokkeres.
    // Dette scopet holder ikke appen i live for alltid; WorkManager tar seg av bakgrunnskjøringen.
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            // Samme midlertidige bruker som vannskjermen. Profil kobles til senere.
            // Starter opp igjen etter omstart og reagerer på lagrede endringer fra alle skjermer.
            // distinctUntilChanged hopper over like verdier. Bare faktiske endringer trenger en ny synk.
            // collect helt til slutt starter innsamlingen. Selve handlingen skjer i onEach.
            container.reminderRepository.observeSettings(1).distinctUntilChanged()
                // it er innstillingene vi nettopp fikk. Planleggeren sørger for riktig av/på-jobb.
                .onEach { container.reminderScheduler.synchronize(it) }
                .retryWhen { error, _ ->
                    // Om tilkoblingen feiler, logger vi feilen og prøver igjen etter en pause.
                    // delay venter uten å blokkere en tråd. true betyr «start lyttingen på nytt».
                    Log.w("AmateApplication", "Prøver å koble påminnelsene til igjen", error)
                    delay(30_000)
                    true
                }.collect { }
        }
    }
}

