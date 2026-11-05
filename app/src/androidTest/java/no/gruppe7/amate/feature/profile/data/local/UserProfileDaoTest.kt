package no.gruppe7.amate.feature.profile.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import no.gruppe7.amate.core.database.AmateDatabase
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
// Samme JUnit-sjekker som resten av Android-testene bruker. Krever ikke et nytt bibliotek.
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

@RunWith(AndroidJUnit4::class)
class UserProfileDaoTest {
    private lateinit var database: AmateDatabase
    private lateinit var dao: UserProfileDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AmateDatabase::class.java).build()
        dao = database.userProfileDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun observeProfile_ikke_eksisterende_profil_NULL() = runTest {
        val profile = dao.observeProfile(999).first()
        assertNull(profile)
    }

    @Test
    fun upsert_lagrer_ny_profil() = runTest {
        dao.upsert(UserProfileEntity(userId = 1, displayName = "Kai"))

        val profile = dao.observeProfile(1).first()
        assertEquals("Kai", profile?.displayName)
    }

    @Test
    fun upsert_oppdaterer_eksisterende_profil() = runTest {
        val id = dao.upsert(UserProfileEntity(userId = 1, displayName = "Kai"))
        dao.upsert(UserProfileEntity(userId = 1, displayName = "Kai Roar"))

        val profile = dao.observeProfile(1).first()
        assertEquals("Kai Roar", profile?.displayName)
    }
}
