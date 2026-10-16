# Vannfunksjonen – første UI-versjon

Appen åpner nå en enkel vannoversikt. Den bruker samme Room-database som før.
Farger og kort er inspirert av mockupen, men avatar, vær, pollen, skritt og felles
navigasjon er ikke implementert her.

## Hva skjer når jeg trykker «+250 ml»?

1. `WaterScreen` sender knappetrykket til `WaterViewModel`.
2. ViewModel ber `WaterRepository` lagre en registrering med dagens tidspunkt.
3. Repository validerer mengden og sender registreringen til `WaterDao`.
4. Room lagrer raden og sender en oppdatert liste gjennom `Flow`.
5. ViewModel oppdaterer `WaterUiState`. Skjermen viser ny liste, sum og måler.

Summen regnes fra dagens liste, så måleren og listen bruker samme data.
Redigering endrer bare mengden og beholder ID, bruker og tidspunkt.
Redigeringsfeltet glir ned inne i den valgte registreringen. «Lagre» oppdaterer
Room, mens «Avbryt» lukker feltet uten endring. Ny egen mengde bruker samme felt
under «Legg til egen mengde». Bare bekreftelse av sletting bruker popup.
Sletting krever en bekreftelse. Ved lagringsfeil beholdes det brukeren skrev.

## Midlertidige valg

- Bruker-ID er `1` til profilfunksjonen leverer den aktive brukerens ID.
- `2 000 ml` er kun et merket **visningsmål**, ikke en personlig anbefaling.
  Målet lagres ikke i en konkurrerende måltabell. Najmas mål-funksjon kan levere
  verdien senere.
- Mengden må være et helt tall fra 1 til 10 000 ml per registrering. Øvre grense
  beskytter mot feiltasting; den er ikke et daglig mål.
- Dagen følger telefonens lokale dato og tidssone. Døgn med sommer-/vintertid
  beregnes fra starten av datoen til starten av neste dato, ikke som faste 24 timer.
- Dagen kontrolleres når skjermen åpnes igjen og hvert 30. sekund mens ViewModel
  lever. Gårsdagens data beholdes i databasen.
- `MainActivity` viser `WaterRoute` direkte. Senere kan Najma plassere samme route
  i felles navigasjon. `HomeScreen`, navigasjonsfiler og felles designsystem er urørt.

## Prøve selv

1. Kjør `app` på emulator eller telefon.
2. Trykk `+250 ml` og `+500 ml`. Dagens total skal bli 750 ml hvis dagen var tom.
3. Velg «Legg til egen mengde», prøv `0`, og rett deretter til `375`.
4. Rediger en registrering. Både mengden på raden og dagens total skal endres.
5. Slett en registrering. Prøv også å avbryte slettingen.
6. Lukk og start appen igjen. Registreringene skal fremdeles være der.

## Automatiske tester

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Med emulator tilkoblet:

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=no.gruppe7.amate.feature.water.data.local.WaterDaoTest,no.gruppe7.amate.feature.water.ui.WaterScreenTest
```

- ViewModel: hurtigregistrering, validering, redigering, bekreftet sletting,
  lese-/skrivefeil, dagskifte, tidssone, sommertid og brukerfiltrering.
- Room: CRUD, dagsgrensene, andre brukere, tom dag og lagring etter gjenåpning.
- UI: registrere → redigere uten popup → slette gjennom ekte Room, validering og
  avbryte en redigering uten å endre lagrede data.

UI- og Room-testene bruker egne testdatabaser, ikke brukerens vanlige vannlogg.
