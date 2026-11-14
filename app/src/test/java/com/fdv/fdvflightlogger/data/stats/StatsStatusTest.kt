package com.fdv.fdvflightlogger.data.stats

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsStatusTest {

    private fun flight(id: Long, created: Long, pirepId: Long?, status: String?) = FlightLogEntity(
        id = id, createdAtEpochMs = created, dep = "KATL", arr = "KJFK",
        blockTime = "0200", fdvPirepId = pirepId, fdvStatus = status
    )

    @Test
    fun `pending and rejected flights are left out, like on FDV`() {
        val stats = StatsCalculator.compute(
            flights = listOf(
                flight(1, created = 1, pirepId = 11, status = "approved"),
                flight(2, created = 2, pirepId = null, status = null),     // logged, never filed: counts
                flight(3, created = 3, pirepId = 13, status = "rejected"),
                flight(4, created = 4, pirepId = 14, status = "pending")   // newest
            ),
            hub = "",
            distanceNm = { _, _ -> null }
        )

        assertEquals(2, stats.totalFlights)
        assertEquals(240, stats.blockMinutes)
        assertEquals(1, stats.pendingFlights)
        assertEquals(1, stats.rejectedFlights)
        assertEquals(4L, stats.lastFlight?.id)    // still your last flight, even while pending
    }
}