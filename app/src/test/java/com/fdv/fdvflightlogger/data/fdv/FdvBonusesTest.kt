package com.fdv.fdvflightlogger.data.fdv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FdvBonusesTest {

    private val body = """
        {"data": [
          {"type": "airport_of_the_month", "label": "Boston Logan",
           "departure": {"icao": "kbos", "name": "Boston Logan Intl"}, "arrival": {"icao": "KBOS", "name": "Boston Logan Intl"},
           "starts_at": "2026-11-01T00:00:00+00:00", "ends_at": "2026-12-01T00:00:00+00:00",
           "category_free": true, "double_hours": false, "match": "either_end"},
          {"type": "flight_of_the_day", "label": "ATL to LAX",
           "departure": {"icao": "KATL", "name": "Atlanta"}, "arrival": {"icao": "KLAX", "name": "Los Angeles"},
           "starts_at": "2026-11-22T00:00:00+00:00", "ends_at": "2026-11-23T00:00:00+00:00",
           "category_free": true, "double_hours": true, "match": "route"},
          {"type": "bonus", "label": "Some future rule", "starts_at": "2026-11-22T00:00:00+00:00",
           "ends_at": "2026-11-23T00:00:00+00:00", "match": "something_new"}
        ]}
    """.trimIndent()

    @Test
    fun `bonuses map from the described shape`() {
        val bonuses = FdvApi.interpretBonuses(200, body)

        assertEquals(2, bonuses.size)                      // the unknown match rule is left out
        val aotm = bonuses[0]
        assertEquals(BonusMatch.EITHER_END, aotm.match)
        assertEquals("KBOS", aotm.departureIcao)           // uppercased
        assertTrue(aotm.categoryFree)
        assertFalse(aotm.doubleHours)
        assertEquals("Into or out of KBOS", FlightBonuses.where(aotm))

        val fotd = bonuses[1]
        assertEquals(BonusMatch.ROUTE, fotd.match)
        assertEquals("KATL → KLAX", FlightBonuses.where(fotd))
        assertEquals("CAT Free · double hours", FlightBonuses.perks(fotd.categoryFree, fotd.doubleHours))
    }
}