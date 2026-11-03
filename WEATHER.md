# Vær på hjemskjermen

Værkortet kan vise værvarsel for brukerens **omtrentlige posisjon**.
Uten posisjon viser det **Oslo som tydelig merket standardsted**. Det er
ekte prognosedata, ikke en måling akkurat nå. Pollen og manuell stedvelger venter.

## Hvordan det henger sammen

`WeatherRoute → WeatherViewModel → WeatherRepository → MetWeatherApiService`

- `WeatherRoute` sjekker Android-tillatelsen. Første gang forklarer kortet hvorfor
  posisjon trengs. **Bruk min posisjon** åpner Androids eget tillatelsesvindu;
  vi maser ikke med nye vinduer ved hvert besøk på hjemskjermen.
- `AndroidLocationDataSource` fyller den tidligere tomme `LocationDataSource`.
  Den bruker AndroidX `LocationManagerCompat.getCurrentLocation` og ber om ett
  ferskt svar, med maks 15 sekunders venting. Fused-provider brukes på Android 12+
  hvis nettverksposisjon er på, ellers prøves GPS-provider på Android 12+.
  Også der får appen bare omtrentlig posisjon fra Android, ikke finere tilgang.
  Android 8–11 bruker nettverksposisjon. Manglende provider/posisjon gir
  en synlig feil og standardsted, ikke oppdiktede koordinater eller en krasj.
  Ingen nye biblioteker eller API-nøkler trengs.
- `MetWeatherApiService` henter Locationforecast compact over HTTPS på IO.
  Ingen API-nøkkel. User-Agent identifiserer skoleprosjektet med GitHub-lenken.
- `MetWeatherParser` velger varseltiden nærmest nå og kontrollerer temperatur/enhet.
  Ukjent symbol gir ukjent værtype, ikke et tilfeldig solikon.
- `WeatherViewModel` holder lasting, resultat og feil. Flere raske trykk gir bare ett
  pågående kall. Nye automatiske forsøk skjer tidligst etter 15 minutter, når
  hjemskjermen vises igjen. Ingen værjobb kjører jevnlig i bakgrunnen.
- `WeatherCard` tegner tilstanden og lar brukeren prøve igjen. Et tidligere
  varsel for samme sted kan beholdes ved nettfeil, med advarsel og synlige tidspunkter.
  Når posisjonen hentes på nytt, fjernes det gamle varselet først, så Oslo-vær aldri presenteres
  som været der brukeren er. Sene svar fra et tidligere sted ignoreres.
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
telefonens IP-adresse og koordinatene som brukes i værforespørselen, avrundet til
fire desimaler. Med tillatelse er dette Androids omtrentlige posisjon; uten
tillatelse er det faste Oslo-koordinater. Ingen adresseoppslag sendes til en
annen karttjeneste. Tidene for din posisjon vises i **telefonens tidssone**, ikke
en tidssone som er slått opp fra koordinatene. Tidssoneforkortelsen vises i kortet.

Appen ber bare om `ACCESS_COARSE_LOCATION`, ikke nøyaktig posisjon eller
bakgrunnstilgang. Den starter ingen posisjonsservice og følger ikke brukeren
kontinuerlig. En pågående posisjonsforespørsel avbrytes når hjem forsvinner eller
appen går i bakgrunnen. Nye automatiske forespørsler begrenses til hvert 15. minutt
mens hjem vises; oppdateringsknappen kan hente en ny posisjon tidligere.

