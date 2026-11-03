package no.gruppe7.amate.feature.reminders.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Én rad per bruker. Denne tabellen gjelder bare vannpåminnelser.
// @Entity forteller Room at denne Kotlin-klassen skal bli en tabell.
// Hvert felt under blir en kolonne. data class gir oss blant annet copy() gratis.
@Entity(tableName = "water_reminder_settings")
data class ReminderEntity(
    // Bruker-ID er nøkkelen: samme bruker kan bare ha én rad med disse innstillingene.
    @PrimaryKey val userId: Long,
    // Dette er brukerens ønske. Androids varslingstillatelse sjekkes separat.
    val enabled: Boolean = false,
    // Ønsket antall, ikke en garanti om hvor mange Android rekker å vise.
    val remindersPerDay: Int = 3,
    // Tid lagres som minutter siden midnatt: 08:00 blir 480.
    val startMinute: Int = 8 * 60,
    // Til er grensen for sending. Et varsel skal ikke sendes akkurat ved eller etter denne tiden.
    val endMinute: Int = 21 * 60,
)
