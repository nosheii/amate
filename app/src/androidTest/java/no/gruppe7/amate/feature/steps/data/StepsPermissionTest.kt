package no.gruppe7.amate.feature.steps.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.steps.sensor.StepSensorDataSource
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StepsPermissionTest {
    @Test fun sensorOnlyRunsAfterPermissionAndCanRestartAfterALaterYes() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AmateDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val events = MutableSharedFlow<Int>(extraBufferCapacity = 8)
        val sensor = object : StepSensorDataSource {
            override val isSensorAvailable = true
            override val stepEvents = events
        }
        try {
            val repository = RoomStepsRepository(db.stepsDao(), sensor, scope)
            assertEquals(0, events.subscriptionCount.value)
            repository.setSensorPermission(false)
            assertEquals(0, events.subscriptionCount.value)
            repository.setSensorPermission(true)
            withTimeout(5000) { events.subscriptionCount.first { it == 1 } }
            // Flere like tillatelsessvar skal ikke starte flere lyttere.
            repository.setSensorPermission(true)
            assertEquals(1, events.subscriptionCount.value)
            events.emit(3)
            withTimeout(5000) { repository.getStepsForToday(1).first { it == 3 } }
            repository.setSensorPermission(false)
            withTimeout(5000) { events.subscriptionCount.first { it == 0 } }
            repository.setSensorPermission(true)
            withTimeout(5000) { events.subscriptionCount.first { it == 1 } }
            events.emit(4)
            withTimeout(5000) { repository.getStepsForToday(1).first { it == 7 } }
        } finally { scope.cancel(); db.close() }
    }
}
