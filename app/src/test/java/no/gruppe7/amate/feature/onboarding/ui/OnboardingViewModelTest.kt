package no.gruppe7.amate.feature.onboarding.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.gruppe7.amate.feature.onboarding.data.OnboardingDraft
import no.gruppe7.amate.feature.onboarding.data.OnboardingRepository
import no.gruppe7.amate.feature.onboarding.data.OnboardingStatus
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val repository = FakeOnboardingRepository()
    private val store = ViewModelStore()
    private var nextKey = 0

    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(saved: SavedStateHandle = SavedStateHandle()) =
        OnboardingViewModel(repository, saved).also { store.put("setup${nextKey++}", it) }

    private fun profile(model: OnboardingViewModel) {
        model.changeName("  Nora  ")
        model.selectAvatar("kanin")
        model.next()
    }

    @Test fun freshUserStartsAtProfileWithoutWritingAnything() = runTest {
        val model = model(); runCurrent()
        assertEquals(OnboardingEntry.SETUP, model.uiState.value.entry)
        assertEquals(OnboardingStep.PROFILE, model.uiState.value.step)
        assertEquals("", model.uiState.value.avatarKey)
        assertEquals(0, repository.saves)
    }

    @Test fun completedUserGoesStraightToApp() = runTest {
        repository.status.value = OnboardingStatus(true, true, true)
        val model = model(); runCurrent()
        assertEquals(OnboardingEntry.READY, model.uiState.value.entry)
        assertEquals(0, repository.loads)
    }

    @Test fun existingValuesArePrefilledWithoutSaving() = runTest {
        repository.draft = OnboardingDraft("Ana", "frosk", 2500, 7000)
        val model = model(); runCurrent()
        assertEquals("Ana", model.uiState.value.name)
        assertEquals("frosk", model.uiState.value.avatarKey)
        assertEquals("2500", model.uiState.value.waterInput)
        assertEquals("7000", model.uiState.value.stepsInput)
        assertEquals(0, repository.saves)
    }

    @Test fun blankNameAndMissingOrUnknownAvatarCannotContinue() = runTest {
        val model = model(); runCurrent()
        model.changeName("   "); model.next()
        assertTrue(model.uiState.value.nameError)
        assertTrue(model.uiState.value.avatarError)
        model.changeName("Nora"); model.selectAvatar("elefant"); model.next()
        assertFalse(model.uiState.value.nameError)
        assertTrue(model.uiState.value.avatarError)
        model.changeName("a".repeat(41)); model.selectAvatar("hund"); model.next()
        assertTrue(model.uiState.value.nameError)
        assertEquals(OnboardingStep.PROFILE, model.uiState.value.step)
    }

    @Test fun badGoalValuesStayOnGoalsWithAnExplanation() = runTest {
        val model = model(); runCurrent(); profile(model)
        for (bad in listOf("0", "499", "10001", "1.5", "abc", "999999999999999")) {
            model.changeWater(bad); model.changeSteps("5000"); model.next()
            assertEquals(OnboardingStep.GOALS, model.uiState.value.step)
            assertTrue(model.uiState.value.waterError)
        }
        model.changeWater("2000"); model.changeSteps("999"); model.next()
        assertTrue(model.uiState.value.stepsError)
        assertEquals(0, repository.saves)
    }

    @Test fun validGoalsAreSavedOnlyAtTheEndAndNameIsTrimmed() = runTest {
        val model = model(); runCurrent(); profile(model)
        model.changeWater("2500"); model.changeSteps("6000"); model.next()
        assertEquals(OnboardingStep.PERMISSIONS, model.uiState.value.step)
        assertEquals(0, repository.saves)
        // Ingen tillatelse kreves for å fullføre profilen.
        model.next(); runCurrent()
        assertEquals(OnboardingDraft("Nora", "kanin", 2500, 6000), repository.saved)
        assertEquals(1L, repository.savedUserId)
        assertEquals(OnboardingEntry.READY, model.uiState.value.entry)
    }

    @Test fun blankGoalsAreOptional() = runTest {
        val model = model(); runCurrent(); profile(model)
        model.next(); model.next(); runCurrent()
        assertNull(repository.saved?.waterGoal)
        assertNull(repository.saved?.stepsGoal)
        assertEquals(OnboardingEntry.READY, model.uiState.value.entry)
    }

    @Test fun skippingGoalsDoesNotTryToSaveBadInputs() = runTest {
        val model = model(); runCurrent(); profile(model)
        model.changeWater("feil"); model.changeSteps("-1"); model.skipGoals(); model.next(); runCurrent()
        assertEquals(OnboardingDraft("Nora", "kanin"), repository.saved)
    }

    @Test fun goingBackKeepsInputsAndAllowsCorrectingGoals() = runTest {
        val model = model(); runCurrent(); profile(model)
        model.changeWater("2300"); model.skipGoals(); model.back()
        assertEquals("2300", model.uiState.value.waterInput)
        model.next(); model.next(); runCurrent()
        assertEquals(2300, repository.saved?.waterGoal)
    }

    @Test fun saveFailureKeepsChoicesAndCanBeRetried() = runTest {
        val model = model(); runCurrent(); profile(model); model.skipGoals()
        repository.failSave = true
        model.next(); runCurrent()
        assertEquals(OnboardingEntry.SETUP, model.uiState.value.entry)
        assertEquals(OnboardingStep.PERMISSIONS, model.uiState.value.step)
        assertTrue(model.uiState.value.saveFailed)
        assertFalse(model.uiState.value.isSaving)
        assertEquals("kanin", model.uiState.value.avatarKey)
        repository.failSave = false
        model.next(); runCurrent()
        assertEquals(OnboardingEntry.READY, model.uiState.value.entry)
    }

    @Test fun doubleTapDoesNotSaveTwiceOrGoBackDuringSave() = runTest {
        repository.holdSave = CompletableDeferred()
        val model = model(); runCurrent(); profile(model); model.skipGoals()
        model.next(); model.next(); model.back(); model.changeName("Feil"); runCurrent()
        assertEquals(1, repository.saves)
        assertEquals(OnboardingStep.PERMISSIONS, model.uiState.value.step)
        assertEquals("  Nora  ", model.uiState.value.name)
        repository.holdSave!!.complete(Unit); runCurrent()
        assertEquals(OnboardingEntry.READY, model.uiState.value.entry)
    }

    @Test fun startupFailureIsNotMistakenForANewUser() = runTest {
        repository.failLoad = true
        val model = model(); runCurrent()
        assertEquals(OnboardingEntry.ERROR, model.uiState.value.entry)
        repository.failLoad = false
        model.retry(); runCurrent()
        assertEquals(OnboardingEntry.SETUP, model.uiState.value.entry)
    }

    @Test fun readySignalDoesNotOpenAppUntilSaveHasReturnedSuccessfully() = runTest {
        repository.holdAfterFlag = CompletableDeferred()
        val model = model(); runCurrent(); profile(model); model.skipGoals()
        model.next(); runCurrent()
        assertEquals(OnboardingEntry.SETUP, model.uiState.value.entry)
        assertTrue(model.uiState.value.isSaving)
        repository.holdAfterFlag!!.complete(Unit); runCurrent()
        assertEquals(OnboardingEntry.READY, model.uiState.value.entry)
    }

    @Test fun savedDraftRestoresStepAndFieldsAfterRecreation() = runTest {
        val saved = SavedStateHandle()
        val first = model(saved); runCurrent(); profile(first)
        first.changeWater("2600"); first.next()
        val restored = model(saved); runCurrent()
        assertEquals(OnboardingStep.PERMISSIONS, restored.uiState.value.step)
        assertEquals("  Nora  ", restored.uiState.value.name)
        assertEquals("kanin", restored.uiState.value.avatarKey)
        assertEquals("2600", restored.uiState.value.waterInput)
    }

    @Test fun resetAndLaterProfileDeletionClearTheOldDraft() = runTest {
        repository.status.value = OnboardingStatus(true, true, true)
        val model = model(); runCurrent()
        repository.draft = OnboardingDraft("Gammel bruker", "katt", 2000, 5000)
        repository.resetCompletion(1); runCurrent()
        model.changeName("Gammelt utkast"); model.selectAvatar("hund"); model.next()
        repository.draft = OnboardingDraft()
        repository.status.value = OnboardingStatus(false, false, false); runCurrent()
        assertEquals(OnboardingStep.PROFILE, model.uiState.value.step)
        assertEquals("", model.uiState.value.name)
        assertEquals("", model.uiState.value.avatarKey)
    }
}

private class FakeOnboardingRepository : OnboardingRepository {
    val status = MutableStateFlow(OnboardingStatus(false, false, false))
    var draft = OnboardingDraft()
    var loads = 0
    var saves = 0
    var saved: OnboardingDraft? = null
    var savedUserId: Long? = null
    var failLoad = false
    var failSave = false
    var holdSave: CompletableDeferred<Unit>? = null
    var holdAfterFlag: CompletableDeferred<Unit>? = null
    override fun observeStatus(userId: Long) = flow {
        check(!failLoad)
        emitAll(status)
    }
    override suspend fun loadDraft(userId: Long): OnboardingDraft { loads++; return draft }
    override suspend fun complete(userId: Long, draft: OnboardingDraft) {
        saves++
        holdSave?.await()
        check(!failSave)
        saved = draft; savedUserId = userId; this.draft = draft
        status.value = OnboardingStatus(true, true, true)
        holdAfterFlag?.await()
    }
    override suspend fun resetCompletion(userId: Long) {
        status.value = status.value.copy(completed = false)
    }
}
