package no.gruppe7.amate.feature.reminders.scheduling

import java.time.ZonedDateTime
import no.gruppe7.amate.feature.reminders.data.local.ReminderDeliveryEntity
import no.gruppe7.amate.feature.reminders.data.local.ReminderEntity

// Ren Kotlin-logikk: trenger verken Android eller en ekte klokke for å testes.
// object betyr at vi bruker ett felles objekt, uten å lage WaterReminderPlan() hver gang.
object WaterReminderPlan {
    // Returnerer planlagte minutter i stigende rekkefølge, ikke ferdige Android-varsler.
    fun slotMinutes(settings: ReminderEntity): List<Int> {
        // Ekstra sikkerhet: ugyldige valg gir en tom plan i stedet for feilregning eller krasj.
        if (settings.remindersPerDay !in 1..5 || settings.startMinute !in 0..1439 ||
            settings.endMinute !in 0..1439 || settings.startMinute >= settings.endMinute) return emptyList()
        // width er lengden på hele tidsrommet, målt i minutter.
        val width = settings.endMinute - settings.startMinute
        // Legger ett tidspunkt midt i hver like store del av tidsrommet.
        // 3 påminnelser mellom 08 og 20 blir omtrent 10, 14 og 18.
        // index starter på 0. Regnestykket finner midten av del 0, del 1 osv.
        // Heltallsdeling runder ned til et helt minutt. distinct fjerner like minutter
        // dersom brukeren har valgt et veldig kort tidsrom med mange påminnelser.
        return (0 until settings.remindersPerDay).map { index ->
            settings.startMinute + width * (2 * index + 1) / (2 * settings.remindersPerDay)
        }.distinct()
    }

    // Finner ett tidspunkt vi kan bruke nå. null betyr «ikke send noe denne gangen».
    // previous er huskelappen fra Room, mens now er dato/tid med telefonens tidssone.
    fun dueSlot(settings: ReminderEntity, previous: ReminderDeliveryEntity?, now: ZonedDateTime): Int? {
        if (!settings.enabled) return null
        // Gjør for eksempel 14:30 om til 870, så det er lett å sammenligne tidene.
        val minute = now.hour * 60 + now.minute
        // Fra er med i vinduet, Til er ikke med. Gamle varsler skal ikke komme om natten.
        if (minute < settings.startMinute || minute >= settings.endMinute) return null
        // ?. kjører bare hvis huskelappen finnes. takeIf beholder den bare hvis datoen stemmer.
        val today = previous?.takeIf { it.localDate == now.toLocalDate().toString() }
        // Endrede valg eller en klokke som stilles tilbake skal ikke gi ekstra varsler.
        // Av/på eller et nytt antall nullstiller altså ikke forsøkene vi allerede har gjort i dag.
        if (today != null && (today.sentCount >= settings.remindersPerDay ||
                now.toInstant().toEpochMilli() <= today.lastAttemptMillis)) return null
        // Hvis klokken er 17 og både 10 og 14 er passert, velger vi bare 14.
        // Før dagens første tidspunkt finnes ingen slik verdi, og vi avslutter med null.
        val latest = slotMinutes(settings).lastOrNull { it <= minute } ?: return null
        // Hopper over gamle tidspunkt i stedet for å sende flere varsler på én gang.
        // Et tidspunkt må være nyere enn det vi brukte sist. Uten huskelapp bruker vi -1.
        return latest.takeIf { it > (today?.lastSlotMinute ?: -1) }
    }
}
