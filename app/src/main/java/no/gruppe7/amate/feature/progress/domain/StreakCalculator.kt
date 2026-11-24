package no.gruppe7.amate.feature.progress.domain

import java.time.LocalDate

internal fun waterStreakDays(daysWithWater: Set<LocalDate>, today: LocalDate): Int {
    // Dagen er fortsatt i gang, så gårsdagens rekke gjelder frem til midnatt.
    var date = if (today in daysWithWater) today else today.minusDays(1)
    var count = 0
    while (date in daysWithWater) {
        count++
        date = date.minusDays(1)
    }
    return count
}
