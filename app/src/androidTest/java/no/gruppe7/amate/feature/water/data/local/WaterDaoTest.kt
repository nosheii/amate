package no.gruppe7.amate.amate.feature.water.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.amate.core.database.AmateDatabase
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