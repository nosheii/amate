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

// Samler mål og påminnelser i versjon 4. Eksisterende vann og innstillinger røres ikke.
// Kolonnenavn, typer og den unike indeksen må samsvare med Najmas Entity-klasser.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                userId INTEGER NOT NULL,
                type TEXT NOT NULL,
                targetValue INTEGER NOT NULL,
                isActive INTEGER NOT NULL
            )
            """.trimIndent()
        )
        // Samme bruker skal ikke kunne få to mål av samme type.
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_goals_userId_type ON goals (userId, type)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS self_care_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                userId INTEGER NOT NULL,
                title TEXT NOT NULL,
                scheduledForMillis INTEGER NOT NULL,
                isCompleted INTEGER NOT NULL,
                completedAtMillis INTEGER
            )
            """.trimIndent()
        )
    }
}

// Under utvikling fikk begge branchene en versjon 2, med forskjellige tabeller.
// Gå direkte 2 → 4, så også en emulator med Najmas gamle mål kan oppgraderes trygt.
// IF NOT EXISTS beholder tabeller og data som finnes, og legger bare til det som mangler.
val MIGRATION_2_4 = object : Migration(2, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_1_2.migrate(db)
        MIGRATION_2_3.migrate(db)
        MIGRATION_3_4.migrate(db)
    }
}

// Appen og testene bruker samme liste, så en ny oppgraderingsvei ikke glemmes ett sted.
// Room velger den direkte 2 → 4-veien når den finnes. Den gamle 2 → 3 beholdes uendret.
val ALL_DATABASE_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_2_4)
