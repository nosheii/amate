package no.gruppe7.amate.feature.goals.data

import kotlinx.coroutines.flow.Flow
import no.gruppe7.amate.feature.goals.data.local.GoalDao
import no.gruppe7.amate.feature.goals.data.local.GoalEntity
import no.gruppe7.amate.feature.goals.data.local.GoalType
import no.gruppe7.amate.feature.goals.data.local.validRange

// Interfacet er det ViewModel-ene får bruke.
// Det skjuler Room, så ViewModel ikke vet hvor dataene kommer fra
// I testene bytter vi ut Room med en falsk versjon av dette interfacet
interface GoalRepository {
    // Følger med på alle aktive mål til en bruker.
    fun observeGoals (userId: Long): Flow<List<GoalEntity>>
    // Følger med på ett bestemt mål. Vannskjermen kan hente vannmåleren.
    fun observeGoal(userId: Long, type: GoalType): Flow<GoalEntity?>
    // Oppretter målet hvis det ikke finnes, ellers oppdateres verdien.
    // Returnerer ID-en til målet.
    suspend fun saveGoal(userId: Long, type: GoalType, targetValue: Int): Long
    suspend fun deleteGoal(goal: GoalEntity)
}

class RoomGoalRepository(private val goalDao: GoalDao) : GoalRepository {

    override fun observeGoals(userId: Long) = goalDao.observeActiveGoals(userId)

    override fun observeGoal(userId: Long, type: GoalType) = goalDao.observeGoal(userId, type)

    override suspend fun saveGoal(userId: Long,type: GoalType, targetValue: Int): Long {

        require(targetValue in type.validRange)

        val existing = goalDao.findGoal(userId, type)
        val goal = existing?.copy(targetValue = targetValue, isActive = true)
            ?: GoalEntity(userId = userId, type = type, targetValue = targetValue)
        val insertedId = goalDao.upsert(goal)
        return existing?.id ?: insertedId
    }

    override suspend fun deleteGoal(goal: GoalEntity)  = goalDao.delete(goal)
}
