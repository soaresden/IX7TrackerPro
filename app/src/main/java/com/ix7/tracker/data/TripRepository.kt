package com.ix7.tracker.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TripRepository(context: Context) {
    private val database = TripDatabase.getDatabase(context)
    private val tripDao = database.tripDao()

    val allTrips: Flow<List<Trip>> = tripDao.getAllTrips().map { entities ->
        entities.map { it.toTrip() }
    }

    suspend fun insertTrip(trip: Trip) {
        tripDao.insertTrip(trip.toEntity())
    }

    suspend fun getTripById(id: String): Trip? {
        return tripDao.getTripById(id)?.toTrip()
    }

    suspend fun deleteTrip(trip: Trip) {
        tripDao.deleteTrip(trip.toEntity())
    }

    suspend fun deleteTrips(ids: List<String>) {
        tripDao.deleteTrips(ids)
    }

    suspend fun getTripCount(): Int {
        return tripDao.getTripCount()
    }
}