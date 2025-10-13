package com.ix7.tracker.utils

import com.ix7.tracker.data.*
import java.util.*

object TripUtils {

    fun formatDurationMMSS(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }

    fun formatDurationSimple(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return when {
            h > 0 -> "${h}h${m.toString().padStart(2, '0')}"
            else -> "${m}min"
        }
    }

    fun formatDurationFull(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when {
            h > 0 -> "${h}h${m.toString().padStart(2, '0')}m"
            m > 0 -> "${m}m${s.toString().padStart(2, '0')}s"
            else -> "${s}s"
        }
    }

    fun detectBatteryCycles(trips: List<Trip>): List<BatteryCycle> {
        if (trips.isEmpty()) return emptyList()

        val sortedTrips = trips.sortedBy { it.startDate }
        val cycles = mutableListOf<BatteryCycle>()
        var currentCycleTrips = mutableListOf<Trip>()
        var cycleNumber = 1

        sortedTrips.forEachIndexed { index, trip ->
            currentCycleTrips.add(trip)

            val nextTrip = sortedTrips.getOrNull(index + 1)
            if (nextTrip != null && nextTrip.startBattery > trip.endBattery + 5) {
                val cycle = createBatteryCycle(cycleNumber, currentCycleTrips)
                cycles.add(cycle)
                currentCycleTrips = mutableListOf()
                cycleNumber++
            }
        }

        if (currentCycleTrips.isNotEmpty()) {
            cycles.add(createBatteryCycle(cycleNumber, currentCycleTrips))
        }

        return cycles
    }

    private fun createBatteryCycle(cycleNumber: Int, trips: List<Trip>): BatteryCycle {
        val firstTrip = trips.first()
        val lastTrip = trips.last()

        val aggregatedStats = SpeedStats(
            range0 = trips.sumOf { it.speedStats.range0 },
            range0_10 = trips.sumOf { it.speedStats.range0_10 },
            range10_20 = trips.sumOf { it.speedStats.range10_20 },
            range20_30 = trips.sumOf { it.speedStats.range20_30 },
            range30_40 = trips.sumOf { it.speedStats.range30_40 },
            range40_50 = trips.sumOf { it.speedStats.range40_50 },
            range50_60 = trips.sumOf { it.speedStats.range50_60 },
            rangeAbove60 = trips.sumOf { it.speedStats.rangeAbove60 }
        )

        return BatteryCycle(
            cycleNumber = cycleNumber,
            startBattery = firstTrip.startBattery,
            endBattery = lastTrip.endBattery,
            startDate = firstTrip.startDate,
            endDate = lastTrip.endDate,
            trips = trips,
            totalDistance = trips.sumOf { it.distance.toDouble() }.toFloat(),
            totalDuration = trips.sumOf { it.duration },
            aggregatedSpeedStats = aggregatedStats
        )
    }

    fun analyzeModeStats(trips: List<Trip>): List<ModeStats> {
        if (trips.isEmpty()) return emptyList()

        return trips.groupBy { it.settings }.map { (settings, tripsForMode) ->
            val batteryConsumptions = tripsForMode.map { trip ->
                val batteryUsed = trip.startBattery - trip.endBattery
                if (trip.distance > 0) batteryUsed.toFloat() / trip.distance else 0f
            }

            val aggregatedStats = SpeedStats(
                range0 = tripsForMode.sumOf { it.speedStats.range0 },
                range0_10 = tripsForMode.sumOf { it.speedStats.range0_10 },
                range10_20 = tripsForMode.sumOf { it.speedStats.range10_20 },
                range20_30 = tripsForMode.sumOf { it.speedStats.range20_30 },
                range30_40 = tripsForMode.sumOf { it.speedStats.range30_40 },
                range40_50 = tripsForMode.sumOf { it.speedStats.range40_50 },
                range50_60 = tripsForMode.sumOf { it.speedStats.range50_60 },
                rangeAbove60 = tripsForMode.sumOf { it.speedStats.rangeAbove60 }
            )

            ModeStats(
                settings = settings,
                tripCount = tripsForMode.size,
                totalDistance = tripsForMode.sumOf { it.distance.toDouble() }.toFloat(),
                totalDuration = tripsForMode.sumOf { it.duration },
                avgSpeed = tripsForMode.map { it.avgSpeed }.average().toFloat(),
                maxSpeed = tripsForMode.maxOf { it.maxSpeed },
                avgBatteryConsumption = batteryConsumptions.average().toFloat(),
                aggregatedSpeedStats = aggregatedStats
            )
        }.sortedByDescending { it.tripCount }
    }

