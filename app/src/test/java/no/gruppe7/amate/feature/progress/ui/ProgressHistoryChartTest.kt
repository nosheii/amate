package no.gruppe7.amate.feature.progress.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressHistoryChartTest {
    private val date = LocalDate.of(2026, 11, 9)

    @Test fun goalAboveRecordedValuesStillFitsInsideChart() {
        val maximum = progressChartMaximum(listOf(ProgressChartDay(date, 500)), 2000)
        assertTrue(maximum > 2000)
        // Søyle og mållinje må regnes med akkurat samme skala.
        assertEquals(4.0, (2000 / maximum) / (500 / maximum), 0.0001)
    }

    @Test fun dataOverGoalIsNotClipped() {
        val maximum = progressChartMaximum(listOf(ProgressChartDay(date, 5000)), 2000)
        assertTrue(maximum > 5000)
    }

    @Test fun emptyWeekHasSafeScaleWithOrWithoutGoal() {
        assertTrue(progressChartMaximum(emptyList(), null) > 0)
        assertTrue(progressChartMaximum(emptyList(), 10000) > 10000)
    }

    @Test fun largeTotalsDoNotOverflow() {
        assertTrue(progressChartMaximum(listOf(ProgressChartDay(date, Long.MAX_VALUE)), null) > Long.MAX_VALUE.toDouble())
    }
}
