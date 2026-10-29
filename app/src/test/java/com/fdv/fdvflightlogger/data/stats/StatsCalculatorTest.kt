package com.fdv.fdvflightlogger.data.stats

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsCalculatorTest {

    private fun flight(
        dep: String,
        arr: String,
        block: String? = null,
        aircraft: String? = null,
        type: String = "ONLINE",
        created: Long = 0
    ) = FlightLogEntity(
        dep = dep,
        arr = arr,
        blockTime = block,
        aircraft = aircraft,
        flightType = type,
        createdAtEpochMs = created
    )

    // Known distances instead of airports.json; anything else is "unknown"
    private val fakeDistance: (String, String) -> Double? = { dep, arr ->
        if (dep == "KPHX" && arr == "KATL") 1376.0 else null
    }

    @Test
    fun emptyLogbookGivesZeroes() {
        val stats = StatsCalculator.compute(emptyList(), "KATL", fakeDistance)

        assertEquals(0, stats.totalFlights)
        assertEquals(0f, stats.onlineShare)
        assertNull(stats.lastFlight)
    }

    @Test
    fun totalsSharesAndRankings() {
        val flights = listOf(
            flight("KPHX", "KATL", block = "0334", aircraft = "A21N", created = 1),
            flight("KATL", "KPHX", block = "0412", aircraft = "a21n", type = "OFFLINE_NO_ATC", created = 2),
            flight("KJFK", "KBOS", block = "01", aircraft = "B739", created = 3)
        )

        val stats = StatsCalculator.compute(flights, "KATL", fakeDistance)

        assertEquals(3, stats.totalFlights)
        assertEquals((3 * 60 + 34) + (4 * 60 + 12) + 60, stats.blockMinutes)
        assertEquals(1376.0, stats.distanceNm, 0.01)
        assertEquals(2, stats.onlineFlights)
        assertEquals(2, stats.hubFlights)
        assertEquals(4, stats.airportsVisited)
        // "a21n" and "A21N" count as the same aircraft
        assertEquals(RankedItem("A21N", 2), stats.topAircraft.first())
        assertEquals("KPHX", stats.longestFlight?.dep)
        assertEquals(3L, stats.lastFlight?.createdAtEpochMs)
    }

    @Test
    fun parseMinutesSkipsHalfTypedTimes() {
        assertEquals(214, StatsCalculator.parseMinutes("0334"))
        assertEquals(180, StatsCalculator.parseMinutes("3"))
        assertNull(StatsCalculator.parseMinutes("033"))
        assertNull(StatsCalculator.parseMinutes("0375"))
    }

    @Test
    fun milestoneLadder() {
        assertEquals(10, nextMilestone(0.0))
        assertEquals(50, nextMilestone(34.0))
        assertEquals(50, nextMilestone(25.0))
        assertEquals(20_000, nextMilestone(12_345.0))
    }
}