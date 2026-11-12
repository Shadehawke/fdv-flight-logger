package com.fdv.fdvflightlogger.data.sync

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.data.db.FlightType
import com.fdv.fdvflightlogger.data.fdv.FdvPirep
import com.fdv.fdvflightlogger.data.prefs.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryImportTest {

    private val hour = 60L * 60 * 1000
    private val t0 = 1_761_000_000_000L

    private fun pirep(id: Long, number: String, dep: String, arr: String, at: Long, online: Boolean = false) =
        FdvPirep(
            id = id, callsign = "DAL$number", airline = "DAL", flightNumber = number,
            dep = dep, arr = arr, aircraft = "A21N", status = "approved",
            submittedAtMillis = at, blockMinutes = 63, airMinutes = null,
            fuelUsedLbs = 4420.0, pax = 158, landingRateFpm = -285, online = online,
            route = null, scheduleId = null, charterId = null
        )

    private fun flight(id: Long, number: String, dep: String, arr: String, at: Long, pirepId: Long? = null) =
        FlightLogEntity(
            id = id, createdAtEpochMs = at, dep = dep, arr = arr,
            airline = "DAL", flightNumber = number, fdvPirepId = pirepId
        )

    @Test
    fun `plan sorts PIREPs into matched, unmatched and already linked`() {
        val flights = listOf(
            flight(1, "366", "KPHX", "KATL", t0, pirepId = 150772),       // synced in 5c
            flight(2, "8841", "KLAS", "KPHX", t0 - 30 * 24 * hour)       // logged, never synced
        )
        val pireps = listOf(
            pirep(150772, "366", "KPHX", "KATL", t0 + 4 * hour),
            pirep(147463, "8841", "KLAS", "KPHX", t0 - 30 * 24 * hour + 2 * hour),
            pirep(146218, "555853", "KONT", "KLAS", t0 - 40 * 24 * hour)  // never logged in the app
        )

        val plan = HistoryImport.plan(flights, pireps)

        assertEquals(1, plan.alreadyLinked)
        assertEquals(listOf(2L to 147463L), plan.matched.map { (f, p) -> f.id to p.id })
        assertEquals(listOf(146218L), plan.unmatched.map { it.id })
        assertEquals(150772L, plan.highestPirepId)
        assertEquals(3, plan.total)
    }

    @Test
    fun `two logged flights can't claim the same PIREP`() {
        val flights = listOf(
            flight(1, "366", "KPHX", "KATL", t0),
            flight(2, "366", "KPHX", "KATL", t0 + 1 * hour)
        )
        val plan = HistoryImport.plan(flights, listOf(pirep(1, "366", "KPHX", "KATL", t0 + 2 * hour)))

        assertEquals(1, plan.matched.size)
        assertTrue(plan.unmatched.isEmpty())
    }

    @Test
    fun `imported flight is dated by its PIREP and marked as imported`() {
        val imported = pirep(146218, "555853", "KONT", "KLAS", t0, online = false)
            .toImportedFlight(WeightUnit.LB, nowMillis = 0L)

        assertEquals(t0, imported.createdAtEpochMs)
        assertTrue(imported.importedFromFdv)
        assertEquals(146218L, imported.fdvPirepId)
        assertEquals("KONT", imported.dep)
        assertEquals("555853", imported.flightNumber)
        assertEquals("0103", imported.blockTime)
        assertEquals("4420", imported.fuelUsed)
        assertEquals(-285, imported.landingRateFpm)
        assertEquals(FlightType.OFFLINE_NO_ATC.name, imported.flightType)
    }

    @Test
    fun `online PIREP imports as an online flight`() {
        val imported = pirep(1, "1060", "KATL", "KAUS", t0, online = true).toImportedFlight(WeightUnit.LB, 0L)
        assertEquals(FlightType.ONLINE.name, imported.flightType)
    }
}