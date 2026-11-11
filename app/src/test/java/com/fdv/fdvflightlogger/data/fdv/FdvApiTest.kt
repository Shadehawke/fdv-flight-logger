package com.fdv.fdvflightlogger.data.fdv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FdvApiTest {

    private val me = """
        {"id": "42", "callsign": "DAL1234", "firstname": "Shawn", "lastname": "Pilot",
         "rank": {"id": 12, "name": "First Officer"}, "hub": {"id": 3, "name": "Atlanta", "icao": "KATL"},
         "hours": 123.4, "flights": 56, "streak": {"current": 3, "best": 10},
         "vatsim_cid": 1234567, "joined_at": "2025-01-01"}
    """.trimIndent()

    @Test
    fun `me maps every field`() {
        val pilot = FdvApi.interpretMe(200, me)

        assertEquals("DAL1234", pilot.callsign)
        assertEquals("Shawn Pilot", pilot.name)
        assertEquals("First Officer", pilot.rank)
        assertEquals("KATL", pilot.hubIcao)
        assertEquals("Atlanta", pilot.hubName)
        assertEquals(123.4, pilot.hours!!, 0.0)
        assertEquals(56, pilot.flights)
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
}