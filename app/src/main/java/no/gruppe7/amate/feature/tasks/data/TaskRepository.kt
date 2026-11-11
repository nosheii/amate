package no.gruppe7.amate.feature.tasks.data

import kotlinx.coroutines.flow.Flow
import no.gruppe7.amate.feature.tasks.data.local.SelfCareTaskDao
import no.gruppe7.amate.feature.tasks.data.local.SelfCareTaskEntity

// Lengste tittel vi godtar. Beskytter mot limte avsnitt, ikke en designregel
const val MAX_TITLE_LENGTH = 60

// Interfacet er det ViewModel-en får bruke. Det skjuler Room for resten av appen.
// I testene bytter vi ut Room med en falsk  versjon av dette interfacet.
interface TaskRepository {

    // Følger med på gjøremålene for en dag.
    fun observeTasksForDay(userId: Long, startMillis: Long, endMillis: Long): Flow<List<SelfCareTaskEntity>>

    // Lager et nytt gjøremål for en dag. Returnerer ID-en.
    suspend fun addTask(userId: Long, title: String, scheduledForMillis: Long): Long

    // Endrer bare tittelen. ID, dag og fullført-status beholdes.
    suspend fun rename(task: SelfCareTaskEntity, title: String)

    // Marker som gjort eller angre, completedAtMillis settes når man har markert gjøremålet.
    suspend fun setCompleted(task: SelfCareTaskEntity, completed: Boolean, nowMillis: Long)

    suspend fun delete(task: SelfCareTaskEntity)

    fun observeCompletedCount(userId: Long): Flow<Int>
}

// Den ekte versjone som bruker Room.
class RoomTaskRepository(
    private val taskDao: SelfCareTaskDao,
) : TaskRepository {

    override fun observeTasksForDay(userId: Long, startMillis: Long, endMillis: Long) =
        taskDao.observeTasksForDay(userId, startMillis, endMillis)

    override suspend fun addTask(userId: Long, title: String, scheduledForMillis: Long): Long {
        // Sjekker også her, så andre skjermer ikke kan lagre tomme gjøremål.
        val cleanTitle = requireValidTitle(title)
        return taskDao.insert(
            SelfCareTaskEntity(userId = userId, title = cleanTitle, scheduledForMillis = scheduledForMillis)
        )
    }

    override suspend fun rename(task: SelfCareTaskEntity, title: String) {
        // Angrer man, nullstilles tidspunktet også, så da sitter man ikke igjen med et gammelt "gjort kl"
        taskDao.update(task.copy(title = requireValidTitle(title)))
    }

    override suspend fun setCompleted(task: SelfCareTaskEntity, completed: Boolean, nowMillis: Long) {
        taskDao.update(task.copy(isCompleted = completed, completedAtMillis =  if (completed) nowMillis else null))
    }

    override suspend fun delete(task: SelfCareTaskEntity) = taskDao.delete(task)

    override fun observeCompletedCount(userId: Long) = taskDao.observeCompletedCount(userId)

    // Fjerner mellomrom rundt og nektet tom eller for lang tekst
    // require() kaster IllegalArgumentException, som ViewModel fanger i write {}.
    private fun requireValidTitle(title: String): String {
        val clean = title.trim()
        require(clean.isNotEmpty() && clean.length <= MAX_TITLE_LENGTH)
        return clean
    }

}
