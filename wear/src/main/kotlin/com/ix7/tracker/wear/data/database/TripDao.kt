package com.ix7.tracker.wear.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ix7.tracker.wear.data.model.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(trips: List<Trip>)

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips ORDER BY startTime DESC LIMIT 30")
    suspend fun getLast30Trips(): List<Trip>

    @Query("SELECT * FROM trips WHERE syncStatus = 'PENDING' ORDER BY startTime DESC")
    suspend fun getPendingTrips(): List<Trip>

    @Query("UPDATE trips SET syncStatus = 'SYNCED' WHERE id = :tripId")
    suspend fun markSynced(tripId: String)

    @Query("UPDATE trips SET syncStatus = 'SYNCED' WHERE id IN (:tripIds)")
    suspend fun markMultipleSynced(tripIds: List<String>)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTrip(tripId: String)

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun getTripCount(): Int

    @Query("DELETE FROM trips ORDER BY startTime ASC LIMIT 1")
    suspend fun deleteOldestTrip()
}