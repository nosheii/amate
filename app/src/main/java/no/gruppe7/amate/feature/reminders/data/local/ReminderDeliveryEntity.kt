package no.gruppe7.amate.feature.reminders.data.local

import androidx.room.Entity

// Huskelapp for hva vi allerede har forsøkt å varsle om denne lokale datoen.
// Den ligger i Room, så en omstart ikke nullstiller dagens teller.
// Bruker-ID + dato er nøkkelen sammen: én rad for hver bruker på hver dato.
@Entity(tableName = "water_reminder_delivery", primaryKeys = ["userId", "localDate"])
data class ReminderDeliveryEntity(
    val userId: Long,
    // Lokal dato, for eksempel «2026-10-19». En ny dag får sin egen teller.
    val localDate: String,
    // Det planlagte tidspunktet vi sist brukte, som minutter etter midnatt.
    // Dette kan være tidligere enn faktisk sending hvis Android var forsinket.
    val lastSlotMinute: Int,
    // Antall forsøk vi har registrert. Det sier ikke at brukeren har sett eller lest varslene.
    val sentCount: Int,
    // Tidspunktet for forsøket som millisekunder siden 1970, uavhengig av tidssone.
    // Brukes som ekstra vern hvis telefonens klokke stilles tilbake.
    val lastAttemptMillis: Long,
)
