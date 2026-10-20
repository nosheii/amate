package no.gruppe7.amate.feature.goals.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.core.database.AmateDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Test: kjører på emulator, fordi Room trenger ekte SQLite.
@RunWith(AndroidJUnit4::class)
class GoalDaoTest {

    private lateinit var database: AmateDatabase
    private lateinit var goalDao: GoalDao

    // Kjøres for hver test og lagrer en midletidig database i minnet.
    // Forsvinner når testen er ferdig, så appens ekte database aldri blir rørt.

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AmateDatabase::class.java).build()
        goalDao = database.goalDao()
    }

    // Lukker den midlertidig databasen etter testen.
    @After
    fun tearDown() { database.close() }

    @Test
    fun upsert_savesGoal() = runBlocking {
        goalDao.upsert(GoalEntity(userId = 1, type = GoalType.WATER, targetValue = 2000))

        // first() henter den første listen som Flow sender.
        val goals = goalDao.observeActiveGoals(1).first()
        assertEquals(1, goals.size)
        assertEquals(GoalType.WATER, goals.first().type)
        assertEquals(2000, goals.first().targetValue)
    }

    @Test
    fun upsert_withSameId_updatesValue() = runBlocking {
        // Lagrer først et mål og tar vare på ID-en Room lager.
        val id = goalDao.upsert(GoalEntity(userId = 1, type = GoalType.STEPS, targetValue = 6000))
        // Lagrer på nytt med samme ID.
        goalDao.upsert(GoalEntity(id = id, userId = 1, type = GoalType.STEPS, targetValue = 8000))
        val goals = goalDao.observeActiveGoals(1).first()
        assertEquals(1, goals.size)
        assertEquals(8000, goals.single().targetValue)
    }
}
