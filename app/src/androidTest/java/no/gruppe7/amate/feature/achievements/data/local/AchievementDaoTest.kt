package no.gruppe7.amate.feature.achievements.data.local

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

@RunWith(AndroidJUnit4::class)
class AchievementDaoTest {

    private lateinit var database: AmateDatabase
    private lateinit var dao: AchievementDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AmateDatabase::class.java).build()
        dao = database.achievementDao()
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun insertAll_savesEarnedBadges() = runBlocking {
        dao.insertAll(listOf(
            UserAchievementEntity(1, "WATER_FIRST", 1000),
            UserAchievementEntity(1, "TASKS_FIRST", 2000),
        ))

        assertEquals(listOf("WATER_FIRST", "TASKS_FIRST"), dao.observeEarned(1).first().map { it.code })
    }

    @Test
    fun insertAll_keepsOriginalDateForBadgeAlreadyEarned() = runBlocking {
        dao.insertAll(listOf(UserAchievementEntity(1, "WATER_FIRST", 1000)))
        // Samme merke "deles ut" igjen senere. Den første datoen skal stå.
        dao.insertAll(listOf(UserAchievementEntity(1, "WATER_FIRST", 9999)))

        val earned = dao.observeEarned(1).first()
        assertEquals(1, earned.size)
        assertEquals(1000L, earned.single().earnedAtMillis)
    }

    @Test
    fun observeEarned_isPerUser() = runBlocking {
        dao.insertAll(listOf(
            UserAchievementEntity(1, "WATER_FIRST", 1000),
            UserAchievementEntity(2, "WATER_10L", 1000),
        ))

        assertEquals(listOf("WATER_FIRST"), dao.observeEarned(1).first().map { it.code })
    }
}