Android-grunnlaget: [omtrentlig posisjon og tillatelser](https://developer.android.com/develop/sensors-and-location/location/permissions/runtime)
og [engangsposisjon med avbrytelse](https://developer.android.com/reference/androidx/core/location/LocationManagerCompat#getCurrentLocation(android.location.LocationManager,java.lang.String,android.os.CancellationSignal,java.util.concurrent.Executor,androidx.core.util.Consumer%3Candroid.location.Location%3E)).

Det opprettes ingen Room-tabell med posisjonshistorikk. Koordinatene holdes i
ViewModel, men HTTP-cachen kan inneholde forespørselsadresse med koordinater og
tilhørende nettverkssvar. En liten preferanse husker bare om forrige
tillatelsesforespørsel ble avslått. Tilgang kan trekkes tilbake i Android.
Etter gjentatte avslag tilbyr kortet apptillatelser. Avslåtte posisjonstjenester
gir en egen knapp til telefonens posisjonsinnstillinger. Ingen innstilling slås
på uten brukerens valg.

Ved offentlig lansering/større bruk bør gruppa sette opp en mellomtjener for
felles cache og vurdere personvernteksten, slik MET anbefaler.

## Sjekk selv

1. Kjør appen med nettilgang og åpne hjem. Før tilgang er gitt, vises Oslo tydelig
   som standardsted. Trykk **Bruk min posisjon**, og velg tilgang mens appen brukes
   eller bare denne gangen. Etter et ferskt posisjonssvar vises **Vær der du er**.
2. Trykk «Oppdater vær». Med tilgang hentes også en ny posisjon. Uten tilgang
   oppdateres Oslo. Emulatoren bruker posisjonen valgt i Extended Controls →
   Location, ikke Mac-ens virkelige posisjon. Send en posisjon derfra ved behov.
3. Nettfeil uten tidligere data gir feilmelding og «Prøv igjen». Ved oppdatering
   av samme standardsted kan forrige varsel beholdes med advarsel. Ved ny
   posisjonsforespørsel tømmes det gamle varselet først. Fersk HTTP-cache kan
   fortsatt fungere uten nett; det er forventet, ikke en feil.
4. Registrer vann og åpne innstillinger for å sjekke at de fungerer som før.
5. Prøv avslag på posisjon: værkortet skal fortsatt fungere for Oslo og forklare
   at det er standardstedet. Ved blokkert tillatelse åpner **Åpne apptillatelser**
   Androids innstillinger. Gi tilgang der og gå tilbake: posisjon skal hentes igjen.
6. Slå av telefonens posisjon og trykk oppdater: kortet forklarer hva som mangler
   og tilbyr **Slå på posisjon**. Når posisjon slås på og du går tilbake, prøver
   appen straks igjen. Manglende svar gir ikke en evig spinner.

Automatiske tester bruker faste svar, ikke ekte internett: ViewModel-tester lokalt,
JSON/HTTP- og Compose-tester på emulatoren. Posisjonstestene bruker kontrollerte
koordinater og tester avslag, bortfalt tilgang, tidsavbrudd, avbrytelse og sene svar.
Ingen tester nullstiller appens database.

## Små væranimasjoner

På branchen `solanimasjon` får det lille ikonfeltet disse variantene:

- Klarvær på dagtid: gul sol med rolig rotasjon og svak, pulserende glød.
- Delvis skyet på dagtid: samme sol bak en sky som beveger seg forsiktig.
- Skyet: rolig sky. Delvis skyet om natten vises også uten sol.
- Regn: sky med tre små, fallende dråper.
- Snø: sky med tre langsomme snøfnugg som svaier litt.
- Sludd: sky med både dråper og snøfnugg.

Alt bruker Material-biblioteket som allerede finnes i appen. Ingen GIF,
nye bildefiler eller nye avhengigheter trengs. Tåke, torden, klar natt og
ukjent vær beholder rolige stillbilder; det brukes ikke blinkende lyn.

`WeatherIcon.kt` velger type og tegner sola. `CloudWeatherIcon.kt` kombinerer
sky, dråper og snøfnugg i samme ikonfelt. Animasjonene stopper når skjermen ikke er aktiv,
mens været lastes/har feilet, eller hvis systemets animasjoner er slått av.
Bakgrunnen, Room og de andres funksjoner er uendret.

Værkortet har ingen manuelle prøvevalg, heller ikke i debug-appen. Det ekte
varselet bestemmer alltid hvilket ikon som vises. Automatiske tester sjekker
fortsatt alle animasjonstypene, og at testkontroller ikke dukker opp i værdetaljene.
