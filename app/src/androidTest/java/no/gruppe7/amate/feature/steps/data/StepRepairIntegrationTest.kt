package no.gruppe7.amate.feature.steps.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.goals.data.RoomGoalRepository
import no.gruppe7.amate.feature.goals.data.local.GoalType
import no.gruppe7.amate.feature.reminders.data.RoomReminderRepository
import no.gruppe7.amate.feature.reminders.data.local.ReminderEntity
import no.gruppe7.amate.feature.reminders.data.local.ReminderDeliveryEntity
import no.gruppe7.amate.feature.steps.data.local.StepReminderEntity
import no.gruppe7.amate.feature.steps.scheduling.StepReminderNotifier
import no.gruppe7.amate.feature.steps.scheduling.StepReminderRunner
import no.gruppe7.amate.feature.steps.sensor.StepSensorDataSource
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StepRepairIntegrationTest {
    private lateinit var db: AmateDatabase
    private lateinit var steps: RoomStepsRepository
    private lateinit var settings: RoomStepReminderRepository
    private val zone = ZoneId.of("Europe/Oslo")
    private val clock = Clock.fixed(Instant.parse("2026-11-18T13:00:00Z"), zone)
    private val date = "2026-11-18"
    private val day = java.time.LocalDate.parse(date).toEpochDay()
    private val notifier = FakeNotifier()

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AmateDatabase::class.java).build()
        steps = RoomStepsRepository(db.stepsDao(), object : StepSensorDataSource {
            override val stepEvents = emptyFlow<Int>()
            override val isSensorAvailable = false
        })
        settings = RoomStepReminderRepository(db.stepReminderDao())
    }
    @After fun cleanup() { db.close() }

    @Test fun concurrentSensorAndManualUpdatesKeepEveryStepAndSeparateUsers() = runBlocking<Unit> {
        (1..100).map { async(Dispatchers.IO) { steps.incrementStep(1, 3, day) } }.awaitAll()
        steps.incrementStep(2, 10, day)
        steps.incrementStep(1, 7, day - 1)
        assertEquals(300, steps.getStepsForToday(1, day).first())
        assertEquals(10, steps.getStepsForToday(2, day).first())
        assertEquals(7, steps.getStepsForToday(1, day - 1).first())
        assertTrue(runCatching { steps.incrementStep(1, -1, day) }.isFailure)
    }

    @Test fun stepEditorSavesItsOwnTableWithoutChangingWaterSettings() = runBlocking<Unit> {
        RoomReminderRepository(db.reminderDao()).save(ReminderEntity(1, true, 2, 480, 1200))
        val editor = StepReminderEditorRepository(settings)
        editor.save(ReminderEntity(1, true, 4, 540, 1260))
        assertEquals(4, settings.observeSettings(1).first().remindersPerDay)
        assertEquals(2, db.reminderDao().getSettings(1)!!.remindersPerDay)
        assertTrue(runCatching { editor.save(ReminderEntity(1, true, 0)) }.isFailure)
        assertTrue(runCatching { editor.save(ReminderEntity(1, true, 3, 1200, 480)) }.isFailure)
    }

    @Test fun scheduledStepsSendOnceWithTodayCountAndDoNotUseWaterQuota() = runBlocking<Unit> {
        settings.save(StepReminderEntity(1, true, 3, 480, 1200))
        steps.incrementStep(1, 321, day)
        db.reminderDeliveryDao().save(ReminderDeliveryEntity(1, date, 840, 3, clock.millis()))
        val runner = StepReminderRunner(db, notifier, clock)
        assertTrue(runner.run(1))
        assertFalse(runner.run(1))
        assertEquals(listOf(321), notifier.counts)
        assertEquals(1, db.stepReminderDeliveryDao().get(1, date)!!.sentCount)
        // Ny Runner etter en omstart leser samme huskelapp fra Room.
        assertFalse(StepReminderRunner(db, notifier, clock).run(1))
        assertEquals(3, db.reminderDeliveryDao().get(1, date)!!.sentCount)
    }

    @Test fun blockedNotificationsDisabledSettingsOutsideHoursAndReachedGoalDoNotSend() = runBlocking<Unit> {
        val runner = StepReminderRunner(db, notifier, clock)
        settings.save(StepReminderEntity(1, false))
        assertFalse(runner.run(1))
        settings.save(StepReminderEntity(1, true, 3, 480, 1200))
        notifier.allowed = false
        assertFalse(runner.run(1))
        notifier.allowed = true
        val night = Clock.fixed(Instant.parse("2026-11-18T22:00:00Z"), zone)
        assertFalse(StepReminderRunner(db, notifier, night).run(1))
        RoomGoalRepository(db.goalDao()).saveGoal(1, GoalType.STEPS, 1000)
        steps.incrementStep(1, 1000, day)
        assertFalse(runner.run(1))
        assertNull(db.stepReminderDeliveryDao().get(1, date))
        assertTrue(notifier.counts.isEmpty())
    }

    private class FakeNotifier : StepReminderNotifier {
        var allowed = true
        val counts = mutableListOf<Int>()
        override fun isAllowed() = allowed
        override fun show(userId: Long, stepsToday: Int) { counts += stepsToday }
        override fun cancel(userId: Long) {}
    }
}
