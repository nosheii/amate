package no.gruppe7.amate.feature.profile.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.onboarding.data.OnboardingRepository

// Sletter all lagret informasjon fra Amate på brukerens tlf
// Sender brukeren tilbake til oppstartssiden igjen.
interface DeleteUserRepository {
    suspend fun deleteEverything(userId: Long)
}

class RoomDeleteUserRepository(
    private val database: AmateDatabase,
    private val onboarding: OnboardingRepository,
    // Sletter også innstillinger som ikke lagres i databasen.
    private val clearLocalSettings: () -> Unit = {},
) : DeleteUserRepository {
    override suspend fun deleteEverything(userId: Long) {
        // slettingen gjennomføres alltid, selv om skjermen byttes udnerveis.
        withContext(NonCancellable) {
            // Sletter i bakgrunnen, så appen ikke henger.
            withContext(Dispatchers.IO) {
                database.clearAllTables()
                clearLocalSettings()
            }
            // Etter "oppstart fullført" spør appen om navn og avatarvalg.
            onboarding.resetCompletion(userId)
        }
    }
}