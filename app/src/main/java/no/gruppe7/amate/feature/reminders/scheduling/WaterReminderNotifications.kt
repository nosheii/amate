package no.gruppe7.amate.feature.reminders.scheduling

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import no.gruppe7.amate.MainActivity
import no.gruppe7.amate.R
import java.util.UUID

// Her ligger Android-delen av varslingen. Denne klassen bestemmer ikke når noe skal sendes.
// Både bakgrunnsjobben og testknappen bruker den, men med ulike varsel-ID-er.
class WaterReminderNotifications(context: Context) : WaterReminderNotifier {
    // Vi beholder appens context, ikke en Activity som kanskje er lukket når jobben kjører.
    private val context = context.applicationContext
    // Androids innebygde tjeneste for å opprette kanaler og håndtere varsler.
    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        // Kanalen gjør at brukeren kan styre bare vannvarsler i Androids innstillinger.
        // Å opprette den viser ingen tillatelsesdialog og sender heller ikke et varsel.
        // Samme kanal-ID brukes hver gang. Android beholder brukerens valg for kanalen.
        // IMPORTANCE_DEFAULT er standardnivået; det overstyrer ikke lydløs/Ikke forstyrr.
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID,
            context.getString(R.string.reminders_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.reminders_channel_description)
        })
    }

    // Samme tillatelsessjekk brukes både av skjermen og rett før vi prøver å sende.
    override fun isAllowed() = notificationsAllowed(context)

    override fun show(userId: Long) {
        // Vanlige påminnelser beholder sin egen ID, adskilt fra testknappen.
        post(tag(userId), NOTIFICATION_ID, context.getString(R.string.reminders_notification_title),
            context.getString(R.string.reminders_notification_body))
    }

    // Sender med én gang. Går ikke innom WorkManager eller telleren i Room.
    // Egen ID gjør at testvarselet ikke erstatter en vanlig vannpåminnelse.
    fun showTestNotification(): Boolean = post(TEST_TAG, TEST_NOTIFICATION_ID,
        context.getString(R.string.reminders_test_notification_title),
        context.getString(R.string.reminders_test_notification_body))

    // Felles oppskrift for vanlige varsler og testvarsler, med hver sin tekst og identitet.
    // true betyr sendt til Android, ikke at lyden ble spilt eller at brukeren har sett det.
    private fun post(tag: String, id: Int, title: String, body: String): Boolean {
        if (!isAllowed()) return false
        // PendingIntent er en handling Android kan utføre senere, når varselet trykkes.
        // Handlingen under ber MainActivity åpne vannregistrering direkte, også fra en annen fane.
        // UPDATE_CURRENT gjenbruker handlingen; IMMUTABLE låser innholdet mot endring ved bruk.
        val openApp = PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_WATER
                // Ny ID per varsel gjør at appen kan skille et nytt trykk fra en gammel,
                // gjenopprettet startmelding. Dette er en tilfeldig ID, ikke en bruker-ID.
                putExtra(MainActivity.EXTRA_WATER_REQUEST_ID, UUID.randomUUID().toString())
                // CLEAR_TOP går tilbake til Activity-en hvis den finnes i oppgavestakken.
                // SINGLE_TOP gjenbruker den hvis den da ligger øverst, i stedet for en ekstra kopi.
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // Builder samler ikon, tekster og trykk-handling før build lager selve varselet.
        // Lite ikon må være et enkelt varselikon, ikke hele appens bakgrunnsbilde.
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(openApp)
            // Fjerner varselet fra varselfeltet når brukeren trykker på det.
            .setAutoCancel(true)
            .build()
        return try {
            // Samme ID oppdaterer forrige vannvarsel i stedet for å fylle varselfeltet.
            NotificationManagerCompat.from(context).notify(tag, id, notification)
            true
        } catch (_: SecurityException) {
            // Android kan trekke tillatelsen tilbake rett etter sjekken. Appen skal ikke krasje.
            false
        }
    }

    override fun cancel(userId: Long) { manager.cancel(tag(userId), NOTIFICATION_ID) }

    companion object {
        // Konstante navn og ID-er, slik at alle deler av appen kjenner igjen samme kanal/varsel.
        const val CHANNEL_ID = "water_reminders"
        const val NOTIFICATION_ID = 4201
        const val TEST_NOTIFICATION_ID = 4202
        const val TEST_TAG = "water-reminder-test"
        // Android kjenner igjen varsler med kombinasjonen tag + ID. Tag skiller brukerne.
        fun tag(userId: Long) = "water-reminder-$userId"

        fun notificationsAllowed(context: Context): Boolean {
            // Android 13+ krever POST_NOTIFICATIONS. Eldre Android trenger ikke denne dialogen.
            val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            val manager = context.getSystemService(NotificationManager::class.java)
            // Alle sjekkene må være OK: tillatelsen, varsler for appen og vannkanalen.
            // ?. betyr at en kanal som ennå ikke finnes ikke krasjer sjekken.
            // En slik manglende kanal teller ikke som blokkert; den opprettes i init før sending.
            return granted && NotificationManagerCompat.from(context).areNotificationsEnabled() &&
                manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
        }
    }
}
