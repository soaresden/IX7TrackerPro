package com.ix7.tracker.wear

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.ix7.tracker.data.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WearDataSyncManager(private val context: android.content.Context) {

    companion object {
        const val TAG = "WearDataSyncManager"
        const val SYNC_PATH = "/trips/sync"
    }

    suspend fun syncTripsToWear(trips: List<Trip>) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📤 Envoi de ${trips.size} trips vers la montre...")

            val request = PutDataMapRequest.create(SYNC_PATH).apply {
                dataMap.putInt("count", trips.size)

                trips.forEachIndexed { index, trip ->
                    dataMap.putDataMap("trip_$index", com.google.android.gms.wearable.DataMap().apply {
                        putString("id", trip.id)
                        putLong("startDate", trip.startDate.time)
                        putInt("startBattery", trip.startBattery)
                        putDouble("startOdometer", trip.startOdometer.toDouble())
                        putDouble("startLatitude", trip.startLocation.latitude)
                        putDouble("startLongitude", trip.startLocation.longitude)
                        putString("startAddress", trip.startLocation.address)

                        putLong("endDate", trip.endDate.time)
                        putInt("endBattery", trip.endBattery)
                        putDouble("endOdometer", trip.endOdometer.toDouble())
                        putDouble("endLatitude", trip.endLocation.latitude)
                        putDouble("endLongitude", trip.endLocation.longitude)
                        putString("endAddress", trip.endLocation.address)

                        putDouble("distance", trip.distance.toDouble())
                        putLong("duration", trip.duration)
                        putDouble("maxSpeed", trip.maxSpeed.toDouble())
                        putDouble("avgSpeed", trip.avgSpeed.toDouble())
                        putDouble("energyUsed", trip.energyUsed.toDouble())

                        putLong("range0", trip.speedStats.range0)
                        putLong("range0_10", trip.speedStats.range0_10)
                        putLong("range10_20", trip.speedStats.range10_20)
                        putLong("range20_30", trip.speedStats.range20_30)
                        putLong("range30_40", trip.speedStats.range30_40)
                        putLong("range40_50", trip.speedStats.range40_50)
                        putLong("range50_60", trip.speedStats.range50_60)
                        putLong("rangeAbove60", trip.speedStats.rangeAbove60)

                        putString("ridingMode", trip.settings.ridingMode.name)
                        putString("driveMode", trip.settings.driveMode.name)
                        putString("speedLock", trip.settings.speedLock.name)
                    })
                }
            }

            request.asPutDataRequest().setUrgent()
            Tasks.await(Wearable.getDataClient(context).putDataItem(request.asPutDataRequest()))
            Log.d(TAG, "✅ ${trips.size} trips envoyés à la montre")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur sync: ${e.message}", e)
            throw e
        }
    }
}