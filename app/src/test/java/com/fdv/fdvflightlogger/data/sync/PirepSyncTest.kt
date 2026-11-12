package com.fdv.fdvflightlogger.data.sync

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.data.db.FlightType
import com.fdv.fdvflightlogger.data.fdv.FdvPirep
import com.fdv.fdvflightlogger.data.prefs.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PirepSyncTest {

    private val hour = 60L * 60 * 1000
    private val logged = 1_761_000_000_000L   // when the logbook entry was created

    private fun flight(
        flightNumber: String? = "366",
        scheduleId: Long? = null,
        route: String? = null
    ) = FlightLogEntity(
        createdAtEpochMs = logged,
        dep = "KPHX",
        arr = "KATL",
        airline = "DAL",
        flightNumber = flightNumber,
        route = route,
        blockTime = "0230",          // planned
        flightType = FlightType.OFFLINE_NO_ATC.name,
        fdvScheduleId = scheduleId
    )

    private fun pirep(
        id: Long,
        submittedAt: Long = logged + 4 * hour,
        flightNumber: String? = "366",
        dep: String = "KPHX",
        scheduleId: Long? = null,
        online: Boolean = false
    ) = FdvPirep(
        id = id, callsign = "DAL$flightNumber", airline = "DAL", flightNumber = flightNumber,
        dep = dep, arr = "KATL", aircraft = "A21N", status = "approved",
        submittedAtMillis = submittedAt, blockMinutes = 209, airMinutes = 193,
        fuelUsedLbs = 17188.0, pax = 244, landingRateFpm = -349, online = online,
        route = "BROAK1 MAXXO DCT IRW GLAVN2", scheduleId = scheduleId, charterId = null
    )

    @Test
    fun `booking ID beats a same-callsign PIREP that is closer in time`() {
        val match = PirepMatcher.match(
            flight(scheduleId = 170187),
            listOf(pirep(1, submittedAt = logged + 1 * hour), pirep(2, submittedAt = logged + 5 * hour, scheduleId = 170187)),
            taken = emptySet()
        )
        assertEquals(2L, match?.id)
    }

    @Test
    fun `same schedule flown last week does not match`() {
        val match = PirepMatcher.match(
            flight(scheduleId = 170187, flightNumber = null),
            listOf(pirep(1, submittedAt = logged - 7 * 24 * hour, scheduleId = 170187)),
            taken = emptySet()
        )
        assertNull(match)
    }

    @Test
    fun `closest same-callsign PIREP wins`() {
        val match = PirepMatcher.match(
            flight(),
            listOf(pirep(1, submittedAt = logged + 30 * hour), pirep(2, submittedAt = logged + 3 * hour)),
            taken = emptySet()
        )
        assertEquals(2L, match?.id)
    }

    @Test
    fun `a PIREP linked to another flight is skipped`() {
        val match = PirepMatcher.match(flight(), listOf(pirep(1)), taken = setOf(1L))
        assertNull(match)
    }

    @Test
    fun `different airports never match`() {
        assertNull(PirepMatcher.match(flight(), listOf(pirep(1, dep = "KLAS")), taken = emptySet()))
    }

    @Test
    fun `different flight number never matches`() {
        assertNull(PirepMatcher.match(flight(), listOf(pirep(1, flightNumber = "1060")), taken = emptySet()))
    }

    @Test
    fun `no flight number matches only when unambiguous`() {
        val one = PirepMatcher.match(flight(flightNumber = null), listOf(pirep(1)), taken = emptySet())
        assertEquals(1L, one?.id)

        val two = PirepMatcher.match(
            flight(flightNumber = null),
            listOf(pirep(1), pirep(2, submittedAt = logged + 8 * hour)),
            taken = emptySet()
        )
        assertNull(two)
    }

    @Test
    fun `actuals replace planned values and link the PIREP`() {
        val synced = flight().withActuals(pirep(150772, online = true), WeightUnit.LB)

        assertEquals("0329", synced.blockTime)    // was the planned 0230
        assertEquals("0313", synced.airTime)
        assertEquals("17188", synced.fuelUsed)
        assertEquals("244", synced.pax)
        assertEquals(-349, synced.landingRateFpm)
        assertEquals(FlightType.ONLINE.name, synced.flightType)
        assertEquals("BROAK1 MAXXO DCT IRW GLAVN2", synced.route)   // was blank, so filled
        assertEquals(150772L, synced.fdvPirepId)
    }

    @Test
    fun `fuel used converts to kg and the pilot's route and offline type are kept`() {
        val synced = flight(route = "MY OWN ROUTE").withActuals(pirep(1, online = false), WeightUnit.KG)

        assertEquals("7796", synced.fuelUsed)     // 17188 lb / 2.20462
        assertEquals("MY OWN ROUTE", synced.route)
        assertEquals(FlightType.OFFLINE_NO_ATC.name, synced.flightType)
    }
}