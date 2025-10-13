package com.ix7.tracker.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Entité Trip pour Room
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

    // Speed stats
    val range0: Long,
    val range0_10: Long,
    val range10_20: Long,
    val range20_30: Long,
    val range30_40: Long,
    val range40_50: Long,
    val range50_60: Long,
    val rangeAbove60: Long,

    // Settings
    val ridingMode: String,
    val driveMode: String,
    val speedLock: String
)

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startDate DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: String): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id IN (:ids)")
    suspend fun deleteTrips(ids: List<String>)

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun getTripCount(): Int
}

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

// Convertisseurs
fun TripEntity.toTrip(): Trip {
    return Trip(
        id = id,
        startDate = java.util.Date(startDate),
        startBattery = startBattery,
        startOdometer = startOdometer,
        startLocation = TripLocation(startLatitude, startLongitude, startAddress),
        endDate = java.util.Date(endDate),
        endBattery = endBattery,
        endOdometer = endOdometer,
        endLocation = TripLocation(endLatitude, endLongitude, endAddress),
        distance = distance,
        duration = duration,
        maxSpeed = maxSpeed,
        avgSpeed = avgSpeed,
        energyUsed = energyUsed,
        speedStats = SpeedStats(range0, range0_10, range10_20, range20_30, range30_40, range40_50, range50_60, rangeAbove60),
        settings = TripSettings(
            ridingMode = RidingMode.valueOf(ridingMode),
            driveMode = DriveMode.valueOf(driveMode),
            speedLock = SpeedLock.valueOf(speedLock)
        )
    )
}

fun Trip.toEntity(): TripEntity {
    return TripEntity(
        id = id,
        startDate = startDate.time,
        startBattery = startBattery,
        startOdometer = startOdometer,
        startLatitude = startLocation.latitude,
        startLongitude = startLocation.longitude,
        startAddress = startLocation.address,
        endDate = endDate.time,
        endBattery = endBattery,
        endOdometer = endOdometer,
        endLatitude = endLocation.latitude,
        endLongitude = endLocation.longitude,
        endAddress = endLocation.address,
        distance = distance,
        duration = duration,
        maxSpeed = maxSpeed,
        avgSpeed = avgSpeed,
        energyUsed = energyUsed,
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