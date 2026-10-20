# Vannpåminnelser – innstillinger og bakgrunnsvarsler

Vannpåminnelsene har nå både Room-lagring, et innstillingsfelt og bakgrunnsvarsler.
Innstillingene ligger midlertidig på vannskjermen. Å flytte dem til en egen side
bak et tannhjul på hjemskjermen blir en egen oppgave, uten å skrive om lagringen.

## Slik henger filene sammen

1. `ReminderSettingsScreen.kt` viser bryteren, antall og Fra/Til. `ReminderSettingsRoute`
   i samme fil kobler skjermen til ViewModel og Androids varslingstillatelse.
2. `RemindersUiState.kt` inneholder det brukeren ser og redigerer akkurat nå.
3. `RemindersViewModel.kt` holder et utkast. «Lagre innstillinger» validerer og lagrer;
   «Avbryt» henter tilbake de siste lagrede valgene. Å bare skjule feltet beholder
   utkastet, og kortet viser at det finnes ulagrede endringer.
4. `ReminderRepository.kt` er mellomleddet til Room og kontrollerer også gyldige verdier.
5. `ReminderDao.kt` leser én brukers innstillinger og lagrer med `@Upsert`, slik at
   samme bruker ikke får en ny rad hver gang.
6. `ReminderEntity.kt` beskriver tabellen `water_reminder_settings`.

En ny bruker starter med påminnelser av, ønsket antall 3 og tidsrom 08:00–21:00.
Standardvalgene vises uten å opprette en rad før brukeren lagrer.
Tidspunktene vises som TT:MM og lagres som minutter fra midnatt.
Denne første versjonen støtter tidsrom innenfor samme dag: Fra må være før Til.
Bruker-ID er fortsatt 1, som i vannfunksjonen, frem til profilen kobles på.

## Databasen oppgraderes uten å slette vannloggen

`AmateDatabase.kt` har nå versjon 3. Migrering 1 → 2 legger til innstillingene,
og 2 → 3 legger til en liten sendelogg. Eksisterende vann og valg beholdes.
`AppContainer.kt` registrerer migreringen og oppretter begge repositories med samme
database. Det brukes ingen destruktiv fallback.

`DatabaseMigrationTest.kt` lager en ekte versjon-1-database med vannregistrering,
oppgraderer den med samme migrering som appen, og sjekker at vannet fortsatt finnes.
Den tester også oppgradering fra versjon 2 med bevarte innstillinger.

## Slik fungerer påminnelsene

1. `AmateApplication.kt` lytter til **lagrede** valg i Room gjennom hele app-prosessen.
   Ulagrede endringer på skjermen starter eller stopper ingenting.
2. `ReminderScheduler.kt` oppretter én unik WorkManager-jobb per bruker med `KEEP`.
   Lagre flere ganger gir derfor ikke flere jobber. Av + Lagre avbryter jobben og
   fjerner et eventuelt vannvarsel fra varselfeltet.
3. `WaterReminderWorker.kt` blir kjørt av Android omtrent hvert 15. minutt. Første
   bakgrunnssjekk har også minst 15 minutters ventetid. Ingen internettforbindelse trengs.
4. `WaterReminderPlan.kt` fordeler ønsket antall jevnt gjennom tidsrommet, midt i hver
   del. Tre mellom 08:00 og 20:00 gir ca. 10:00, 14:00 og 18:00.
5. `WaterReminderRunner.kt` leser de nyeste valgene fra Room og sjekker telefonens
   lokale dato og tid. Den sender bare innenfor Fra (inkludert) og Til (ikke inkludert).
   Er flere tidspunkt passert, velges bare det siste; vi sender ingen bunke for å ta igjen.
6. `ReminderDeliveryEntity.kt` og `ReminderDeliveryDao.kt` husker siste brukte
   tidspunkt og antall sendeforsøk per bruker/dato. Det hindrer gjentakelse ved
   omstart, parallelle jobber og endring av innstillingene. Av/på nullstiller ikke telleren.
7. `WaterReminderNotifications.kt` lager en varselkanal, sjekker app- og kanaltillatelse,
   og viser et vennlig varsel. Trykk åpner appens vannskjerm. Samme varsel-ID brukes
   slik at det bare ligger ett vannvarsel per bruker i varselfeltet.

Android bestemmer når WorkManager faktisk får kjøre. Batterisparing og Doze kan
forsinke jobben; eksakte klokkeslett eller nøyaktig antall kan ikke garanteres.
Et veldig kort tidsrom kan derfor gi færre eller ingen varsler. Gamle varsler fra
utenfor tidsrommet forkastes. En jobb som blir avbrutt, eller en tillatelse som fjernes
akkurat ved sending, kan også gi færre varsler. Android-varsling og en Room-transaksjon
kan ikke sammen gi en absolutt garanti om «nøyaktig én levering» ved prosesskrasj.

Planen bruker lokal tid også etter endret tidssone eller sommertid. En gjentatt time
skal ikke sende samme tidspunkt på nytt. WorkManager beholder jobben etter vanlig
prosessavslutning og omstart av telefonen. Etter **Tving stopp** må appen åpnes igjen.
Vi bruker ikke eksakte alarmer eller en permanent bakgrunnstjeneste.

Foreløpig brukes bruker 1. Når profil/innlogging kommer må både vannskjermen og
oppstartslyttingen kobles til aktiv bruker, og gammel brukers jobb avbrytes ved bytte.
Varslene stoppes ikke ved visningsmålet på 2000 ml; dette er ennå ikke et personlig mål.

## Tillatelse og ønsket innstilling er forskjellige ting

