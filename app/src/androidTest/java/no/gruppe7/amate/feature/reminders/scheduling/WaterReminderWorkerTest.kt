package no.gruppe7.amate.feature.reminders.scheduling

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WaterReminderWorkerTest {
    @Test fun missingUserFailsSafely() = runBlocking {
        // Testbyggeren oppretter Worker direkte, så vi slipper å vente på WorkManagers intervall.
        val worker = TestListenableWorkerBuilder<WaterReminderWorker>(ApplicationProvider.getApplicationContext<Context>()).build()
        assertEquals(ListenableWorker.Result.failure(), worker.doWork())
    }

    @Test fun userWithoutSettingsFinishesWithoutNotification() = runBlocking {
        // En gyldig testbruker uten lagrede valg skal bare bli hoppet over, ikke gi feil eller varsel.
        val worker = TestListenableWorkerBuilder<WaterReminderWorker>(ApplicationProvider.getApplicationContext<Context>())
            .setInputData(workDataOf(WaterReminderWorker.USER_ID to 987654322L)).build()
        assertEquals(ListenableWorker.Result.success(), worker.doWork())
    }
}
