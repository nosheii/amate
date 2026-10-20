package no.gruppe7.amate.feature.reminders.ui

// Dette er et utkast mens brukeren redigerer. Room endres først når «Lagre» trykkes.
// Skjermen får én slik pakke med alt den trenger å vise. copy() lager en oppdatert pakke.
data class RemindersUiState(
    // Disse fire feltene er valgene i editoren, og kan være annerledes enn det som er lagret.
    val enabled: Boolean = false,
    val remindersPerDay: Int = 3,
    val startTime: String = "08:00",
    val endTime: String = "21:00",
    // Telefonens faktiske tillatelse, ikke det samme som bryteren enabled.
    val notificationsAllowed: Boolean = false,
    // Brukes til ventetegn og til å hindre redigering mens vi henter eller lagrer data.
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    // Ved innlastingsfeil viser vi Prøv igjen, i stedet for å la standardvalg overskrive lagrede valg.
    val loadFailed: Boolean = false,
    // Har brukeren redigert siden sist lagring/Avbryt? Styrer blant annet Lagre-knappen.
    val hasChanges: Boolean = false,
    // ID til en tekst i strings.xml. null betyr at vi ikke har noen feilmelding å vise.
    val errorMessageResId: Int? = null,
    // Styrer bekreftelsen «Innstillingene er lagret», ikke om påminnelser er på eller av.
    val saved: Boolean = false,
)
