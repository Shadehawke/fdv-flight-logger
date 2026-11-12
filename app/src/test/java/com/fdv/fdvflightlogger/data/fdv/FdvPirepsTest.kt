package com.fdv.fdvflightlogger.data.fdv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FdvPirepsTest {

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource(name)) { "Missing test resource $name" }.readText()

    @Test
    fun `logbook page maps the real response`() {
        val page = FdvApi.interpretPireps(200, fixture("fdv_pireps.json"))

        assertEquals(3, page.pireps.size)
        assertEquals(1, page.page)
        assertEquals(267, page.lastPage)
        assertEquals(800, page.total)
        assertTrue(page.hasMore)

        val newest = page.pireps.first()
        assertEquals(150772L, newest.id)
        assertEquals("DAL366", newest.callsign)
        assertEquals("DAL", newest.airline)
        assertEquals("366", newest.flightNumber)
        assertEquals("KPHX", newest.dep)
        assertEquals("KATL", newest.arr)
        assertEquals("A21N", newest.aircraft)
        assertEquals("approved", newest.status)
        assertEquals(Instant.parse("2026-10-22T20:06:41Z").toEpochMilli(), newest.submittedAtMillis)
        assertEquals(209, newest.blockMinutes)
        assertNull(newest.airMinutes)             // the list has no log
        assertEquals(17188.0, newest.fuelUsedLbs!!, 0.0)
        assertEquals(244, newest.pax)
        assertEquals(-349, newest.landingRateFpm)
        assertFalse(newest.online)

        // A charter: its airline name differs, but the flight number still splits off cleanly
        assertEquals("555853", page.pireps.last().flightNumber)
    }

    @Test
    fun `new list fields are mapped`() {
        // Shape after IT's 2026-11-11 update: route, booking IDs, and an online flight with no fuel figure
        val body = """
            {"data": [{"id": 160000, "flight": "DAL1060", "airline": {"icao": "DAL"},
              "departure": {"icao": "KATL"}, "arrival": {"icao": "KAUS"},
              "route": "POUNC2 STEIT DCT MEI DCT LCH DCT LUKKN WLEEE7",
              "processed_at": "2026-11-12T01:00:00+00:00",
              "schedule_id": 170187, "charter_id": null,
              "online_percent": 37, "fuel_used_lbs": 0}],
             "meta": {"page": 1, "per_page": 50, "total": 1, "last_page": 1}}
        """.trimIndent()

        val page = FdvApi.interpretPireps(200, body)
        val pirep = page.pireps.single()

        assertFalse(page.hasMore)
        assertEquals("POUNC2 STEIT DCT MEI DCT LCH DCT LUKKN WLEEE7", pirep.route)
        assertEquals(170187L, pirep.scheduleId)
        assertNull(pirep.charterId)
        assertTrue(pirep.online)                  // any share above 0 counts
        assertNull(pirep.fuelUsedLbs)             // 0 means Black Box didn't capture it
    }

    @Test
    fun `detail gives air time from the real Black Box log`() {
        val pirep = FdvApi.interpretPirep(200, fixture("fdv_pirep_detail.json"))

        // Taking Off 11:49 AM to the first Touched down 3:02 PM; "Landing lights" lines and the bounce are ignored
        assertEquals(193, pirep.airMinutes)
        assertEquals(209, pirep.blockMinutes)     // Black Box's own "Arrived flight duration: 03:29"
        assertEquals("BROAK1 MAXXO DCT IRW DCT FSM DCT MEMFS DCT HUTCC DCT KNSAW DCT RUSSA GLAVN2", pirep.route)
    }

    @Test
    fun `air time crosses midnight`() {
        val log = listOf(
            PirepLogDto("10/22/2026 11:30 PM", "Taking Off"),
            PirepLogDto("10/23/2026 1:15 AM", "Touched down at -142 fpm, gear lever: Down")
        )
        assertEquals(105, FdvPirepMapper.airMinutes(log))
    }

    @Test
    fun `air time falls back to the landed line`() {
        val log = listOf(
            PirepLogDto("01/05/2026 9:00 AM", "Taking Off"),
            PirepLogDto("01/05/2026 9:45 AM", "Landed on runway 26R KATL, touched down at 2590 ft")
        )
        assertEquals(45, FdvPirepMapper.airMinutes(log))
    }

    @Test
    fun `no takeoff line means no air time`() {
        val log = listOf(PirepLogDto("10/22/2026 3:02 PM", "Touched down at -349 fpm"))
        assertNull(FdvPirepMapper.airMinutes(log))
    }

    @Test
    fun `unreadable log time means no air time`() {
        val log = listOf(
            PirepLogDto("2026-10-22 11:49", "Taking Off"),
            PirepLogDto("10/22/2026 3:02 PM", "Touched down at -349 fpm")
        )
        assertNull(FdvPirepMapper.airMinutes(log))
    }

    @Test
    fun `429 carries the wait time`() {
        val e = FdvApi.rateLimited("30")
        assertEquals(30, e.retryAfterSeconds)
        assertEquals("FlyDeltaVirtual is limiting requests. Try again in 30 seconds.", e.message)
        assertFalse(e.signedOut)
    }

    @Test
    fun `429 without a header still has a message`() {
        val e = FdvApi.rateLimited(null)
        assertNull(e.retryAfterSeconds)
        assertEquals("FlyDeltaVirtual is limiting requests. Try again in a minute.", e.message)
    }

    @Test
    fun `401 on the logbook signs out`() {
        val e = assertThrows(FdvApiException::class.java) {
            FdvApi.interpretPireps(401, """{"success": false, "error": "invalid_token", "message": "Token expired"}""")
        }
        assertTrue(e.signedOut)
    }
}