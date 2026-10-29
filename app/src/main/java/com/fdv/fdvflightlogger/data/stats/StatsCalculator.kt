package com.fdv.fdvflightlogger.data.stats

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.data.db.FlightType

object StatsCalculator {

    /**
     * Builds dashboard stats from the whole logbook.
     * [distanceNm] is passed in rather than calling AirportRepository directly,
     * so tests can supply known distances without loading airports.json.
     */
    fun compute(
        flights: List<FlightLogEntity>,
        hub: String,
        distanceNm: (dep: String, arr: String) -> Double?
    ): FlightStats {
        if (flights.isEmpty()) return FlightStats()

        val hubCode = hub.trim().uppercase()

        // Look up each flight's distance once; it feeds both the total and the longest flight
        val withDistance = flights.map { it to distanceNm(it.dep, it.arr) }
        val longest = withDistance
            .filter { (_, nm) -> nm != null }
            .maxByOrNull { (_, nm) -> nm!! }

        return FlightStats(
            totalFlights = flights.size,
            blockMinutes = flights.sumOf { parseMinutes(it.blockTime) ?: 0 },
            airMinutes = flights.sumOf { parseMinutes(it.airTime) ?: 0 },
            distanceNm = withDistance.sumOf { (_, nm) -> nm ?: 0.0 },
            onlineFlights = flights.count { it.flightType == FlightType.ONLINE.name },
            hubFlights = if (hubCode.isEmpty()) 0 else flights.count {
                it.dep.equals(hubCode, ignoreCase = true) || it.arr.equals(hubCode, ignoreCase = true)
            },
            airportsVisited = flights
                .flatMap { listOf(it.dep, it.arr) }
                .map { it.trim().uppercase() }
                .filter { it.isNotEmpty() }
                .toSet()
                .size,
            topAircraft = topN(flights.mapNotNull { it.aircraft?.trim()?.uppercase()?.takeIf { a -> a.isNotEmpty() } }),
            topRoutes = topN(flights.map { "${it.dep.trim().uppercase()} → ${it.arr.trim().uppercase()}" }),
            longestFlight = longest?.first,
            longestFlightNm = longest?.second ?: 0.0,
            lastFlight = flights.maxByOrNull { it.createdAtEpochMs }
        )
    }

    // Count each label, most common first; ties break alphabetically so the order is stable
    private fun topN(labels: List<String>, n: Int = 5): List<RankedItem> =
        labels.groupingBy { it }.eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(n)
            .map { RankedItem(it.key, it.value) }

    /**
     * Logbook times are the digits typed into TimeField: "0334" = 3h 34m, "3" or "03" = 3h.
     * Three digits ("033") is a time still being typed, so it's skipped rather than guessed.
     */
    internal fun parseMinutes(digits: String?): Int? {
        val d = digits?.filter { it.isDigit() } ?: return null
        return when (d.length) {
            1, 2 -> d.toInt() * 60
            4 -> {
                val minutes = d.takeLast(2).toInt()
                if (minutes > 59) null else d.take(2).toInt() * 60 + minutes
            }
            else -> null
        }
    }
}