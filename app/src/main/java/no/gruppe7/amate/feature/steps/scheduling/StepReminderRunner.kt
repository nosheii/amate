package no.gruppe7.amate.feature.steps.scheduling

import androidx.room.withTransaction
import java.time.Clock
import java.time.ZonedDateTime
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.reminders.data.local.ReminderDeliveryEntity
import no.gruppe7.amate.feature.reminders.scheduling.WaterReminderPlan
import no.gruppe7.amate.feature.steps.data.asReminderSettings
import no.gruppe7.amate.feature.steps.data.local.StepReminderDeliveryEntity

interface StepReminderNotifier {
    fun isAllowed(): Boolean
    fun show(userId: Long, stepsToday: Int)
    fun cancel(userId: Long)
}

class StepReminderRunner(
    private val database: AmateDatabase,
    private val notifier: StepReminderNotifier,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    suspend fun run(userId: Long): Boolean {
        if (!notifier.isAllowed()) return false
        return database.withTransaction {
            val settings = database.stepReminderDao().getSettings(userId) ?: return@withTransaction false
            val now = ZonedDateTime.now(clock)
            val date = now.toLocalDate()
            val dao = database.stepReminderDeliveryDao()
            val previous = dao.get(userId, date.toString())
            // Samme tidsregler som vann, men en egen logg hindrer at varslene bruker hverandres kvote.
            val log = previous?.let { ReminderDeliveryEntity(it.userId, it.localDate,
                it.lastSlotMinute, it.sentCount, it.lastAttemptMillis) }
            val slot = WaterReminderPlan.dueSlot(settings.asReminderSettings(), log, now)
                ?: return@withTransaction false
            val steps = database.stepsDao().getStepsForDayOneShot(userId, date.toEpochDay())?.stepCount ?: 0
            // Målet leses fra samme tabell som hjemskjermen; nådd mål trenger ingen flere påminnelser.
            val target = database.goalDao().findGoal(userId, no.gruppe7.amate.feature.goals.data.local.GoalType.STEPS)
                ?.takeIf { it.isActive && it.targetValue > 0 }
            if (target != null && steps >= target.targetValue) return@withTransaction false
            if (!notifier.isAllowed()) return@withTransaction false
            dao.save(StepReminderDeliveryEntity(userId, date.toString(), slot,
                (previous?.sentCount ?: 0) + 1, now.toInstant().toEpochMilli()))
            notifier.show(userId, steps)
            true
        }
    }
}
