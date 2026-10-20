package no.gruppe7.amate.feature.reminders.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// DAO er samlingen med databaseoperasjoner. Room lager koden bak funksjonene for oss.
@Dao
interface ReminderDao {
    // Flow sender nye innstillinger til skjermen når raden endres.
    // :userId erstattes med ID-en vi sender inn. Vi henter bare den brukerens valg.
    // Spørsmålstegnet i ReminderEntity? betyr at raden kan mangle hos en ny bruker.
    @Query("SELECT * FROM water_reminder_settings WHERE userId = :userId")
    fun observeSettings(userId: Long): Flow<ReminderEntity?>

    // Bakgrunnsjobben leser de nyeste lagrede valgene, ikke et gammelt UI-utkast.
    // Her trenger vi bare ett svar akkurat nå, derfor suspend i stedet for Flow.
    @Query("SELECT * FROM water_reminder_settings WHERE userId = :userId")
    suspend fun getSettings(userId: Long): ReminderEntity?

    // Lager raden første gang, og oppdaterer samme rad ved neste lagring.
    // @Upsert gjør begge deler, så vi slipper å sjekke om raden finnes selv.
    @Upsert
    suspend fun save(settings: ReminderEntity)
}
