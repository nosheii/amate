package no.gruppe7.amate.feature.steps.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import no.gruppe7.amate.feature.steps.data.local.DailyStepsEntity
import no.gruppe7.amate.feature.steps.data.local.StepsDao
import no.gruppe7.amate.feature.steps.sensor.StepSensorDataSource
import java.time.LocalDate

interface StepsRepository {
    // sanntidsantal skritt for gitt dag
    fun getStepsForToday(userId: Long = 0, epochDay: Long = LocalDate.now().toEpochDay()): Flow<Int>
    // sanntidsliste over registrerte dager for bruker, nyeste først
    fun getStepsHistory(userId: Long = 0): Flow<List<DailyStepsEntity>>
    // legger til skritt i dagens sum, oppretter raden hvis den ikke finnes
    suspend fun incrementStep(userId: Long = 0, count: Int = 1, epochDay: Long = LocalDate.now().toEpochDay())
    val isSensorAvailable: Boolean
}
// lytter på sensor og legger hver skritthendelse i databasenh
class RoomStepsRepository(
    private val stepsDao: StepsDao,
    private val stepSensorDataSource: StepSensorDataSource,
    externalScope: CoroutineScope? = null,
) : StepsRepository {

    override val isSensorAvailable: Boolean
        get() = stepSensorDataSource.isSensorAvailable

    init {
        // send sensorhendelse videre til databasen
        externalScope?.launch {
            stepSensorDataSource.stepEvents.collect { delta ->
                incrementStep(userId = 0, count = delta)
            }
        }
    }

    override fun getStepsForToday(userId: Long, epochDay: Long): Flow<Int> {
        return stepsDao.getStepsForDay(userId, epochDay).map { entity ->
            entity?.stepCount ?: 0
        }
    }

    override fun getStepsHistory(userId: Long): Flow<List<DailyStepsEntity>> {
        return stepsDao.getStepsHistory(userId)
    }

    override suspend fun incrementStep(userId: Long, count: Int, epochDay: Long) {
        // leser sum, legger til nye skritt ogskriver resultatene
        val existing = stepsDao.getStepsForDayOneShot(userId, epochDay)
        val currentCount = existing?.stepCount ?: 0
        val updatedEntity = DailyStepsEntity(
            userId = userId,
            epochDay = epochDay,
            stepCount = currentCount + count,
        )
        stepsDao.upsertSteps(updatedEntity)
    }
}
