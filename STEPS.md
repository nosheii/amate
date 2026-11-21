# Skritt og skrittpåminnelser

Skrittkortet på hjemskjermen og skrittskjermen bruker samme Room-repository og
samme skrittmål. Sensoren startes etter tillatelse til fysisk aktivitet. Android
bruker skrittdetektor når den finnes, ellers skrittelleren. Manuelle registreringer
og sensorhendelser legges nå til atomisk i databasen, så samtidige oppdateringer
ikke overskriver hverandre. Dagens sum bytter dato når appen åpnes igjen.

## Påminnelser

Innstillinger har separate seksjoner for vann og skritt. Begge gjenbruker den samme
editoren og tidsvalideringen, men har hver sin ViewModel-nøkkel, tabell, varselkanal,
WorkManager-jobb og sendelogg. Bare «Lagre» endrer daglige påminnelser. Testvarselet
ber om varslingstillatelse ved behov og viser om forsøket ble sendt eller blokkert.
Trykk på skrittvarselet åpner skrittskjermen; et brukt varsel overstyrer ikke valgt
fane etter rotasjon.

Skrittjobben sjekkes omtrent hvert 15. minutt og leser ferske innstillinger og dagens
lagrede skritt. Samme tidspunkt sendes ikke flere ganger, og ingen nye påminnelser
sendes når dagens aktive skrittmål er nådd. Android kan forsinke bakgrunnsjobber.
Testvarselet bruker egen identitet og bruker ikke opp dagens påminnelseskvote.

Databasen er på versjon 8. Migrering 6→7 legger til skrittinnstillingene som allerede
var referert til på main. 7→8 legger til en egen sendelogg for skritt. Tidligere vann,
profil, avatar, mål og skritt beholdes. Ingen destruktiv migrering brukes.

## Verifisering

Enhets- og emulatortester dekker døgnskifte, samtidige registreringer, separate
vann-/skrittinnstillinger, tidsvindu, blokkering, nådd mål, duplikatvern, planlegging,
varslenavigasjon og migrering fra versjon 6/7 (i tillegg til eksisterende eldre tester).
Testene bruker egne databaser; varslenavigasjon bevarer og gjenoppretter testprofilen.

Fysisk sensorinnsamling må også prøves på en telefon. Appen har ingen egen
forgrunnstjeneste som garanterer kontinuerlig skrittelling når app-prosessen er
avsluttet. Bakgrunnsjobben leser skritt som allerede er lagret; den starter ikke sensoren.
