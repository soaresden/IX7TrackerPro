package com.ix7.tracker.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.ix7.tracker.wear.WearDataSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ===== ENTITÉ ROOM =====

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val startDate: Long,
    val startBattery: Int,
    val startOdometer: Float,
    val startLatitude: Double,
    val startLongitude: Double,
    val startAddress: String,
    val endDate: Long,
    val endBattery: Int,
    val endOdometer: Float,
    val endLatitude: Double,
    val endLongitude: Double,
    val endAddress: String,
    val distance: Float,
    val duration: Long,
    val maxSpeed: Float,
    val avgSpeed: Float,
    val energyUsed: Float,
    val range0: Long,
    val range0_10: Long,
    val range10_20: Long,
    val range20_30: Long,
    val range30_40: Long,
    val range40_50: Long,
    val range50_60: Long,
    val rangeAbove60: Long,
    val ridingMode: String,
    val driveMode: String,
    val speedLock: String
)

// ===== DAO =====

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startDate DESC LIMIT 30")
    fun getLast30Trips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips ORDER BY startDate DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: String): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(trips: List<TripEntity>)

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id IN (:ids)")
    suspend fun deleteTrips(ids: List<String>)

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun getTripCount(): Int
}

// ===== DATABASE =====

@Database(entities = [TripEntity::class], version = 1, exportSchema = false)
abstract class TripDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: TripDatabase? = null

        fun getDatabase(context: Context): TripDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TripDatabase::class.java,
                    "trip_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ===== CONVERSIONS =====

fun TripEntity.toTrip(): Trip {
    return Trip(
        id = id,
        startDate = java.util.Date(startDate),
        startBattery = startBattery,
        startOdometer = startOdometer.toFloat(),
        startLocation = TripLocation(startLatitude, startLongitude, startAddress),
        endDate = java.util.Date(endDate),
        endBattery = endBattery,
        endOdometer = endOdometer.toFloat(),
        endLocation = TripLocation(endLatitude, endLongitude, endAddress),
        distance = distance.toFloat(),
        duration = duration,
        maxSpeed = maxSpeed.toFloat(),
        avgSpeed = avgSpeed.toFloat(),
        energyUsed = energyUsed.toFloat(),
        speedStats = SpeedStats(
            range0, range0_10, range10_20, range20_30,
            range30_40, range40_50, range50_60, rangeAbove60
        ),
        settings = TripSettings(
            ridingMode = com.ix7.tracker.core.RideMode.valueOf(ridingMode),
            driveMode = com.ix7.tracker.core.WheelMode.valueOf(driveMode),
            speedLock = com.ix7.tracker.core.SpeedLimitMode.valueOf(speedLock)
        )
    )
}

fun Trip.toEntity(): TripEntity {
    return TripEntity(
        id = id,
        startDate = startDate.time,
        startBattery = startBattery,
        startOdometer = startOdometer.toFloat(),
        startLatitude = startLocation.latitude,
        startLongitude = startLocation.longitude,
        startAddress = startLocation.address,
        endDate = endDate.time,
        endBattery = endBattery,
        endOdometer = endOdometer.toFloat(),
        endLatitude = endLocation.latitude,
        endLongitude = endLocation.longitude,
        endAddress = endLocation.address,
        distance = distance.toFloat(),
        duration = duration,
        maxSpeed = maxSpeed.toFloat(),
        avgSpeed = avgSpeed.toFloat(),
        energyUsed = energyUsed.toFloat(),
        range0 = speedStats.range0,
        range0_10 = speedStats.range0_10,
        range10_20 = speedStats.range10_20,
        range20_30 = speedStats.range20_30,
        range30_40 = speedStats.range30_40,
        range40_50 = speedStats.range40_50,
        range50_60 = speedStats.range50_60,
        rangeAbove60 = speedStats.rangeAbove60,
        ridingMode = settings.ridingMode.name,
        driveMode = settings.driveMode.name,
        speedLock = settings.speedLock.name
    )
}

// ===== REPOSITORY =====

class TripRepository(context: Context) {
    private val database = TripDatabase.getDatabase(context)
    private val tripDao = database.tripDao()

    private val wearSyncManager by lazy { WearDataSyncManager(context) }

    val last30Trips: Flow<List<Trip>> = tripDao.getLast30Trips().map { entities ->
        entities.map { it.toTrip() }
    }

    val allTrips: Flow<List<Trip>> = tripDao.getAllTrips().map { entities ->
        entities.map { it.toTrip() }
    }

    suspend fun insertTrip(trip: Trip) {
        tripDao.insertTrip(trip.toEntity())
    }

    suspend fun insertTrips(trips: List<Trip>) {
        tripDao.insertTrips(trips.map { it.toEntity() })
    }

    suspend fun getTripById(id: String): Trip? {
        return tripDao.getTripById(id)?.toTrip()
    }

    suspend fun updateTrip(trip: Trip) = withContext(Dispatchers.IO) {
        try {
            tripDao.updateTrip(trip.toEntity())
            android.util.Log.d("TripRepository", "✅ Trip ${trip.id} mis à jour")
        } catch (e: Exception) {
            android.util.Log.e("TripRepository", "❌ Erreur update: ${e.message}", e)
            throw e
        }
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

    suspend fun syncToWear(trips: List<Trip>) = withContext(Dispatchers.IO) {
        try {
            val tripsToSync = trips.take(30)
            wearSyncManager.syncTripsToWear(tripsToSync)
            android.util.Log.d("TripRepository", "✅ ${tripsToSync.size} trips envoyés à la montre")
        } catch (e: Exception) {
            android.util.Log.e("TripRepository", "❌ Erreur sync: ${e.message}", e)
            throw e
        }
    }
}