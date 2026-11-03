package no.gruppe7.amate.feature.reminders.scheduling

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import no.gruppe7.amate.AmateApplication

// Android oppretter denne når det er tid for en bakgrunnssjekk, også uten åpen skjerm.
// CoroutineWorker lar oss bruke suspend-funksjoner når vi leser og skriver i Room.
class WaterReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    // WorkManager kaller doWork automatisk. Skjermen trenger ikke å kalle den selv.
    override suspend fun doWork(): Result {
        // ID-en ble lagt i inputData av Scheduler. -1 brukes hvis den mangler.
        val userId = inputData.getLong(USER_ID, -1)
        if (userId <= 0) return Result.failure()
        return try {
            // Henter de samme felles objektene som resten av appen bruker.
            val container = (applicationContext as AmateApplication).container
            container.waterReminderRunner.run(userId)
            // Avslått tillatelse eller utenfor tidsrommet er normalt, ikke en feil.
            // success betyr «sjekken gikk fint», også hvis den ikke skulle sende noe.
            Result.success()
        } catch (error: CancellationException) {
            // WorkManager kan stoppe jobben, f.eks. når den avbrytes. Vi skal respektere det,
            // ikke behandle en vanlig stopp som en feil som må prøves på nytt.
            throw error
        } catch (error: Exception) {
            Log.w("WaterReminderWorker", "Kunne ikke sjekke vannpåminnelsen", error)
            // Kortvarige databasefeil får et par nye forsøk, ikke en evig løkke.
            // retry ber WorkManager prøve senere. failure avslutter dette forsøksløpet.
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    // Felles nøkkel for verdien Scheduler sender og Worker leser, så navnene ikke blir ulike.
    companion object { const val USER_ID = "water_reminder_user_id" }
}
