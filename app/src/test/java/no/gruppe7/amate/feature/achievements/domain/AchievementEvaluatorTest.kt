package no.gruppe7.amate.feature.achievements.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Ren JVM-test uten Android. Reglene for merkene er det viktigste å ha kontroll på.
class AchievementEvaluatorTest {
    private val evaluator = AchievementEvaluator()

    @Test
    fun nothingLoaded_givesNoAchievements() {
        assertTrue(evaluator.earned(AchievementStats()).isEmpty())
    }

    @Test
    fun firstWaterEntry_givesFirstBadgeButNotTenLitres() {
        assertEquals(setOf("WATER_FIRST"), evaluator.earned(AchievementStats(waterTotalMl = 250)))
    }

    @Test
    fun exactThreshold_counts() {
        val earned = evaluator.earned(AchievementStats(waterTotalMl = 10_000, waterGoalDays = 3))
        assertEquals(setOf("WATER_FIRST", "WATER_10L", "WATER_GOAL_3"), earned)
    }

    @Test
    fun stepsSourceMissing_doesNotAffectOtherBadges() {
        // Skritt er null (lesefeil eller ikke lastet). Vann og gjøremål skal fortsatt telle.
        val earned = evaluator.earned(AchievementStats(waterTotalMl = 500, stepsBestDay = null, tasksCompleted = 1))
        assertEquals(setOf("WATER_FIRST", "TASKS_FIRST"), earned)
    }

    @Test
    fun progress_isBetweenZeroAndOneAndNullWhenUnknown() {
        val goal7 = AchievementCatalog.byCode("WATER_GOAL_7")!!
        // 2 av 7 dager. Float sammenlignes med en liten slingringsmonn.
        assertEquals(2f / 7f, evaluator.progress(goal7, AchievementStats(waterGoalDays = 2))!!, 0.001f)
        // Over terskelen stopper på 1, ikke 1,7.
        assertEquals(1f, evaluator.progress(goal7, AchievementStats(waterGoalDays = 12))!!, 0.001f)
        assertNull(evaluator.progress(goal7, AchievementStats()))
    }

    @Test
    fun catalogCodes_areUnique() {
        // To merker med samme kode ville blitt blandet sammen med det som er tatt.
        assertEquals(AchievementCatalog.all.size, AchievementCatalog.all.map { it.code }.toSet().size)
    }
}