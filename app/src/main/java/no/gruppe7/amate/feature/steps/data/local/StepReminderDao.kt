package no.gruppe7.amate.feature.steps.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// DAO er samlingen med databaseoperasjoner. Room lager koden bak funksjonene for oss.
@Dao
interface StepReminderDao {
    // Flow sender nye innstillinger til skjermen når raden endres.
    // :userId erstattes med ID-en vi sender inn. Vi henter bare den brukerens valg.
    // Spørsmålstegnet i StepReminderEntity? betyr at raden kan mangle hos en ny bruker.
    @Query("SELECT * FROM step_reminder_settings WHERE userId = :userId")
    fun observeSettings(userId: Long): Flow<StepReminderEntity?>

    // Bakgrunnsjobben leser de nyeste lagrede valgene, ikke et gammelt UI-utkast.
    // Her trenger vi bare ett svar akkurat nå, derfor suspend i stedet for Flow.
    @Query("SELECT * FROM step_reminder_settings WHERE userId = :userId")
    suspend fun getSettings(userId: Long): StepReminderEntity?

    // Lager raden første gang, og oppdaterer samme rad ved neste lagring.
    // @Upsert gjør begge deler, så vi slipper å sjekke om raden finnes selv.
    @Upsert
    suspend fun save(settings: StepReminderEntity)
}
