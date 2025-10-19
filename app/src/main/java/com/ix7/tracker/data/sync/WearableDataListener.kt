package com.ix7.tracker.data.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.ix7.tracker.data.TripRepository
import com.ix7.tracker.data.Trip
import com.ix7.tracker.data.TripLocation
import com.ix7.tracker.data.SpeedStats
import com.ix7.tracker.data.TripSettings
import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.WheelMode
import com.ix7.tracker.core.SpeedLimitMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Date

class WearableDataListener : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == "/trips/sync") {

                Log.d("WEAR_SYNC", "Data received from watch")
                handleTripsSyncFromWatch(event.dataItem)
            }
        }
    }

    private fun handleTripsSyncFromWatch(dataItem: com.google.android.gms.wearable.DataItem) {
        try {
            val dataMapItem = DataMapItem.fromDataItem(dataItem)
            val dataMap = dataMapItem.dataMap
            val tripCount = dataMap.getInt("count", 0)

            Log.d("WEAR_SYNC", "Received $tripCount trips from watch")

            val trips = mutableListOf<Trip>()
            for (i in 0 until tripCount) {
                val tripMap = dataMap.getDataMap("trip_$i") ?: continue

                val trip = Trip(
                    id = tripMap.getString("id") ?: "",
                    startDate = Date(tripMap.getLong("startDate")),
                    startBattery = tripMap.getInt("startBattery"),
                    startOdometer = tripMap.getDouble("startOdometer").toFloat(),
                    startLocation = TripLocation(
                        latitude = tripMap.getDouble("startLatitude"),
                        longitude = tripMap.getDouble("startLongitude"),
                        address = tripMap.getString("startAddress") ?: ""
                    ),
                    endDate = Date(tripMap.getLong("endDate")),
                    endBattery = tripMap.getInt("endBattery"),
                    endOdometer = tripMap.getDouble("endOdometer").toFloat(),
                    endLocation = TripLocation(
                        latitude = tripMap.getDouble("endLatitude"),
                        longitude = tripMap.getDouble("endLongitude"),
                        address = tripMap.getString("endAddress") ?: ""
                    ),
                    distance = tripMap.getDouble("distance").toFloat(),
                    duration = tripMap.getLong("duration"),
                    maxSpeed = tripMap.getDouble("maxSpeed").toFloat(),
                    avgSpeed = tripMap.getDouble("avgSpeed").toFloat(),
                    energyUsed = tripMap.getDouble("energyUsed").toFloat(),
                    speedStats = SpeedStats(
                        range0 = tripMap.getLong("range0"),
                        range0_10 = tripMap.getLong("range0_10"),
                        range10_20 = tripMap.getLong("range10_20"),
                        range20_30 = tripMap.getLong("range20_30"),
                        range30_40 = tripMap.getLong("range30_40"),
                        range40_50 = tripMap.getLong("range40_50"),
                        range50_60 = tripMap.getLong("range50_60"),
                        rangeAbove60 = tripMap.getLong("rangeAbove60")
                    ),
                    settings = TripSettings(
                        ridingMode = tryParseEnum(tripMap.getString("ridingMode"), RideMode.ECO),
                        driveMode = tryParseEnum(tripMap.getString("driveMode"), WheelMode.TWO_WHEELS),
                        speedLock = tryParseEnum(tripMap.getString("speedLock"), SpeedLimitMode.LIMITED)
                    )
                )
                trips.add(trip)
            }

            scope.launch(Dispatchers.IO) {
                try {
                    val repository = TripRepository(this@WearableDataListener)
                    repository.insertTrips(trips)
                    Log.d("WEAR_SYNC", "✅ Saved $tripCount trips to phone DB")
                    sendAckToWatch()
                } catch (e: Exception) {
                    Log.e("WEAR_SYNC", "❌ Error saving trips: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("WEAR_SYNC", "❌ Error parsing data: ${e.message}", e)
        }
    }

    private suspend fun sendAckToWatch() {
        try {
            val request = PutDataMapRequest.create("/trips/ack").apply {
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putBoolean("success", true)
            }.asPutDataRequest()

            request.setUrgent()
            Tasks.await(Wearable.getDataClient(this@WearableDataListener).putDataItem(request))
            Log.d("WEAR_SYNC", "✅ ACK sent to watch")
        } catch (e: Exception) {
            Log.e("WEAR_SYNC", "❌ Error sending ACK: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

private inline fun <reified T : Enum<T>> tryParseEnum(value: String?, default: T): T {
    return try {
        if (value != null) java.lang.Enum.valueOf(T::class.java, value) else default
    } catch (e: Exception) {
        default
    }
}