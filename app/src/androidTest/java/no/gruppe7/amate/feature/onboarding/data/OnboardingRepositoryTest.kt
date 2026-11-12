package no.gruppe7.amate.feature.onboarding.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.goals.data.GoalRepository
import no.gruppe7.amate.feature.goals.data.RoomGoalRepository
import no.gruppe7.amate.feature.goals.data.local.GoalType
import no.gruppe7.amate.feature.profile.data.RoomProfileRepository
import no.gruppe7.amate.feature.profile.data.local.AvatarConfigEntity
import no.gruppe7.amate.feature.profile.data.local.UserProfileEntity
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingRepositoryTest {
    private lateinit var db: AmateDatabase
    private lateinit var profiles: RoomProfileRepository
    private lateinit var goals: RoomGoalRepository
    private lateinit var completion: MemoryCompletion
    private lateinit var repository: RoomOnboardingRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AmateDatabase::class.java).build()
        profiles = RoomProfileRepository(db.userProfileDao(), db.avatarConfigDao())
        goals = RoomGoalRepository(db.goalDao())
        completion = MemoryCompletion()
        repository = RoomOnboardingRepository(db, profiles, goals, completion)
    }
    @After fun tearDown() { db.close() }

    @Test fun profileAvatarAndGoalsUseExistingUserAndRepositories() = runBlocking<Unit> {
        repository.complete(1, OnboardingDraft("  Nora  ", "kanin", 2400, 7000))
        assertEquals("Nora", profiles.observeProfile(1).first()?.displayName)
        assertEquals("kanin", profiles.observeAvatar(1).first()?.avatarKey)
        assertEquals(2400, goals.observeGoal(1, GoalType.WATER).first()?.targetValue)
        assertEquals(7000, goals.observeGoal(1, GoalType.STEPS).first()?.targetValue)
        assertTrue(repository.observeStatus(1).first().completed)
        assertNull(profiles.observeProfile(2).first())
    }

    @Test fun skippingGoalsKeepsAgeOldGoalsAndOtherUsers() = runBlocking<Unit> {
        profiles.saveProfile(UserProfileEntity(1, "Gammelt navn", 24))
        profiles.saveProfile(UserProfileEntity(2, "Annen bruker", 30))
        profiles.saveAvatar(AvatarConfigEntity(1, "katt"))
        goals.saveGoal(1, GoalType.WATER, 2800)
        goals.saveGoal(2, GoalType.WATER, 1900)
        val draft = repository.loadDraft(1)
        assertEquals(OnboardingDraft("Gammelt navn", "katt", 2800), draft)
        repository.complete(1, OnboardingDraft("Nytt navn", "hund"))
        assertEquals(24, profiles.observeProfile(1).first()?.age)
        assertEquals(2800, goals.observeGoal(1, GoalType.WATER).first()?.targetValue)
        assertEquals("Annen bruker", profiles.observeProfile(2).first()?.displayName)
        assertEquals(1900, goals.observeGoal(2, GoalType.WATER).first()?.targetValue)
    }

    @Test fun failedSecondGoalRollsBackProfileAvatarAndFirstGoal() = runBlocking<Unit> {
        profiles.saveProfile(UserProfileEntity(1, "Før", 23))
        profiles.saveAvatar(AvatarConfigEntity(1, "bjørn"))
        goals.saveGoal(1, GoalType.WATER, 1800)
        val failingGoals = object : GoalRepository by goals {
            override suspend fun saveGoal(userId: Long, type: GoalType, targetValue: Int): Long {
                check(type != GoalType.STEPS) { "Test: lagring feilet" }
                return goals.saveGoal(userId, type, targetValue)
            }
        }
        val failing = RoomOnboardingRepository(db, profiles, failingGoals, completion)
        assertTrue(runCatching { failing.complete(1, OnboardingDraft("Etter", "hund", 2500, 5000)) }.isFailure)
        assertEquals(UserProfileEntity(1, "Før", 23), profiles.observeProfile(1).first())
        assertEquals("bjørn", profiles.observeAvatar(1).first()?.avatarKey)
        assertEquals(1800, goals.observeGoal(1, GoalType.WATER).first()?.targetValue)
        assertFalse(completion.done.value)
    }

    @Test fun failedCompletionFlagCanBeRetriedWithoutDuplicateGoals() = runBlocking<Unit> {
        val draft = OnboardingDraft("Nora", "frosk", 2300, 6000)
        completion.failWrite = true
        assertTrue(runCatching { repository.complete(1, draft) }.isFailure)
        assertFalse(repository.observeStatus(1).first().completed)
        completion.failWrite = false
        repository.complete(1, draft)
        assertTrue(repository.observeStatus(1).first().completed)
        assertEquals(2, goals.observeGoals(1).first().size)
    }

    @Test fun deletedProfileOverridesAnOldCompletionFlag() = runBlocking<Unit> {
        repository.complete(1, OnboardingDraft("Nora", "katt"))
        db.openHelper.writableDatabase.execSQL("DELETE FROM user_profile WHERE userId = 1")
        assertTrue(completion.done.value)
        val status = repository.observeStatus(1).first()
        assertFalse(status.completed)
        assertFalse(status.hasProfile)
    }

    @Test fun resetCompletionReopensSetupWithoutDeletingUserData() = runBlocking<Unit> {
        repository.complete(1, OnboardingDraft("Nora", "katt", 2000))
        repository.resetCompletion(1)
        assertFalse(repository.observeStatus(1).first().completed)
        assertEquals("Nora", profiles.observeProfile(1).first()?.displayName)
        assertEquals(2000, goals.observeGoal(1, GoalType.WATER).first()?.targetValue)
    }

    @Test fun invalidInputsDoNotWriteAnyProfile() = runBlocking<Unit> {
        for (draft in listOf(OnboardingDraft(" ", "katt"), OnboardingDraft("Nora", "elefant"),
            OnboardingDraft("Nora", "katt", 499), OnboardingDraft("Nora", "katt", stepsGoal = 50001))) {
            assertTrue(runCatching { repository.complete(1, draft) }.isFailure)
        }
        assertNull(profiles.observeProfile(1).first())
        assertFalse(completion.done.value)
    }

    @Test fun completionPreferenceSurvivesANewStoreInstance() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = AndroidOnboardingCompletionStore(context)
        val testUser = 987654321L
        try {
            first.setCompleted(testUser, true)
            val second = AndroidOnboardingCompletionStore(context)
            assertTrue(second.observeCompleted(testUser).first())
            second.setCompleted(testUser, false)
            assertFalse(first.observeCompleted(testUser).first())
        } finally { first.setCompleted(testUser, false) }
    }
}

private class MemoryCompletion : OnboardingCompletionStore {
    val done = MutableStateFlow(false)
    var failWrite = false
    override fun observeCompleted(userId: Long) = done
    override suspend fun setCompleted(userId: Long, completed: Boolean) {
        check(!failWrite)
        done.value = completed
    }
}
