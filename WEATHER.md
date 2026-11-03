# Første værkobling

Branchen `weather-start` viser værvarsel for **Oslo** på hjemskjermen. Det er
ekte prognosedata, ikke en måling akkurat nå. Pollen, GPS og stedvelger venter.

## Hvordan det henger sammen

`WeatherRoute → WeatherViewModel → WeatherRepository → MetWeatherApiService`

- `MetWeatherApiService` henter Locationforecast compact over HTTPS på IO.
  Ingen API-nøkkel. User-Agent identifiserer skoleprosjektet med GitHub-lenken.
- `MetWeatherParser` velger varseltiden nærmest nå og kontrollerer temperatur/enhet.
  Ukjent symbol gir ukjent værtype, ikke et tilfeldig solikon.
- `WeatherViewModel` holder lasting, resultat og feil. Flere raske trykk gir bare ett
  pågående kall. Nye automatiske forsøk skjer tidligst etter 15 minutter, når
  hjemskjermen vises igjen. Ingen værjobb kjører jevnlig i bakgrunnen.
- `WeatherCard` tegner tilstanden og lar brukeren prøve igjen. Ved mislykket
  oppdatering beholdes forrige varsel med en advarsel og synlige tidspunkter.
  Kortet starter kompakt med sted, temperatur, værtype og varseltid. Pilen
  åpner mer informasjon inne i kortet; kilde og lisens er alltid tilgjengelige.
  En værstyrt bakgrunn er ikke laget ennå, men kan senere bruke samme symbolkode.
- `AppContainer` gir samme repository til kortet gjennom `MainActivity` og
  `AmateShell`. Vann og påminnelser bruker fortsatt sine egne koblinger.

HTTP-cache bruker opptil 5 MiB i appens cachemappe. Android håndterer Expires,
Last-Modified, betingede forespørsler og gzip. «Oppdater» omgår ikke serverens
cachefrist. Cache er ikke Room, så databaseversjon og lagrede vannregistreringer
endres ikke. Ved 429/503 venter klienten minst ett minutt og følger Retry-After.

## Kilde og personvern

Data fra [MET Norway / Locationforecast](https://api.met.no/weatherapi/locationforecast/2.0/documentation),
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
Appen viser et forenklet utdrag med Material-ikoner, ikke MET sine ikonfiler.
Kreditering og lenker finnes i kortet.

Vi følger [MET sine bruksvilkår](https://api.met.no/doc/TermsOfService).
Denne lille skoleversjonen kaller API-et direkte, med mellomlagring. MET mottar
telefonens IP-adresse og de faste Oslo-koordinatene, ikke telefonens GPS-posisjon.
Ved offentlig lansering/større bruk bør gruppa sette opp en mellomtjener for
felles cache og vurdere personvernteksten, slik MET anbefaler.

## Sjekk selv

1. Kjør appen med nettilgang og åpne hjem. Værkortet viser lasting og deretter Oslo,
   temperatur, værtype, varseltid og kildens oppdateringstid.
2. Trykk «Oppdater vær». Kortet beholder data mens det laster.
3. Nettfeil uten tidligere data gir feilmelding og «Prøv igjen». Feil etter en
   vellykket henting beholder forrige varsel med advarsel. Fersk HTTP-cache kan
   fortsatt fungere uten nett; det er forventet, ikke en feil.
4. Registrer vann og åpne innstillinger for å sjekke at de fungerer som før.

Automatiske tester bruker faste svar, ikke ekte internett: ViewModel-tester lokalt,
JSON/HTTP- og Compose-tester på emulatoren. Ingen tester nullstiller appens database.
