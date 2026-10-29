package com.fdv.fdvflightlogger.data.stats

import com.fdv.fdvflightlogger.data.db.FlightLogEntity

/** One entry in a "top" list, e.g. A21N flown 12 times. */
data class RankedItem(val label: String, val count: Int)

/** Progress toward the next round number, for the dashboard gauges. */
data class Milestone(val value: Double, val target: Int) {
    val progress: Float get() = (value / target).toFloat().coerceIn(0f, 1f)
}

data class FlightStats(
    val totalFlights: Int = 0,
    val blockMinutes: Int = 0,
    val airMinutes: Int = 0,
    val distanceNm: Double = 0.0,
    val onlineFlights: Int = 0,
    val hubFlights: Int = 0,
    val airportsVisited: Int = 0,
    val topAircraft: List<RankedItem> = emptyList(),
    val topRoutes: List<RankedItem> = emptyList(),
    val longestFlight: FlightLogEntity? = null,
    val longestFlightNm: Double = 0.0,
    val lastFlight: FlightLogEntity? = null
) {
    val blockHours: Double get() = blockMinutes / 60.0

    val onlineShare: Float get() = shareOf(onlineFlights)
    val hubShare: Float get() = shareOf(hubFlights)

    val flightsMilestone: Milestone get() = Milestone(totalFlights.toDouble(), nextMilestone(totalFlights.toDouble()))
    val hoursMilestone: Milestone get() = Milestone(blockHours, nextMilestone(blockHours))

    private fun shareOf(count: Int): Float =
        if (totalFlights == 0) 0f else count.toFloat() / totalFlights
}

private val MILESTONES = listOf(10, 25, 50, 100, 250, 500, 1_000, 2_500, 5_000, 10_000)

/** The first milestone above [value]; past 10,000 it keeps going in steps of 10,000. */
internal fun nextMilestone(value: Double): Int =
    MILESTONES.firstOrNull { it > value } ?: ((value / 10_000).toInt() + 1) * 10_000