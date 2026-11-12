# Lokal oppstart

Dette er ikke konto/passord eller flere brukere. Oppstarten bruker samme lokale `userId = 1`
som vann, skritt, profil og mål allerede bruker.

- Navn og avatar må velges. Vann- og skrittmål er valgfrie og bruker GoalRepository sine grenser.
- Inntil «Start AMATE» trykkes, ligger profil- og målvalgene bare i ViewModel/SavedStateHandle.
- Profil, avatar og eventuelle mål lagres i én Room-transaksjon. Eksisterende alder beholdes.
- «Sett mål senere»/tomme mål sletter ikke eksisterende mål.
- Først etter lagringen settes `completed_1` i den lokale `onboarding`-preferansen.
- Tillatelsene sjekkes hos Android. Varseltidspunkter aktiveres ikke av oppstarten.
- Eksisterende profiler må gjennom oppstarten én gang, men feltene er forhåndsutfylt.

## Til Ana Maria: start på nytt

`container.onboardingRepository.resetCompletion(1)` åpner oppstarten igjen. Denne funksjonen
sletter **ikke** profil, mål, vannlogg eller andre data. Den delen er fortsatt ditt ansvar.

Ved full sletting bør huskelappen nullstilles først, så fullføres slettingen i en transaksjon.
Skjermen observerer Room: en slettet profil eller avatar vil også sende brukeren tilbake til
oppstarten, selv om huskelappen mot formodning fortsatt er sann. Nye valg overskriver ikke gamle
registreringer automatisk. Stans skrittsensoren (`stepsRepository.setSensorPermission(false)`)
og påminnelsene før en full reset, og fjern tilhørende brukerdata samlet. Androids tillatelser
blir ikke trukket tilbake ved å slette den lokale profilen.

Ingen databaseversjon eller migrering er endret av denne funksjonen.
