package com.fdv.fdvflightlogger.data.sync

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.data.fdv.FdvPirep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PirepStatusSyncTest {

    private fun flight(id: Long, pirepId: Long?, status: String?) =
        FlightLogEntity(id = id, dep = "KATL", arr = "KJFK", fdvPirepId = pirepId, fdvStatus = status)

    private fun pirep(id: Long, status: String?) = FdvPirep(
        id = id, callsign = "DAL1", airline = "DAL", flightNumber = "1", dep = "KATL", arr = "KJFK",
        aircraft = null, status = status, submittedAtMillis = null, blockMinutes = null, airMinutes = null,
        fuelUsedLbs = null, pax = null, landingRateFpm = null, online = false, route = null,
        scheduleId = null, charterId = null
    )

    @Test
    fun `only pending and never-checked linked flights are awaited`() {
        val flights = listOf(
            flight(1, 101, null),          // linked before version 7: never checked
            flight(2, 102, "pending"),
            flight(3, 103, "approved"),    // final
            flight(4, 104, "rejected"),    // final
            flight(5, null, null)          // never filed
        )
        assertEquals(listOf(1L, 2L), PirepStatusSync.awaiting(flights).map { it.id })
    }

    @Test
    fun `changes lists only flights whose status moved`() {
        val open = listOf(flight(1, 101, null), flight(2, 102, "pending"), flight(3, 103, "pending"))
        val onFdv = listOf(
            pirep(101, "Approved"),        // case doesn't matter
            pirep(102, "pending"),         // unchanged
            pirep(103, "rejected"),
            pirep(999, "approved")         // not one of ours
        )
        assertEquals(mapOf(1L to "approved", 3L to "rejected"), PirepStatusSync.changes(open, onFdv))
    }

    @Test
    fun `paging stops once the oldest awaited PIREP is reached`() {
        assertFalse(PirepStatusSync.reachedOldest(listOf(pirep(160, null), pirep(150, null)), oldestWanted = 120))
        assertTrue(PirepStatusSync.reachedOldest(listOf(pirep(130, null), pirep(110, null)), oldestWanted = 120))
    }
}