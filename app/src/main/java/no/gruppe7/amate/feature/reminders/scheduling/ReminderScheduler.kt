package no.gruppe7.amate.feature.reminders.scheduling

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.gruppe7.amate.feature.reminders.data.local.ReminderEntity

// Scheduler betyr bare planlegger her. Den kobler lagret av/på-valg til en bakgrunnsjobb.
// Den bestemmer ikke om et varsel er klart akkurat nå; det sjekker Runner og Plan.
interface ReminderScheduler {
    suspend fun synchronize(settings: ReminderEntity)
}

class WorkManagerReminderScheduler(
    context: Context,
    private val notifier: WaterReminderNotifier,
) : ReminderScheduler {
    // applicationContext hører til hele appen, ikke en skjerm som kan lukkes.
    private val workManager = WorkManager.getInstance(context.applicationContext)

    // Vi venter på WorkManager på IO-tråder, så ventingen ikke fryser knappene i UI-et.
    override suspend fun synchronize(settings: ReminderEntity) = withContext(Dispatchers.IO) {
        if (settings.enabled) {
            // Ber om en gjentakende sjekk med 15 minutters intervall.
            // Android kan kjøre den senere på grunn av batterisparing; dette er ingen eksakt alarm.
            val request = PeriodicWorkRequestBuilder<WaterReminderWorker>(15, TimeUnit.MINUTES)
                // Ingen øyeblikkelig sending når man lagrer. Testknappen er et eget løp.
                .setInitialDelay(15, TimeUnit.MINUTES)
                // Sender bare bruker-ID videre. Jobben henter ferske tider og antall fra Room.
                .setInputData(workDataOf(WaterReminderWorker.USER_ID to settings.userId))
                .build()
            // KEEP beholder én jobb. Tid og antall leses fra Room ved hver kjøring,
            // så vi trenger ikke lage en ny jobb hver gang noen trykker Lagre.
            // result.get() venter til WorkManager har behandlet bestillingen, ikke til et varsel sendes.
            workManager.enqueueUniquePeriodicWork(workName(settings.userId),
                ExistingPeriodicWorkPolicy.KEEP, request).result.get()
        } else {
            // Navnet peker på akkurat denne brukerens jobb, ikke alle bakgrunnsjobber i appen.
            workManager.cancelUniqueWork(workName(settings.userId)).result.get()
            // Fjerner også et gammelt vannvarsel som fortsatt ligger i varselfeltet.
            notifier.cancel(settings.userId)
        }
        // Kotlin sin måte å si at funksjonen er ferdig uten å returnere en egen verdi.
        Unit
    }

    companion object {
        // Samme bruker får alltid samme navn. Det er dette som gjør KEEP mulig.
        fun workName(userId: Long) = "water-reminders-$userId"
    }
}
