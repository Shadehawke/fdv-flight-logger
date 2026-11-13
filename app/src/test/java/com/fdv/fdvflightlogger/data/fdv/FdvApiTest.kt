package com.fdv.fdvflightlogger.data.fdv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FdvApiTest {

    private val me = """
        {"id": 1703, "callsign": "DAL1234", "firstname": "Shawn", "lastname": "Pilot",
         "rank": {"id": 12, "name": "First Officer", "hours_required": 2900},
         "next_rank": {"id": 13, "name": "Sky Team Executive Captain", "hours_required": 4000, "hours_remaining": 892.9},
         "hub": {"id": 3, "name": "Atlanta", "icao": "KATL"},
         "location": {"icao": "KATL", "name": "Atlanta Hartsfield-Jackson International Airport", "source": "last_flight"},
         "hours": 3107.1,
         "hours_breakdown": {"flown": 2879.6, "bonus": 116.4, "transfer": 111, "transfer_applied": 111},
         "flights": 795, "streak": {"current": 3, "best": 10},
         "vatsim_cid": "1234567", "joined_at": "2025-01-01T00:00:00+00:00"}
    """.trimIndent()

    @Test
    fun `me maps every field`() {
        val pilot = FdvApi.interpretMe(200, me)

        assertEquals("DAL1234", pilot.callsign)
        assertEquals("Shawn Pilot", pilot.name)
        assertEquals("First Officer", pilot.rank)
        assertEquals("KATL", pilot.hubIcao)
        assertEquals("Atlanta", pilot.hubName)
        assertEquals(3107.1, pilot.hours!!, 0.0)
        assertEquals(795, pilot.flights)
        assertEquals(3, pilot.streakCurrent)
        assertEquals(10, pilot.streakBest)
    }

    @Test
    fun `me inside a data envelope is unwrapped`() {
        assertEquals("DAL1234", FdvApi.interpretMe(200, """{"data": $me}""").callsign)
    }

    @Test
    fun `pilot with no hub and one name still maps`() {
        val pilot = FdvApi.interpretMe(200, """{"callsign": "DAL9", "firstname": "Shawn", "hub": null}""")
        assertEquals("Shawn", pilot.name)
        assertNull(pilot.hubIcao)
    }

    @Test
    fun `401 means signed out`() {
        val e = assertThrows(FdvApiException::class.java) {
            FdvApi.interpretMe(401, """{"success": false, "error": "invalid_token", "message": "Token expired"}""")
        }
        assertTrue(e.signedOut)
        assertEquals("Your FDV sign-in has expired. Sign in again.", e.message)
    }

    @Test
    fun `server error keeps the session and shows FDV's message`() {
        val e = assertThrows(FdvApiException::class.java) {
            FdvApi.interpretMe(500, """{"message": "Server Error"}""")
        }
        assertFalse(e.signedOut)
        assertEquals("Server Error", e.message)
    }

    @Test
    fun `non-JSON error falls back to the status code`() {
        val e = assertThrows(FdvApiException::class.java) { FdvApi.interpretMe(503, "<html>down</html>") }
        assertEquals("FlyDeltaVirtual returned an error (HTTP 503). Try again in a moment.", e.message)
    }

    @Test
    fun `missing callsign is unreadable`() {
        val e = assertThrows(FdvApiException::class.java) { FdvApi.interpretMe(200, """{"firstname": "Shawn"}""") }
        assertEquals("FlyDeltaVirtual sent a response the app couldn't read.", e.message)
    }

    @Test
    fun `me maps rank progress, location and hours breakdown`() {
        val pilot = FdvApi.interpretMe(200, me)

        assertEquals("Sky Team Executive Captain", pilot.nextRank)
        assertEquals(892.9, pilot.nextRankHoursRemaining!!, 0.0)
        assertEquals(0.188f, pilot.rankProgress!!, 0.001f)     // (3107.1 - 2900) / (4000 - 2900)
        assertEquals("KATL", pilot.lastFlightLocation)
        assertEquals(2879.6, pilot.hoursFlown!!, 0.0)
        assertEquals(116.4, pilot.hoursBonus!!, 0.0)
        assertEquals(111.0, pilot.hoursTransfer!!, 0.0)
    }

    @Test
    fun `top rank has no next rank and no progress`() {
        val pilot = FdvApi.interpretMe(
            200,
            """{"callsign": "DAL1", "rank": {"name": "Chief Pilot", "hours_required": 10000}, "next_rank": null, "hours": 12000}"""
        )
        assertNull(pilot.nextRank)
        assertNull(pilot.rankProgress)
    }

    @Test
    fun `hub fallback location is not a last landing`() {
        val pilot = FdvApi.interpretMe(
            200,
            """{"callsign": "DAL1", "location": {"icao": "KBOS", "source": "hub"}}"""
        )
        assertNull(pilot.lastFlightLocation)
    }
}