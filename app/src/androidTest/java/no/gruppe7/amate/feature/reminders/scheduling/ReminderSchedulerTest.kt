package no.gruppe7.amate.feature.reminders.scheduling

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.feature.reminders.data.local.ReminderEntity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderSchedulerTest {
    @Test fun savingAgainKeepsOneJobAndOffCancelsIt() = runBlocking {
        // Bruker ekte WorkManager, men et falskt varselobjekt. Vi tester bestillingen, ikke ventetiden.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = WorkManager.getInstance(context)
        val notifier = FakeWaterNotifier()
        val scheduler = WorkManagerReminderScheduler(context, notifier)
        // Egen testbruker: rører ikke påminnelsene til den vanlige bruker 1.
        val userId = 987654321L
        val name = WorkManagerReminderScheduler.workName(userId)
        try {
            val settings = ReminderEntity(userId, true)
            scheduler.synchronize(settings)
            val first = manager.getWorkInfosForUniqueWork(name).get().single { !it.state.isFinished }
            scheduler.synchronize(settings.copy(remindersPerDay = 5))
            // !isFinished finner aktive jobber. Samme ID som før viser at KEEP gjenbrukte jobben.
            val active = manager.getWorkInfosForUniqueWork(name).get().filter { !it.state.isFinished }
            assertEquals(1, active.size)
            assertEquals(first.id, active.single().id)
            scheduler.synchronize(settings.copy(enabled = false))
            assertTrue(manager.getWorkInfosForUniqueWork(name).get().all { it.state.isFinished })
            assertEquals(1, notifier.cancelled)
        } finally {
            // En test skal ikke etterlate en gjentakende jobb når den er ferdig.
            manager.cancelUniqueWork(name).result.get()
        }
    }
}
