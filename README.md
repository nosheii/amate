# Amate

Amate er en Android-app som hjelper brukere med å bygge gode selvomsorgsrutiner uten prestasjonspress. Appen er laget for personer som trenger støtte til å etablere hverdagsvaner, for eksempel ved sykdom, funksjonsnedsettelse eller en ny livssituasjon.

## Funksjoner

- **Profil og avatar.** Velg navn og en av fem håndtegnede avatarer ved første oppstart. Valgt avatar vises på hjemskjermen og kan endres i profilen.
- **Vann.** Registrer vann med hurtigvalg eller eget volum. Vannstreaken på hjemskjermen teller sammenhengende dager med minst én vannregistrering.
- **Skritt.** Registrer skritt manuelt eller bruk telefonens skrittsensor når den er tilgjengelig og appen har tillatelse.
- **Mål og fremgang.** Sett dagsmål for vann og skritt under **Innstillinger → Mine mål**, og se utviklingen de siste sju dagene på fremgangssiden.
- **Gjøremål.** Opprett og rediger egne gjøremål, og kryss av når de er fullført.
- **Påminnelser.** Velg mellom én og fem daglige påminnelser for vann og skritt, innenfor et tidsrom du velger.
- **Vær.** Se varsel fra MET Norway. Bruk omtrentlig posisjon med tillatelse, søk etter et sted selv, eller bruk Oslo som standardsted. [Slik fungerer værkortet](WEATHER.md).

## Lagring og personvern

Profil, avatar, mål, registreringer, gjøremål og påminnelsesinnstillinger lagres på enheten. Appen har ingen innlogging eller egen server. Android kan ta med appdata i sikkerhetskopien hvis sikkerhetskopiering er aktivert. Ved værhenting mottar MET koordinatene til stedet som vises. Ved manuelt stedsøk mottar Open-Meteo søketeksten. [Mer om hva værtjenestene mottar](WEATHER.md#kilde-og-personvern).

## Teknologi

- Kotlin
- Jetpack Compose og Material 3
- Room (lokal database)
- WorkManager (bakgrunnsjobber for påminnelser)

## Kom i gang

1. Klon repoet og åpne prosjektmappen i Android Studio.
2. Installer Android SDK 37 via SDK Manager. Bruk JDK-en som følger med Android Studio som Gradle JDK. Prosjektet er bygget med JDK 25.
3. Kjør `app` på en emulator eller en fysisk enhet med Android 8.0 (API 26) eller nyere. Vær og stedsøk krever nett, og de bruker ingen API-nøkkel.

Kjør bygg, enhetstester og lint fra prosjektmappen.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Debug-APK-en ligger i `app/build/outputs/apk/debug/app-debug.apk`. Kjør `./gradlew connectedDebugAndroidTest` med en emulator eller Android-enhet koblet til for å kjøre instrumenterte tester.

## Gruppe

Prosjektet er laget av gruppe 7 i MOB3000 (Applikasjonsutvikling for mobile enheter). Hvert medlem bygger sin egen "vertikale funksjon" (Compose-skjerm → ViewModel/UiState → Repository → DAO → Room-tabell):

- Nora Al-Tay – prosjektleder, vannregistrering og påminnelser (pilotfunksjon), avatarer tegnet i Procreate og integrasjon mot vær-API
- Ana María Araya Flores – profil og avatar-valg
- Najma Abdi – mål og egendefinerte gjøremål, koordinerer databaseklassen og navigasjonen
- Felix Torp-Nilsen – skritt og sensor

Se [prosjektstruktur og Git-arbeidsflyt](PROJECT_STRUCTURE.md).