    fun generateDummyTrips(): List<Trip> {
        return listOf(
            Trip(
                id = "1",
                startDate = Date(System.currentTimeMillis() - 86400000 * 5),
                startBattery = 100,
                startOdometer = 1234.5f,
                startLocation = TripLocation(48.8566, 2.3522, "Paris Centre"),
                endDate = Date(System.currentTimeMillis() - 86400000 * 5 + 1860000),
                endBattery = 68,
                endOdometer = 1247.0f,
                endLocation = TripLocation(48.8606, 2.3376, "Tour Eiffel"),
                distance = 12.5f,
                duration = 1860,
                maxSpeed = 42f,
                avgSpeed = 24f,
                energyUsed = 245f,
                speedStats = SpeedStats(180, 420, 374, 600, 366, 100, 20, 0),
                settings = TripSettings(RidingMode.SPORT, DriveMode.TWO_WHEELS, SpeedLock.UNLOCKED)
            ),
            Trip(
                id = "2",
                startDate = Date(System.currentTimeMillis() - 86400000 * 4),
                startBattery = 68,
                startOdometer = 1247.0f,
                startLocation = TripLocation(48.8606, 2.3376, "Tour Eiffel"),
                endDate = Date(System.currentTimeMillis() - 86400000 * 4 + 1200000),
                endBattery = 42,
                endOdometer = 1255.3f,
                endLocation = TripLocation(48.8738, 2.2950, "La Défense"),
                distance = 8.3f,
                duration = 1200,
                maxSpeed = 38f,
                avgSpeed = 25f,
                energyUsed = 180f,
                speedStats = SpeedStats(120, 300, 280, 400, 200, 20, 0, 0),
                settings = TripSettings(RidingMode.ECO, DriveMode.TWO_WHEELS, SpeedLock.LOCKED)
            ),
            Trip(
                id = "3",
                startDate = Date(System.currentTimeMillis() - 86400000 * 3),
                startBattery = 95,
                startOdometer = 1255.3f,
                startLocation = TripLocation(48.8738, 2.2950, "La Défense"),
                endDate = Date(System.currentTimeMillis() - 86400000 * 3 + 1500000),
                endBattery = 65,
                endOdometer = 1267.8f,
                endLocation = TripLocation(48.8584, 2.2945, "Arc de Triomphe"),
                distance = 12.5f,
                duration = 1500,
                maxSpeed = 45f,
                avgSpeed = 30f,
                energyUsed = 250f,
                speedStats = SpeedStats(150, 380, 400, 450, 300, 50, 20, 0),
                settings = TripSettings(RidingMode.RACE, DriveMode.TWO_WHEELS, SpeedLock.UNLOCKED)
            ),
            Trip(
                id = "4",
                startDate = Date(System.currentTimeMillis() - 86400000 * 2),
                startBattery = 65,
                startOdometer = 1267.8f,
                startLocation = TripLocation(48.8584, 2.2945, "Arc de Triomphe"),
                endDate = Date(System.currentTimeMillis() - 86400000 * 2 + 900000),
                endBattery = 50,
                endOdometer = 1273.1f,
                endLocation = TripLocation(48.8566, 2.3522, "Paris Centre"),
                distance = 5.3f,
                duration = 900,
                maxSpeed = 25f,
                avgSpeed = 21f,
                energyUsed = 120f,
                speedStats = SpeedStats(200, 350, 250, 100, 0, 0, 0, 0),
                settings = TripSettings(RidingMode.PEDESTRIAN, DriveMode.ONE_WHEEL, SpeedLock.LOCKED)
            )
        )
    }
}