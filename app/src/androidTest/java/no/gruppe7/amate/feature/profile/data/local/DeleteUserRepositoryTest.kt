package no.gruppe7.amate.feature.profile.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import no.gruppe7.amate.core.database.AmateDatabase
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.feature.onboarding.data.OnboardingDraft
import no.gruppe7.amate.feature.onboarding.data.OnboardingRepository
import no.gruppe7.amate.feature.onboarding.data.OnboardingStatus
import no.gruppe7.amate.feature.profile.data.RoomDeleteUserRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class DeleteUserRepositoryTest {
    private lateinit var database: AmateDatabase

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AmateDatabase::class.java).build()
    }
    @After
    fun teardown() {
        database.close()
    }
    @Test
    fun deleteEverything_ClearDatabaseAndResetOnboarding() = runBlocking {
        database.userProfileDao().upsert(UserProfileEntity(userId = 1, displayName = "Kai"))
        database.avatarConfigDao().upsert(AvatarConfigEntity(userId = 1, avatarKey = "hund"))

        assertNotNull(database.userProfileDao().observeProfile(1).first())
        assertNotNull(database.avatarConfigDao().observeAvatarConfig(1).first())

        val onboarding = FakeOnboarding()
        RoomDeleteUserRepository(database, onboarding).deleteEverything(1)

        assertNull(database.userProfileDao().observeProfile(1).first())
        assertNull(database.avatarConfigDao().observeAvatarConfig(1).first())
        assertEquals(1L, onboarding.resetFor)
    }
    @Test
    fun deleteEverything_ClearSettingsOutsideDatabase() = runBlocking {
        var cleared = false
        RoomDeleteUserRepository(database, FakeOnboarding()) { cleared = true }.deleteEverything(1)

        assertTrue(cleared)
    }
}

private class FakeOnboarding : OnboardingRepository {
    var resetFor: Long? = null
    override fun observeStatus(userId: Long): Flow<OnboardingStatus> = emptyFlow()
    override suspend fun loadDraft(userId: Long) = OnboardingDraft()
    override suspend fun complete(userId: Long, draft: OnboardingDraft) {}
    override suspend fun resetCompletion(userId: Long) { resetFor = userId }
}