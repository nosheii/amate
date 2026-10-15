package no.gruppe7.amate.feature.water.data

import no.gruppe7.amate.feature.water.data.local.WaterDao
import no.gruppe7.amate.feature.water.data.local.WaterEntryEntity
import kotlinx.coroutines.flow.Flow

/*
 * Dette er en liste over hva et WaterRepository må kunne gjøre.
 *
 * Jeg  skriver ikke hvordan funksjonene fungerer her.
 * Det gjør jeg i RoomWaterRepository nedenfor.
 */
interface WaterRepository {

    fun observeEntriesForDay(
        userId: Long,
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<WaterEntryEntity>>

    // Følger med på alle vannregistreringene til en bruker.
    // Flow sender automatisk en ny liste når databasen endres.
    fun observeEntries(
        userId: Long,
    ): Flow<List<WaterEntryEntity>>

    // Følger med på brukerens totale vannmengde innenfor et tidsrom.
    // Jeg  bruker start og slutt for å angi starten og slutten på dagen.
    fun observeTotalForDay(
        userId: Long,
        startMillis: Long,
        endMillis: Long,
    ): Flow<Int>

    // Lager og lagrer en ny vannregistrering.
    // Long som returneres er ID-en Room lager for registreringen.
    suspend fun addWater(
        userId: Long,
        amountMl: Int,
        loggedAtMillis: Long,
    ): Long

    // Oppdaterer en registrering som allerede finnes.
    suspend fun update(entry: WaterEntryEntity)

    // Sletter en registrering.
    suspend fun delete(entry: WaterEntryEntity)
}

/*
 * Dette er den faktiske Room-versjonen av WaterRepository.
 *
 * WaterDao kommer inn gjennom constructoren, slik at denne klassen
 * kan bruke DAO-funksjonene jeg lagde tidligere.
 */
class RoomWaterRepository(
    private val waterDao: WaterDao,
) : WaterRepository {

    override fun observeEntriesForDay(
        userId: Long,
        startMillis: Long,
        endMillis: Long,
    ) = waterDao.observeEntriesForDay(userId, startMillis, endMillis)

    // "override" betyr at jeg fyller inn en funksjon fra interfacet.
    override fun observeEntries(
        userId: Long,
    ): Flow<List<WaterEntryEntity>> {

        // Ber DAO-en hente registreringene fra Room.
        return waterDao.observeEntries(userId)
    }

    override fun observeTotalForDay(
        userId: Long,
        startMillis: Long,
        endMillis: Long,
    ): Flow<Int> {

        // Sender tidsrommet videre til DAO-en, som summerer vannmengden.
        return waterDao.observeTotalForDay(
            userId = userId,
            startMillis = startMillis,
            endMillis = endMillis,
        )
    }

    override suspend fun addWater(
        userId: Long,
        amountMl: Int,
        loggedAtMillis: Long,
    ): Long {

        // Sjekker også her, så andre skjermer ikke kan lagre ugyldige mengder.
        require(amountMl in 1..MAX_ENTRY_ML)

        // Først lager jeg et objekt som representerer en rad i databasen.
        val newEntry = WaterEntryEntity(
            userId = userId,
            amountMl = amountMl,
            loggedAtMillis = loggedAtMillis,
        )

        // Deretter lagrer DAO-en objektet og returnerer den nye ID-en.
        return waterDao.insert(newEntry)
    }

    override suspend fun update(entry: WaterEntryEntity) {
        require(entry.amountMl in 1..MAX_ENTRY_ML)
        waterDao.update(entry)
    }

    override suspend fun delete(entry: WaterEntryEntity) {
        waterDao.delete(entry)
    }
}

// Grense mot feiltasting i én registrering, ikke et daglig drikkemål.
const val MAX_ENTRY_ML = 10_000
