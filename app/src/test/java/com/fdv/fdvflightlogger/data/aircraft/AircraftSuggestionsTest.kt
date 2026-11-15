package com.fdv.fdvflightlogger.data.aircraft

import com.fdv.fdvflightlogger.data.fdv.FdvFleetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AircraftSuggestionsTest {

    private fun type(icao: String, name: String, canFly: Boolean, minRank: String? = null) =
        FdvFleetType(icao, name, null, null, null, minRank, canFly, registrations = 1)

    private val fleet = listOf(
        type("A321", "Airbus A321", canFly = true),
        type("A359", "Airbus A350-900", canFly = false, minRank = "Sky Team Captain"),
        type("ZZ99", "Test Jet 99", canFly = true)      // not in the built-in list
    ).associateBy { it.icao }

    @Test
    fun `without a fleet the picker behaves as before`() {
        assertEquals(
            AircraftTypes.search("A32").map { it.icao },
            AircraftSuggestions.search("A32", emptyMap()).map { it.icao }
        )
    }

    @Test
    fun `flyable types come first, then locked, then not in the fleet`() {
        val icaos = AircraftSuggestions.search("AIRBUS", fleet, limit = 20).map { it.icao }
        assertEquals("A321", icaos.first())
        assertTrue(icaos.indexOf("A359") < icaos.indexOf("A320"))
    }

    @Test
    fun `a waived rank check makes locked fleet types flyable`() {
        val icaos = AircraftSuggestions.search("AIRBUS", fleet, rankWaived = true, limit = 20).map { it.icao }
        assertTrue(icaos.indexOf("A359") < 2)          // now in the flyable group with the A321
    }

    @Test
    fun `FDV-only types are suggested`() {
        assertEquals(listOf("ZZ99"), AircraftSuggestions.search("ZZ", fleet).map { it.icao })
    }

    @Test
    fun `the line under the field says what FDV thinks`() {
        assertEquals("Airbus A321 · FDV fleet", AircraftSuggestions.describe("a321", fleet))
        assertEquals("Airbus A350-900 · rank: Sky Team Captain", AircraftSuggestions.describe("A359", fleet))
        assertEquals("Airbus A350-900 · FDV fleet · rank waived", AircraftSuggestions.describe("A359", fleet, rankWaived = true))
        assertEquals("Airbus A320 · not in FDV fleet", AircraftSuggestions.describe("A320", fleet))
        assertEquals("Airbus A320", AircraftSuggestions.describe("A320", emptyMap()))
    }
}