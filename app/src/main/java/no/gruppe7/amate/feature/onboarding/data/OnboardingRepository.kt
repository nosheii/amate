package no.gruppe7.amate.feature.onboarding.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.goals.data.GoalRepository
import no.gruppe7.amate.feature.goals.data.local.GoalType
import no.gruppe7.amate.feature.goals.data.local.validRange
import no.gruppe7.amate.feature.profile.data.AvatarKeys
import no.gruppe7.amate.feature.profile.data.ProfileRepository
import no.gruppe7.amate.feature.profile.data.local.AvatarConfigEntity
import no.gruppe7.amate.feature.profile.data.local.UserProfileEntity

const val ONBOARDING_NAME_MAX_LENGTH = 40

data class OnboardingDraft(
    val name: String = "",
    val avatarKey: String = "",
    val waterGoal: Int? = null,
    val stepsGoal: Int? = null,
)

data class OnboardingStatus(val completed: Boolean, val hasProfile: Boolean, val hasAvatar: Boolean)

interface OnboardingRepository {
    fun observeStatus(userId: Long): Flow<OnboardingStatus>
    suspend fun loadDraft(userId: Long): OnboardingDraft
    suspend fun complete(userId: Long, draft: OnboardingDraft)
    // Ana Maria kan bruke denne ved «start på nytt». Den sletter IKKE brukerdata i seg selv.
    suspend fun resetCompletion(userId: Long)
}

class RoomOnboardingRepository(
    private val database: AmateDatabase,
    private val profiles: ProfileRepository,
    private val goals: GoalRepository,
    private val completion: OnboardingCompletionStore,
) : OnboardingRepository {
    override fun observeStatus(userId: Long): Flow<OnboardingStatus> = combine(
        completion.observeCompleted(userId), profiles.observeProfile(userId), profiles.observeAvatar(userId),
    ) { completed, profile, avatar ->
        // En gammel huskelapp skal ikke slippe oss inn hvis profilen senere blir slettet.
        OnboardingStatus(completed && !profile?.displayName.isNullOrBlank() && avatar?.avatarKey in AvatarKeys.all,
            hasProfile = profile != null, hasAvatar = avatar != null)
    }.distinctUntilChanged()

    override suspend fun loadDraft(userId: Long): OnboardingDraft {
        val profile = profiles.observeProfile(userId).first()
        val avatar = profiles.observeAvatar(userId).first()
        val savedGoals = goals.observeGoals(userId).first()
        return OnboardingDraft(
            name = profile?.displayName.orEmpty(),
            avatarKey = avatar?.avatarKey?.takeIf { it in AvatarKeys.all }.orEmpty(),
            waterGoal = savedGoals.firstOrNull { it.type == GoalType.WATER && it.isActive }?.targetValue,
            stepsGoal = savedGoals.firstOrNull { it.type == GoalType.STEPS && it.isActive }?.targetValue,
        )
    }

    override suspend fun complete(userId: Long, draft: OnboardingDraft) {
        val name = draft.name.trim()
        require(name.isNotBlank() && name.length <= ONBOARDING_NAME_MAX_LENGTH)
        require(draft.avatarKey in AvatarKeys.all)
        require(draft.waterGoal == null || draft.waterGoal in GoalType.WATER.validRange)
        require(draft.stepsGoal == null || draft.stepsGoal in GoalType.STEPS.validRange)
        val previousProfile = profiles.observeProfile(userId).first()
        // Alt i Room lagres samlet. Feiler ett mål, skal vi ikke sitte igjen med en halv profil.
        database.withTransaction {
            profiles.saveProfile(UserProfileEntity(userId, name, previousProfile?.age))
            profiles.saveAvatar(AvatarConfigEntity(userId, draft.avatarKey))
            // Å hoppe over et mål betyr «ikke endre det», ikke «slett et eksisterende mål».
            draft.waterGoal?.let { goals.saveGoal(userId, GoalType.WATER, it) }
            draft.stepsGoal?.let { goals.saveGoal(userId, GoalType.STEPS, it) }
        }
        // Først etter vellykket lagring åpner vi hjem. Et nytt forsøk er trygt ved feil her også.
        completion.setCompleted(userId, true)
    }

    override suspend fun resetCompletion(userId: Long) = completion.setCompleted(userId, false)
}
