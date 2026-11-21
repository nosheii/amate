package no.gruppe7.amate.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import no.gruppe7.amate.feature.profile.data.local.AvatarConfigEntity
import no.gruppe7.amate.feature.profile.data.local.UserProfileDao
import no.gruppe7.amate.feature.goals.data.local.GoalDao
import no.gruppe7.amate.feature.goals.data.local.GoalEntity
import no.gruppe7.amate.feature.profile.data.local.AvatarConfigDao
import no.gruppe7.amate.feature.profile.data.local.UserProfileEntity
import no.gruppe7.amate.feature.tasks.data.local.SelfCareTaskEntity
import no.gruppe7.amate.feature.tasks.data.local.SelfCareTaskDao
import no.gruppe7.amate.feature.water.data.local.WaterDao
import no.gruppe7.amate.feature.water.data.local.WaterEntryEntity
import no.gruppe7.amate.feature.reminders.data.local.ReminderDao
import no.gruppe7.amate.feature.reminders.data.local.ReminderEntity
import no.gruppe7.amate.feature.reminders.data.local.ReminderDeliveryEntity
import no.gruppe7.amate.feature.reminders.data.local.ReminderDeliveryDao
import no.gruppe7.amate.feature.steps.data.local.DailyStepsEntity
import no.gruppe7.amate.feature.steps.data.local.StepsDao
import no.gruppe7.amate.feature.steps.data.local.StepReminderEntity
import no.gruppe7.amate.feature.steps.data.local.StepReminderDao

@Database(
    entities = [
        UserProfileEntity::class,
        AvatarConfigEntity::class,
        WaterEntryEntity::class,
        ReminderEntity::class,
        ReminderDeliveryEntity::class,
        GoalEntity::class,
        SelfCareTaskEntity::class,
        DailyStepsEntity::class,
        StepReminderEntity::class,
    ],

    version = 7,
    exportSchema = false,
)
abstract class LegacyStepDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao

    abstract fun avatarConfigDao(): AvatarConfigDao

    abstract fun waterDao(): WaterDao

    abstract fun goalDao(): GoalDao

    abstract fun selfCareTaskDao(): SelfCareTaskDao

    abstract fun reminderDao(): ReminderDao

    abstract fun reminderDeliveryDao(): ReminderDeliveryDao

    abstract fun stepsDao(): StepsDao

    abstract fun stepReminderDao(): StepReminderDao
}
