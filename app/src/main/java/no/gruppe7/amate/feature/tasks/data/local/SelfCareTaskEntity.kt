package no.gruppe7.amate.feature.tasks.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** TODO: Legg til Room Entity og felt for planlegging/fullforing. */
// En rad i tabellen self_care_tasks = ett gjøremål, f.eks. "Gå en tur."
// Feltene må matche MIGRATION_3_4 nøyaktig. Nye felt krever ny databaseversjon og migrering
@Entity (tableName = "self_care_tasks")
data class SelfCareTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 0,
    val title: String = "",
    // Hvilken dag gjøremålet hører til. Lagres som starten av dagen i millisekunder,
    // samme måte som vannregistreringene lagrer tidspunkt.
    val scheduledForMillis: Long,
    val isCompleted: Boolean = false,
    // Når det ble fullført. Null betyr "ikke fullført ennå".
    val completedAtMillis: Long? = null,
)