På Android 13 og nyere ber bryteren om varslingstillatelse når den slås på.
Ved avslag forblir valget av, og brukeren kan åpne telefonens varselinnstillinger.
På eldre Android vises samme lenke hvis varsler er blokkert i systemet.
Tillatelsen sjekkes på nytt når appen blir synlig. Den lagres ikke i Room, fordi
den kan endres utenfor appen. Et tidligere lagret ønske beholdes ved senere
tilbakekalling av tillatelsen; skjermen viser at varsler er blokkert.

## Prøv selv

**Rask varseltest:** Åpne «Vannpåminnelser» og trykk **Test varsel**. Ved behov
ber knappen om Androids varslingstillatelse først. Dette slår ikke på daglige
påminnelser og lagrer ikke utkastet. Testen sender med én gang, bruker samme
varselkanal som vannpåminnelsene, men egen ID/tekst. Den endrer verken Room-telleren
eller WorkManager-jobben. Gjentatte trykk oppdaterer samme testvarsel.
Dra ned varselfeltet for å se «Amate – testvarsel». Lyd/banner kan være dempet av
telefonen. Testknappen bekrefter varselvisningen, ikke at bakgrunnsplanen får kjøre.

1. Velg `app` og kjør appen.
2. Åpne «Vannpåminnelser» mellom vannknappene og dagens registreringer.
3. Velg et annet antall og for eksempel 09:30–20:00. Lagre.
4. Start appen på nytt og sjekk at valgene er beholdt.
5. Prøv ugyldige tider eller Til tidligere enn Fra; ingenting skal lagres.
6. Endre et valg og trykk Avbryt. Det lagrede valget skal være uendret.
7. Slå på bryteren, tillat varsler og **lagre**. Velg for eksempel 3 mellom 08:00–20:00.
   Gå ut av appen med Hjem, ikke Tving stopp. Et varsel kan komme ved en bakgrunnssjekk
   etter et planlagt tidspunkt. Ikke forvent umiddelbar sending når du trykker Lagre.
8. Trykk på varselet: appen skal åpnes. Slå deretter av + Lagre: varselet fjernes og
   den unike jobben avbrytes. Endre gjerne tid/antall og lagre flere ganger; bare én jobb skal finnes.
9. De automatiserte testene under bruker en styrt klokke for å sjekke en hel dags
   forløp uten å vente i timevis. Det erstatter ikke en reell langtidstest under Doze.

Alle tekster ligger i `app/src/main/res/values/strings.xml`.
`MainActivity.kt` og `WaterScreen.kt` setter inn feltet uten å bygge felles navigasjon.

## Andre endringer og tester

- `AndroidManifest.xml` erklærer `POST_NOTIFICATIONS`. Selve forespørselen skjer
  først når brukeren slår på bryteren.
- `app/build.gradle.kts` legger Room-kodegeneratoren til Android-testene også,
  slik at migreringstesten kan lage den gamle databaseversjonen.
- `.gitignore` utelater `.kotlin/`, som er en automatisk generert hurtigbuffer.
- `RemindersViewModelTest.kt` har ni tester av utkast, lagring, feil og tillatelse.
- `ReminderDaoTest.kt` har fire tester av standardvalg, oppdatering, validering og
  gjenåpning av databasen.
- `ReminderSettingsScreenTest.kt` har to tester av feltene, lagring og Avbryt.
- `DatabaseMigrationTest.kt` sjekker 1 → 3 og 2 → 3 med bevart vannlogg og innstillinger.
- `WaterReminderPlanTest.kt` tester tidspunkt, dagbytte, sommertid, endringer og duplikatvern.
- `WaterReminderRunnerTest.kt` tester med ekte Room og falsk klokke/varsler, også parallelle
  kall og gjenåpnet database.
- `ReminderSchedulerTest.kt` kontrollerer at WorkManager beholder én jobb og avbryter den.
- `WaterReminderWorkerTest.kt` kjører selve worker-klassen for manglende bruker/valg.
- `WaterReminderNotificationsTest.kt` gir testappen varslingstillatelse og kontrollerer
  et ekte varsel i Androids varselfelt. Det bruker en egen testbruker og rydder varselet bort.
- `ReminderTestNotificationTest.kt` trykker på testknappen og sjekker at et ekte
  testvarsel vises uten å slå på påminnelser, lagre utkast eller endre dagens teller.
- `ReminderTestButtonTest.kt` sjekker forklaringen og innstillingslenken når testen er blokkert.

Kjør lokale tester og bygg:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Kjør de nye databasetestene og UI-testene med en emulator tilkoblet:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Til PR-en

- Room-innstillinger, en enkel utfellbar editor og Androids varslingstillatelse.
- WorkManager-jobb med av/på, jevnt fordelte ønskede tidspunkt og vern mot duplikater.
- Vannvarsler som åpner appen, og migreringer som bevarer eksisterende data.
- Tester av logikk, database, migrering, UI, scheduling og ekte varselvisning.
- Hjemskjerm, egen innstillingsside, vær/pollen og andre gruppemedlemmers funksjoner er ikke bygget her.

Bakgrunnsatferden følger [WorkManager-dokumentasjonen](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).

Verifisert 19. oktober 2026: 28 lokale tester og 27 Android-tester bestod på
Medium Phone / Android 17. Debug-bygg og lint bestod også (0 lint-feil; eksisterende
versjons- og ressursadvarsler gjenstår). En fysisk telefon og en hel dags kjøring
under batterisparing/Doze er ikke langtidstestet.

Etter at testknappen ble lagt til, bestod også 6 målrettede Android-tester for
innstillingsskjermen, testknappen og varselvisningen. De 28 lokale testene, bygg
og lint ble kjørt på nytt og bestod.
