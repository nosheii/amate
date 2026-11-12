package no.gruppe7.amate.feature.onboarding.data

import android.content.Context
import android.annotation.SuppressLint
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

interface OnboardingCompletionStore {
    fun observeCompleted(userId: Long): Flow<Boolean>
    suspend fun setCompleted(userId: Long, completed: Boolean)
}

// Bare «oppstart fullført» ligger her. Navn, avatar og mål ligger fortsatt i Room.
class AndroidOnboardingCompletionStore(context: Context) : OnboardingCompletionStore {
    private val preferences = context.applicationContext.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
    private fun key(userId: Long) = "completed_$userId"

    override fun observeCompleted(userId: Long): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key(userId)) trySend(preferences.getBoolean(key(userId), false))
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(preferences.getBoolean(key(userId), false))
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    @SuppressLint("UseKtx") // Vi trenger commit sitt svar om lagringen lyktes, ikke KTX som returnerer Unit.
    override suspend fun setCompleted(userId: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        // commit gir oss beskjed hvis lagringen feiler. Da skal vi ikke late som oppstarten er ferdig.
        val previous = preferences.getBoolean(key(userId), false)
        if (!preferences.edit().putBoolean(key(userId), completed).commit()) {
            // SharedPreferences endrer minnet før disken. Rull tilbake også den verdien ved feil.
            preferences.edit().putBoolean(key(userId), previous).commit()
            error("Oppstarten kunne ikke lagres")
        }
    }
}
