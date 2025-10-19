package com.ix7.tracker.wear.data.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.ix7.tracker.wear.data.database.AppDatabase
import com.ix7.tracker.wear.data.model.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TripDataSyncService(private val context: Context) {
    private val dataClient = Wearable.getDataClient(context)
    private val tripDao = AppDatabase.getInstance(context).tripDao()

    suspend fun sendPendingTripsToPhone() = withContext(Dispatchers.IO) {
        try {
            val pendingTrips = tripDao.getPendingTrips()

            if (pendingTrips.isEmpty()) {
                Log.d("TRIP_SYNC", "No pending trips to sync")
                return@withContext
            }

            // Sérialise les trips en JSON
            val tripsJson = Json.encodeToString<List<Trip>>(pendingTrips)

            val putDataReq = PutDataMapRequest.create("/trips/sync").apply {
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putString("trips", tripsJson)
                dataMap.putInt("count", pendingTrips.size)
            }.asPutDataRequest()

            // Marque comme urgent pour transmission immédiate
            putDataReq.setUrgent()

            // Envoie les données
            Tasks.await(dataClient.putDataItem(putDataReq))

            // Marque comme synced APRÈS envoi réussi
            pendingTrips.forEach { trip ->
                tripDao.markSynced(trip.id)
            }

            Log.d("TRIP_SYNC", "Successfully sent ${pendingTrips.size} trips to phone")
        } catch (e: Exception) {
            Log.e("TRIP_SYNC", "Error syncing: ${e.message}", e)
            throw e
        }
    }

    suspend fun keepOnly30LastTrips() = withContext(Dispatchers.IO) {
        try {
            val count = tripDao.getTripCount()
            if (count > 30) {
                repeat(count - 30) {
                    tripDao.deleteOldestTrip()
                }
                Log.d("TRIP_SYNC", "Cleaned up trips. Kept last 30")
            } else {
                Log.d("TRIP_SYNC", "Trip count is OK (${count})")
            }
        } catch (e: Exception) {
            Log.e("TRIP_SYNC", "Error cleaning trips: ${e.message}", e)
        }
    }
}