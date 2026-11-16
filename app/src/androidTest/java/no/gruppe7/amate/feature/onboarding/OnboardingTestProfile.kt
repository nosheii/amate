package no.gruppe7.amate.feature.onboarding

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import no.gruppe7.amate.AmateApplication
import no.gruppe7.amate.feature.onboarding.data.AndroidOnboardingCompletionStore
import no.gruppe7.amate.feature.onboarding.data.OnboardingDraft
import no.gruppe7.amate.feature.profile.data.local.AvatarConfigEntity
import no.gruppe7.amate.feature.profile.data.local.UserProfileEntity

// Testhjelp for ekte MainActivity. Den tar vare på og gjenoppretter bare testbrukerens profil.
// Emulator-testene kjøres i en separat app-ID, ikke over brukerens vanlige appdata.
class OnboardingTestProfile(context: Context) {
    val container = (context.applicationContext as AmateApplication).container
    private val completion = AndroidOnboardingCompletionStore(context)
    private var profile: UserProfileEntity? = null
    private var avatar: AvatarConfigEntity? = null
    private var completed = false

    suspend fun capture() {
        profile = container.profileRepository.observeProfile(1).first()
        avatar = container.profileRepository.observeAvatar(1).first()
        completed = completion.observeCompleted(1).first()
    }

    suspend fun ready() = container.onboardingRepository.complete(1, OnboardingDraft("Testbruker", "katt"))

    suspend fun clearProfile() {
        container.onboardingRepository.resetCompletion(1)
        container.database.withTransaction {
            container.database.openHelper.writableDatabase.execSQL("DELETE FROM user_profile WHERE userId = 1")
            container.database.openHelper.writableDatabase.execSQL("DELETE FROM avatar_config WHERE userId = 1")
        }
    }

    suspend fun restore() {
        container.database.withTransaction {
            profile?.let { container.profileRepository.saveProfile(it) }
                ?: container.database.openHelper.writableDatabase.execSQL("DELETE FROM user_profile WHERE userId = 1")
            avatar?.let { container.profileRepository.saveAvatar(it) }
                ?: container.database.openHelper.writableDatabase.execSQL("DELETE FROM avatar_config WHERE userId = 1")
        }
        completion.setCompleted(1, completed)
    }
}
