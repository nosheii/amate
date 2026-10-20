package no.gruppe7.amate.feature.goals.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Hvilke typer mål appen støtter.
// Room lagrer enum-navnet som tekst i databasen enten ("WATER", "STEPS")
// så vi trenger ingen TypeConverter
enum class GoalType { WATER, STEPS }

// Gyldig område for hver måltype.
// Ligger her, og ikke i ViewModel, fordi både ViewModel (for feilmelding til
// og repository (som siste sjekk før lagring) skal bruke samme regel.
// Ny måltype = en ny linje her.
val GoalType.validRange: IntRange
    get() = when (this) {
        GoalType.WATER -> 500..10_000
        GoalType.STEPS -> 1_000..50_000
    }

// En rad i tabellen "goals" = ett mål, f.eks. "2000 ml vann om dagen"
// Indeksen på (userId, type) er unik, så databasen nekter to vannmål for samme bruker.
@Entity(
    tableName = "goals",
    indices = [Index(value = ["userId", "type"], unique = true)],
)
data class GoalEntity(
    // Room lager ID-en selv når vi setter inn en ny rad.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // Hvem målet tilhører. Kobles til profilfunksjonen senere.
    val userId: Long = 0,
    val type: GoalType = GoalType.WATER,
    // Selve målet, i ml for vann og antall for skritt.
    val targetValue: Int = 0,
    // Lar oss skru av et mål uten å slette det, hvis vi trenger historikk senere.
    val isActive: Boolean = true,
)
