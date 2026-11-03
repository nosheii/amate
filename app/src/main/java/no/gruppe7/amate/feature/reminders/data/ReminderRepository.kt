package no.gruppe7.amate.feature.reminders.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import no.gruppe7.amate.feature.reminders.data.local.ReminderDao
import no.gruppe7.amate.feature.reminders.data.local.ReminderEntity

// ViewModel bruker dette grensesnittet, og trenger derfor ikke kjenne Room.
// Testene kan bruke en enkel testversjon av samme avtale i stedet for en ekte database.
interface ReminderRepository {
    // Flow er en strøm av oppdateringer, ikke bare ett svar fra databasen.
    fun observeSettings(userId: Long): Flow<ReminderEntity>
    // suspend lar lagringen vente på databasen uten å blokkere skjermen.
    suspend fun save(settings: ReminderEntity)
}

// Dette er versjonen som faktisk bruker Room. DAO-en gjør selve databasejobben.
class RoomReminderRepository(private val dao: ReminderDao) : ReminderRepository {
    override fun observeSettings(userId: Long): Flow<ReminderEntity> =
        dao.observeSettings(userId).map { saved ->
            // En ny bruker ser standardvalg, men vi lagrer ikke før «Lagre» trykkes.
            // ?: betyr «bruk det til høyre hvis verdien til venstre er null».
            saved ?: ReminderEntity(userId = userId)
        }

    override suspend fun save(settings: ReminderEntity) {
        // Samme kontroll beskytter også hvis innstillingene lagres fra en annen skjerm.
        // require stopper lagringen med en feil hvis en verdi ikke er gyldig.
        // 1439 er 23:59. Fra må være før Til; denne versjonen støtter ikke over midnatt.
        require(settings.userId > 0)
        require(settings.remindersPerDay in 1..5)
        require(settings.startMinute in 0..1439 && settings.endMinute in 0..1439)
        require(settings.startMinute < settings.endMinute)
        // Først når alle kontrollene over er bestått, skrives valgene til Room.
        dao.save(settings)
    }
}
