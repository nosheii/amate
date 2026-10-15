package no.gruppe7.amate.feature.water.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import no.gruppe7.amate.core.database.AmateDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WaterDaoTest {

    private lateinit var database: AmateDatabase
    private lateinit var waterDao: WaterDao

    @Test
    fun dayQueries_excludeOtherUsersAndUseExclusiveEnd() = runBlocking {
        // Starten er med, slutten er ikke med. Andre brukere skal heller ikke tas med.
        val rows = listOf(
            WaterEntryEntity(userId = 1, amountMl = 100, loggedAtMillis = 999),
            WaterEntryEntity(userId = 1, amountMl = 250, loggedAtMillis = 1000),
            WaterEntryEntity(userId = 1, amountMl = 350, loggedAtMillis = 1999),
            WaterEntryEntity(userId = 1, amountMl = 500, loggedAtMillis = 2000),
            WaterEntryEntity(userId = 2, amountMl = 750, loggedAtMillis = 1500),
        )
        rows.forEach { waterDao.insert(it) }
        val entries = waterDao.observeEntriesForDay(1, 1000, 2000).first()
        assertEquals(listOf(350, 250), entries.map { it.amountMl })
        assertEquals(600, waterDao.observeTotalForDay(1, 1000, 2000).first())
        assertEquals(0, waterDao.observeTotalForDay(1, 3000, 4000).first())
    }

    @Test
    fun entries_surviveClosingAndReopeningDatabase() = runBlocking {
        // Bruker en egen testfil, så appens ekte database aldri blir rørt.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "water-persistence-${java.util.UUID.randomUUID()}.db"
        var persistent = Room.databaseBuilder(context, AmateDatabase::class.java, name).build()
        try {
            persistent.waterDao().insert(
                WaterEntryEntity(userId = 1, amountMl = 375, loggedAtMillis = 1000)
            )
            persistent.close()
            persistent = Room.databaseBuilder(context, AmateDatabase::class.java, name).build()
            assertEquals(375, persistent.waterDao().observeEntries(1).first().single().amountMl)
        } finally {
            persistent.close()
            context.deleteDatabase(name)
        }
    }

    // Kjøres før hver test og lager en midlertidig database.
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            AmateDatabase::class.java,
        ).build()

        waterDao = database.waterDao()
    }

    // Lukker den midlertidige databasen etter testen.
    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insert_savesWaterEntry() = runBlocking {
        val entry = WaterEntryEntity(
            userId = 1,
            amountMl = 250,
            loggedAtMillis = 1000,
        )

        waterDao.insert(entry)

        // first() henter den første listen som Flow sender.
        val savedEntries = waterDao.observeEntries(userId = 1).first()

        assertEquals(1, savedEntries.size)
        assertEquals(250, savedEntries.first().amountMl)
    }

    @Test
    fun observeTotalForDay_returnsCorrectTotal() = runBlocking {
        waterDao.insert(
            WaterEntryEntity(
                userId = 1,
                amountMl = 250,
                loggedAtMillis = 1000,
            )
        )

        waterDao.insert(
            WaterEntryEntity(
                userId = 1,
                amountMl = 350,
                loggedAtMillis = 2000,
            )
        )

        // Summerer registreringer mellom tidspunkt 0 og 5000.
        val total = waterDao.observeTotalForDay(
            userId = 1,
            startMillis = 0,
            endMillis = 5000,
        ).first()

        assertEquals(600, total)
    }

    @Test
    fun update_changesWaterEntry() = runBlocking {

        // Lagrer først en registrering på 250 ml.
        // Room returnerer ID-en som ble laget.
        val id = waterDao.insert(
            WaterEntryEntity(
                userId = 1,
                amountMl = 250,
                loggedAtMillis = 1000,
            )
        )

        // Lager en oppdatert versjon med samme ID.
        // Mengden endres fra 250 ml til 500 ml.
        val updatedEntry = WaterEntryEntity(
            id = id,
            userId = 1,
            amountMl = 500,
            loggedAtMillis = 1000,
        )

        // Ber Room oppdatere raden med denne ID-en.
        waterDao.update(updatedEntry)

        // Henter registreringen på nytt fra databasen.
        val savedEntry = waterDao
            .observeEntries(userId = 1)
            .first()
            .first()

        // Kontrollerer at den lagrede mengden nå er 500 ml.
        assertEquals(500, savedEntry.amountMl)
    }

    @Test
    fun delete_removesWaterEntry() = runBlocking {

        // Lagrer først en vannregistrering.
        val id = waterDao.insert(
            WaterEntryEntity(
                userId = 1,
                amountMl = 250,
                loggedAtMillis = 1000,
            )
        )

        // Lager objektet vi vil slette.
        // Det må ha samme ID som raden i databasen.
        val entryToDelete = WaterEntryEntity(
            id = id,
            userId = 1,
            amountMl = 250,
            loggedAtMillis = 1000,
        )

        // Sletter registreringen fra databasen.
        waterDao.delete(entryToDelete)

        // Henter alle registreringene etter slettingen.
        val savedEntries = waterDao
            .observeEntries(userId = 1)
            .first()

        // Listen skal nå være tom.
        assertEquals(0, savedEntries.size)
    }
}
