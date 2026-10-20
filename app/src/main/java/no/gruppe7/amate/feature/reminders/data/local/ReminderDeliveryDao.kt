package no.gruppe7.amate.feature.reminders.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

// Leser og oppdaterer huskelappen for dagens sendeforsøk, ikke brukerens innstillinger.
@Dao
interface ReminderDeliveryDao {
    // Vi trenger både bruker og dato, ellers kunne brukere eller ulike dager blitt blandet.
    // null betyr at det ikke ligger noen huskelapp for denne brukeren/datoen ennå.
    @Query("SELECT * FROM water_reminder_delivery WHERE userId = :userId AND localDate = :date")
    suspend fun get(userId: Long, date: String): ReminderDeliveryEntity?

    // Skriver over dagens huskelapp med den nye telleren. Lager ikke en ny rad per varsel.
    @Upsert
    suspend fun save(delivery: ReminderDeliveryEntity)
}
