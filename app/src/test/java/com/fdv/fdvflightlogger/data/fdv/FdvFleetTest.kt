package com.fdv.fdvflightlogger.data.fdv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FdvFleetTest {

    private val body = """
        {"data": [
          {"id": 1, "icao": "A321", "name": "Airbus A321", "manufacturer": "Airbus", "passengers": 191,
           "wingspan_ft": 117, "range_nm": 3200, "min_rank": null, "can_fly": true, "registrations": 127},
          {"id": 2, "icao": "a359", "name": "Airbus A350-900", "manufacturer": "Airbus", "passengers": 306,
           "wingspan_ft": 212, "range_nm": 8100, "min_rank": {"id": 9, "name": "Sky Team Captain"},
           "can_fly": false, "registrations": 28},
          {"id": 3, "icao": "A321", "name": "Duplicate", "can_fly": false},
          {"id": 4, "icao": null, "name": "No type code"}
        ]}
    """.trimIndent()

    @Test
    fun `fleet maps from the live shape, keyed by type`() {
        val fleet = FdvApi.interpretFleet(200, body)

        assertEquals(setOf("A321", "A359"), fleet.keys)     // no-type row skipped, code uppercased

        val a321 = fleet.getValue("A321")
        assertTrue(a321.canFly)
        assertNull(a321.minRank)
        assertEquals("Airbus A321", a321.name)             // the first listing wins over the duplicate

        val a359 = fleet.getValue("A359")
        assertFalse(a359.canFly)
        assertEquals("Sky Team Captain", a359.minRank)
    }
}