package com.ix7.tracker.utils

import com.ix7.tracker.data.*
import com.ix7.tracker.core.SpeedLimitMode
import com.ix7.tracker.core.WheelMode
import com.ix7.tracker.core.RideMode
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
}
