package com.ix7.tracker.tracker

import android.content.Context
import android.location.Location
import android.util.Log
import com.ix7.tracker.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class TripRecorder(private val context: Context) {
    private val repository = TripRepository(context)

    private var currentTrip: CurrentTripData? = null
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private var startTime: Long = 0
    private var pausedTime: Long = 0
    private var lastPauseTime: Long = 0

    private val speedBuckets = mutableMapOf(
        0 to 0L,
        1 to 0L,
        2 to 0L,
        3 to 0L,
        4 to 0L,
        5 to 0L,
        6 to 0L,
        7 to 0L
    )

    fun startTrip(
        battery: Int,
        odometer: Float,
        location: Location?,
        settings: TripSettings
    ) {
        if (_isRecording.value) return

        startTime = System.currentTimeMillis()
        pausedTime = 0

        currentTrip = CurrentTripData(
            id = UUID.randomUUID().toString(),
            startDate = Date(startTime),
            startBattery = battery,
            startOdometer = odometer,
            startLocation = location?.let {
                TripLocation(it.latitude, it.longitude, "")
            } ?: TripLocation(0.0, 0.0, ""),
            settings = settings,
            maxSpeed = 0f,
            speedSamples = mutableListOf()
        )

        speedBuckets.keys.forEach { speedBuckets[it] = 0L }

        _isRecording.value = true
        Log.i("TripRecorder", "🟢 Trajet démarré: ${currentTrip?.id}")
    }

    fun pauseTrip() {
        if (!_isRecording.value) return
        lastPauseTime = System.currentTimeMillis()
        Log.i("TripRecorder", "⏸️ Trajet en pause")
    }

    fun resumeTrip() {
        if (!_isRecording.value) return
        if (lastPauseTime > 0) {
            pausedTime += System.currentTimeMillis() - lastPauseTime
            lastPauseTime = 0
        }
        Log.i("TripRecorder", "▶️ Trajet repris")
    }

    fun updateSpeed(speed: Float) {
        if (!_isRecording.value || lastPauseTime > 0) return

        val trip = currentTrip ?: return

        // Mettre à jour vitesse max
        if (speed > trip.maxSpeed) {
            trip.maxSpeed = speed
        }

        // Ajouter échantillon
        trip.speedSamples.add(speed)

        // Catégoriser vitesse
        val bucket = when {
            speed < 1f -> 0      // 0 km/h
            speed < 10f -> 1     // 0-10 km/h
            speed < 20f -> 2     // 10-20 km/h
            speed < 30f -> 3     // 20-30 km/h
            speed < 40f -> 4     // 30-40 km/h
            speed < 50f -> 5     // 40-50 km/h
            speed < 60f -> 6     // 50-60 km/h
            else -> 7            // 60+ km/h
        }

        speedBuckets[bucket] = (speedBuckets[bucket] ?: 0) + 1
    }

    suspend fun stopTrip(
        battery: Int,
        odometer: Float,
        location: Location?
    ): Trip? {
        if (!_isRecording.value) return null

        val trip = currentTrip ?: return null

        val endTime = System.currentTimeMillis()
        val totalDuration = ((endTime - startTime - pausedTime) / 1000).coerceAtLeast(1)

        val distance = (odometer - trip.startOdometer).coerceAtLeast(0.01f)
        val avgSpeed = if (trip.speedSamples.isNotEmpty()) {
            trip.speedSamples.average().toFloat()
        } else 0f

        val batteryUsed = (trip.startBattery - battery).coerceAtLeast(0)
        val energyUsed = batteryUsed * 10f // Approximation

        // Convertir les buckets en secondes
        val totalSamples = trip.speedSamples.size.coerceAtLeast(1)
        val secondsPerSample = totalDuration.toFloat() / totalSamples

        val speedStats = SpeedStats(
            range0 = ((speedBuckets[0] ?: 0) * secondsPerSample).toLong(),
            range0_10 = ((speedBuckets[1] ?: 0) * secondsPerSample).toLong(),
            range10_20 = ((speedBuckets[2] ?: 0) * secondsPerSample).toLong(),
            range20_30 = ((speedBuckets[3] ?: 0) * secondsPerSample).toLong(),
            range30_40 = ((speedBuckets[4] ?: 0) * secondsPerSample).toLong(),
            range40_50 = ((speedBuckets[5] ?: 0) * secondsPerSample).toLong(),
            range50_60 = ((speedBuckets[6] ?: 0) * secondsPerSample).toLong(),
            rangeAbove60 = ((speedBuckets[7] ?: 0) * secondsPerSample).toLong()
        )

        val completedTrip = Trip(
            id = trip.id,
            startDate = trip.startDate,
            startBattery = trip.startBattery,
            startOdometer = trip.startOdometer,
            startLocation = trip.startLocation,
            endDate = Date(endTime),
            endBattery = battery,
            endOdometer = odometer,
            endLocation = location?.let {
                TripLocation(it.latitude, it.longitude, "")
            } ?: TripLocation(0.0, 0.0, ""),
            distance = distance,
            duration = totalDuration,
            maxSpeed = trip.maxSpeed,
            avgSpeed = avgSpeed,
            energyUsed = energyUsed,
            speedStats = speedStats,
            settings = trip.settings
        )

        // Sauvegarder dans la DB
        repository.insertTrip(completedTrip)

        _isRecording.value = false
        currentTrip = null

        Log.i("TripRecorder", "🏁 Trajet terminé et sauvegardé: ${completedTrip.id}")
        Log.i("TripRecorder", "   Distance: ${distance}km, Durée: ${totalDuration}s, Max: ${trip.maxSpeed}km/h")

        return completedTrip
    }
}

private data class CurrentTripData(
    val id: String,
    val startDate: Date,
    val startBattery: Int,
    val startOdometer: Float,
    val startLocation: TripLocation,
    val settings: TripSettings,
    var maxSpeed: Float,
    val speedSamples: MutableList<Float>
)