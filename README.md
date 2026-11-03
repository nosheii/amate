# Amate

Amate er en selvomsorgsapp som hjelper brukere med å bygge gode hverdagsrutiner uten prestasjonspress. Appen er laget for alle som mangler automatiserte selvomsorgsvaner i dagliglivet, enten det skyldes funksjonsnedsettelser, sykdom eller en ny livssituasjon.

## Hva appen gjør

Mange eksisterende selvomsorgs-apper er laget for en "gjennomsnittsbruker" og har et prestasjonsorientert, tallfokusert design med streaks, poeng og statistikk, som ofte oppleves skambasert eller dømmende. Amate tar en annen tilnærming: brukeren velger selv en avatar, lager sin egen liste med gjøremål, og mottar oppmuntrende feedback gjennom dagen. Streaks brukes kun for å holde motivasjonen oppe, og det finnes ingen straff for å miste en streak.

## Funksjoner

- **Varsler** – Påminnelser om vann og egendefinerte gjøremål, med brukervalgt frekvens (1–5 om dagen) og "stille timer" (f.eks. ikke 22–07)
- **Stegsensor** – Skritteller med dagsmål
- **Lokasjonstjenester** – Brukes til vær og pollen, som styrer bakgrunn, avatarens tilbehør (paraply, solbriller) og anbefalinger som "gå tur i dag"
- **Eksterne API-er** – Værdata fra Yr/MET og pollenvarsel fra Google Pollen API
- **Lokal database (Room)** – Lagrer bruker, mål, prestasjoner og egne gjøremål på enheten. Brukerdata lagres kun lokalt; bare posisjon sendes til vær- og pollentjenestene
- **Navigation** – Navigasjon mellom skjermer med Navigation Compose
- **Tilpasset UI** – Egen avatar (tegnet i Procreate), bakgrunn som skifter med tid på døgnet og vær, og støtte for portrett og landskap

## Teknologi

- Kotlin
- Jetpack Compose / Material Design
- Room (lokal database)
- Navigation Compose

## Kom i gang

1. Klon repoet
2. Åpne prosjektet i Android Studio
3. Bygg og kjør appen på emulator eller fysisk Android-enhet

## Gruppe

Prosjektet er laget av gruppe 7 i MOB3000 (Applikasjonsutvikling for mobile enheter). Hvert medlem bygger sin egen "vertikale funksjon" (Compose-skjerm → ViewModel/UiState → Repository → DAO → Room-tabell):

- Nora Al-Tay – prosjektleder, vannregistrering og påminnelser (pilotfunksjon), avatarer tegnet i Procreate, integrasjon mot vær-/pollen-API
- Ana María Araya Flores – profil og avatar-valg
- Najma Abdi – mål og egendefinerte gjøremål, koordinerer databaseklassen og navigasjonen
- Felix Torp-Nilsen – skritt og sensor

Se `PROJECT_STRUCTURE.md` for git-arbeidsflyten vi bruker i prosjektet.
