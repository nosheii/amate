package no.gruppe7.amate.feature.reminders.scheduling

import java.time.ZoneId
import java.time.ZonedDateTime
import no.gruppe7.amate.feature.reminders.data.local.ReminderDeliveryEntity
import no.gruppe7.amate.feature.reminders.data.local.ReminderEntity
import org.junit.Assert.*
import org.junit.Test

// Disse testene kjører på PC-en. Vi sender inn dato og tid selv, uten emulator eller venting.
// assertEquals sammenligner forventet og faktisk verdi. assertNull forventer «ikke send».
class WaterReminderPlanTest {
    // Felles eksempel: bruker 1 ønsker tre påminnelser mellom 08:00 og 20:00.
    private val settings = ReminderEntity(1, true, 3, 480, 1200)
    private fun now(hour: Int, minute: Int = 0) =
        ZonedDateTime.of(2026, 10, 19, hour, minute, 0, 0, ZoneId.of("Europe/Oslo"))
    private fun previous(slot: Int = 600, count: Int = 1) =
        ReminderDeliveryEntity(1, "2026-10-19", slot, count, now(10).toInstant().toEpochMilli())

    @Test fun distributesChosenCountEvenlyInsideWindow() {
        assertEquals(listOf(600, 840, 1080), WaterReminderPlan.slotMinutes(settings))
        assertEquals(listOf(840), WaterReminderPlan.slotMinutes(settings.copy(remindersPerDay = 1)))
        for (count in 1..5) {
            val slots = WaterReminderPlan.slotMinutes(settings.copy(remindersPerDay = count))
            assertEquals(count, slots.size)
            assertTrue(slots.all { it in 480 until 1200 })
        }
    }

    @Test fun offBeforeFirstSlotAndOutsideWindowDoNotSend() {
        assertNull(WaterReminderPlan.dueSlot(settings.copy(enabled = false), null, now(14)))
        assertNull(WaterReminderPlan.dueSlot(settings, null, now(7)))
        assertNull(WaterReminderPlan.dueSlot(settings, null, now(9, 59)))
        assertNull(WaterReminderPlan.dueSlot(settings, null, now(20)))
        assertNull(WaterReminderPlan.dueSlot(settings, null, now(23)))
    }

    @Test fun delayedJobOnlySelectsLatestDueSlotAndDoesNotRepeatIt() {
        // Klokken 17 skal vi bare ta siste passerte tidspunkt, ikke ta igjen hele dagen.
        assertEquals(840, WaterReminderPlan.dueSlot(settings, null, now(17)))
        assertNull(WaterReminderPlan.dueSlot(settings, previous(840), now(17)))
        assertEquals(1080, WaterReminderPlan.dueSlot(settings, previous(840), now(18)))
    }

    @Test fun changingTimeCountAndTogglingDoesNotResetTodaysLimit() {
        assertNull(WaterReminderPlan.dueSlot(settings, previous(count = 3), now(18)))
        assertNull(WaterReminderPlan.dueSlot(settings.copy(remindersPerDay = 1), previous(), now(18)))
        assertNull(WaterReminderPlan.dueSlot(settings.copy(startMinute = 600), previous(1100), now(19)))
    }

    @Test fun newLocalDayGetsANewPlan() {
        assertEquals(600, WaterReminderPlan.dueSlot(settings, previous(count = 3), now(10).plusDays(1)))
    }

    @Test fun localTimeWorksAcrossDstAndUsesPhonesTimeZone() {
        // I Oslo går klokken fra sommertid til vintertid 25. oktober dette året.
        val beforeDst = now(10).withDayOfMonth(24)
        val afterDst = beforeDst.plusDays(1)
        assertNotEquals(beforeDst.offset, afterDst.offset)
        assertEquals(600, WaterReminderPlan.dueSlot(settings, null, beforeDst))
        assertEquals(600, WaterReminderPlan.dueSlot(settings, null, afterDst))
        // Samme øyeblikk er natt i Los Angeles, og skal ikke gi vannvarsel der.
        assertNull(WaterReminderPlan.dueSlot(settings, null,
            afterDst.withZoneSameInstant(ZoneId.of("America/Los_Angeles"))))
    }

    @Test fun repeatedHourAndClockMovedBackDoNotRepeatSlot() {
        // 02:30 finnes to ganger når klokken stilles tilbake. Det skal fortsatt bare bli ett forsøk.
        val repeated = ZonedDateTime.parse("2026-10-25T02:30:00+02:00[Europe/Oslo]")
        val early = settings.copy(remindersPerDay = 1, startMinute = 120, endMinute = 180)
        val log = ReminderDeliveryEntity(1, "2026-10-25", 150, 1, repeated.toInstant().toEpochMilli())
        assertNull(WaterReminderPlan.dueSlot(early, log, repeated.withLaterOffsetAtOverlap()))
        assertNull(WaterReminderPlan.dueSlot(settings, previous(), now(9)))
    }

    @Test fun invalidOrTinyWindowsCannotCreateInvalidOrDuplicateMinutes() {
        assertTrue(WaterReminderPlan.slotMinutes(settings.copy(remindersPerDay = 0)).isEmpty())
        assertTrue(WaterReminderPlan.slotMinutes(settings.copy(endMinute = 480)).isEmpty())
        assertTrue(WaterReminderPlan.slotMinutes(settings.copy(endMinute = 1440)).isEmpty())
        assertEquals(listOf(480), WaterReminderPlan.slotMinutes(settings.copy(endMinute = 481, remindersPerDay = 5)))
    }
}
