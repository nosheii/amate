package no.gruppe7.amate.feature.goals.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// Her samler vi alt vi kan gjøre med målene i databasen.
// Room skriver selve koden bak disse funksjonene for oss.

@Dao
interface GoalDao {

    // Upsert betyr insert og update i ett.
    // Om det ikke finnes noe rad med denne ID-en,
    // Settes den inn og får en ID tilbake.
    // Om det finnes, oppdateres da raden og Room returnerer -1
    @Upsert
    suspend fun upsert(goal: GoalEntity): Long

    // Sletter et mål. Objektet må ha samme ID som raaden i databasen.
    @Delete
    suspend fun delete(goal: GoalEntity)

    //  Henter alle aktive mål til brukeren.
    // Flow sender automatisk en ny liste hver gang tabellen endrer seg.
    // Skjermen oppdaterer seg selv etter lagring og sletting.
    // ORDER BY type gir stabil rekkefølge (STEPS før Water, alfabetisk.)
    @Query("SELECT * FROM goals WHERE userId = :userId AND isActive = 1 ORDER BY type")
    fun observeActiveGoals(userId: Long): Flow<List<GoalEntity>>

    // Henter ett bestemt mål, for eksempel vannmålet.
    // Da returneres null hvis brukeren ikke har lagt det ennå.
    // Her er funksjonen vannskjermen kan bruke i stedet for det.
    @Query("SELECT * FROM goals WHERE userId = :userId AND type = :type AND isActive = 1 LIMIT 1")
    fun observeGoal(userId: Long, type: GoalType): Flow<GoalEntity?>

    // Brukes av repository ved lagring, for å finne raden som allerede finnes.
    // Ikke Flow, fordi vi bare trenger svaret en gang
    @Query("SELECT * FROM goals WHERE userId = :userId AND type = :type LIMIT 1")
    suspend fun findGoal(userId: Long, type: GoalType): GoalEntity?

}
