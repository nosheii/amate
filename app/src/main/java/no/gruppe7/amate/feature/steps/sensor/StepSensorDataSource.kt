package no.gruppe7.amate.feature.steps.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** TODO: Pakk inn SensorManager og TYPE_STEP_COUNTER. */
interface StepSensorDataSource {
    val stepEvents: Flow<Int>
    val isSensorAvailable: Boolean
}
// leser skritt fra androids snesormanager
//
class AndroidStepSensorDataSource(
    context: Context,
) : StepSensorDataSource {


    // applicationcontext unngår å holde på en activity
    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val stepSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    override val isSensorAvailable: Boolean
        get() = stepSensor != null

    // Gjør sensorlytteren om til en Flow. Lytteren registreres når noen begynner å
    // samle inn flowen, og fjernes når innsamlingen stopper.
    override val stepEvents: Flow<Int> = callbackFlow {
        if (sensorManager == null || stepSensor == null) {
            close()
            return@callbackFlow
        }
        //forrige verdi på skrittteller
        var lastStepCounterValue: Float? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                when (event.sensor.type) {
                    Sensor.TYPE_STEP_DETECTOR -> {
                        // hver hendelser er ett skritt
                        val count = event.values.firstOrNull()?.toInt() ?: 1
                        trySend(count)
                    }
                    Sensor.TYPE_STEP_COUNTER -> {
                        val totalSteps = event.values.firstOrNull() ?: return
                        if (lastStepCounterValue != null) {
                            val delta = (totalSteps - lastStepCounterValue!!).toInt()
                            // Negativ verdi betyr at telleren er nullstilt (omstart)
                            if (delta > 0) {
                                trySend(delta)
                            }
                        }
                        lastStepCounterValue = totalSteps
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val registered = sensorManager.registerListener(
            listener,
            stepSensor,
            SensorManager.SENSOR_DELAY_UI,
        )

        if (!registered) {
            close()
            return@callbackFlow
        }

    }
}
