package no.gruppe7.amate.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Flytter en eksisterende installasjon fra databaseversjon 1 til 2.
// Vi legger bare til den nye tabellen. Ingen vannregistreringer slettes eller endres.
// migrate kjøres av Room når en gammel database åpnes med en nyere skjemaversjon.
// SQL-en beskriver samme kolonner og typer som Entity-klassen: tall, tekst og primærnøkkel.
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // IF NOT EXISTS betyr «lag tabellen bare hvis den ikke finnes fra før».
        // enabled lagres som INTEGER (0/1) i SQLite selv om Kotlin-feltet er Boolean.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS water_reminder_settings (
                userId INTEGER NOT NULL PRIMARY KEY,
                enabled INTEGER NOT NULL,
                remindersPerDay INTEGER NOT NULL,
                startMinute INTEGER NOT NULL,
                endMinute INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

// Versjon 3 husker sendeforsøk. Både vannloggen og innstillingene beholdes.
// En installasjon på versjon 1 bruker først 1→2 og deretter 2→3.
// En installasjon på versjon 2 trenger bare denne siste delen.
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Primærnøkkelen består av bruker + lokal dato, akkurat som i ReminderDeliveryEntity.
        // Dette oppretter bare loggtabellen. Det er Runner som senere fyller den med data.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS water_reminder_delivery (
                userId INTEGER NOT NULL,
                localDate TEXT NOT NULL,
                lastSlotMinute INTEGER NOT NULL,
                sentCount INTEGER NOT NULL,
                lastAttemptMillis INTEGER NOT NULL,
                PRIMARY KEY(userId, localDate)
            )
            """.trimIndent()
        )
    }
}
