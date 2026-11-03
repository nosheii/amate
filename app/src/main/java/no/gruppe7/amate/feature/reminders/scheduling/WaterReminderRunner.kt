package no.gruppe7.amate.feature.reminders.scheduling

import androidx.room.withTransaction
import java.time.Clock
import java.time.ZonedDateTime
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.reminders.data.local.ReminderDeliveryEntity

// Skiller Androids varselkode fra logikken, så testene kan bruke et falskt varsel.
interface WaterReminderNotifier {
    // Kan vi sende, vis et varsel, og fjern et varsel. Android-versjonen ligger i Notifications-filen.
    fun isAllowed(): Boolean
    fun show(userId: Long)
    fun cancel(userId: Long)
}

// Samler én bakgrunnssjekk: les valg, spør planen, oppdater telleren og prøv å sende.
// Scheduler bestiller sjekkene, Worker starter dem, og denne klassen gjør selve jobben.
class WaterReminderRunner(
    private val database: AmateDatabase,
    private val notifier: WaterReminderNotifier,
    // Vanligvis telefonens klokke. Tester kan sende inn en fast klokke og slippe å vente.
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    // true betyr at vi gikk gjennom et sendeforsøk, ikke at brukeren har lest varselet.
    suspend fun run(userId: Long): Boolean {
        // Ved blokkering gjør vi ingenting, og bruker heller ikke opp dagens teller.
        if (!notifier.isAllowed()) return false
        // Sjekk, teller og sending holdes samlet mot andre databaseoperasjoner.
        // To jobber kan dermed ikke reservere det samme tidspunktet samtidig.
        // En transaksjon behandler databaseendringene som én samlet operasjon.
        // Android-varselet er likevel utenfor Rooms kontroll, så dette er ingen absolutt
        // leveringsgaranti hvis appen krasjer akkurat mellom varsling og database-commit.
        return database.withTransaction {
            // return@withTransaction avslutter denne blokken tidlig, med false som svar.
            // Ingen lagrede valg betyr at vi heller ikke har fått noe å sende påminnelser for.
            val settings = database.reminderDao().getSettings(userId) ?: return@withTransaction false
            val now = ZonedDateTime.now(clock)
            val date = now.toLocalDate().toString()
            // Henter bare huskelappen for denne brukeren og denne lokale datoen.
            val dao = database.reminderDeliveryDao()
            val previous = dao.get(userId, date)
            // Planen kan si nei fordi det er for tidlig, for sent, av eller allerede forsøkt.
            val slot = WaterReminderPlan.dueSlot(settings, previous, now) ?: return@withTransaction false
            // Tillatelsen kan ha blitt fjernet mens databasen ble lest.
            if (!notifier.isAllowed()) return@withTransaction false
            // Oppdaterer samme dagsrad: ett forsøk til og siste brukte planlagte tidspunkt.
            // ?: 0 gjør at telleren starter på 0 hvis dette er dagens første forsøk.
            dao.save(ReminderDeliveryEntity(userId, date, slot, (previous?.sentCount ?: 0) + 1,
                now.toInstant().toEpochMilli()))
            // Bygger ikke varselet her. Den delen overlater vi til Android-klassen.
            notifier.show(userId)
            true
        }
    }
}
