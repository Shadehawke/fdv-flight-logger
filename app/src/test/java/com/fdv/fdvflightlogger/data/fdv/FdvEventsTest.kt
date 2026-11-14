package com.fdv.fdvflightlogger.data.fdv

import com.fdv.fdvflightlogger.ui.components.eventRoute
import com.fdv.fdvflightlogger.ui.components.eventTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class FdvEventsTest {

    // Listed out of order on purpose; the third has no dates and must be skipped
    private val body = """
        {"data": [
          {"id": 302, "name": "Seattle Fly-In", "category": "Fly In", "status": "upcoming",
           "starts_at": "2026-11-21T18:00:00+00:00", "ends_at": "2026-11-21T22:00:00+00:00",
           "departure": null, "arrival": {"icao": "KSEA", "name": "Seattle-Tacoma Intl"},
           "description": "", "image_url": null, "url": "https://flydeltavirtual.com/events/302",
           "registered": false, "registered_pilots": 3, "max_pilots": null},
          {"id": 301, "name": "Tuesday Night Group Flight", "category": "Group Flight", "status": "current",
           "starts_at": "2026-11-17T00:00:00+00:00", "ends_at": "2026-11-17T03:00:00+00:00",
           "departure": {"icao": "katl", "name": "Hartsfield-Jackson Atlanta Intl"},
           "arrival": {"icao": "KJFK", "name": "John F Kennedy Intl"},
           "description": "Join us!\r\n\r\n✈️ Gates open at 2330Z",
           "image_url": "https://flydeltavirtual.com/public/banner.jpg",
           "url": "https://flydeltavirtual.com/events/301",
           "registered": true, "registered_pilots": 14, "max_pilots": 40},
          {"id": 303, "name": "No dates"}
        ]}
    """.trimIndent()

    private fun millis(iso: String) = Instant.parse(iso).toEpochMilli()

    @Test
    fun `events map from the live shape, soonest first`() {
        val events = FdvApi.interpretEvents(200, body)

        assertEquals(listOf(301L, 302L), events.map { it.id })

        val group = events[0]
        assertEquals("KATL", group.departureIcao)              // uppercased
        assertEquals("Join us!\n\n✈️ Gates open at 2330Z", group.description)
        assertEquals(40, group.maxPilots)
        assertTrue(group.registered)
        assertEquals("KATL → KJFK", eventRoute(group))

        val flyIn = events[1]
        assertNull(flyIn.departureIcao)
        assertNull(flyIn.maxPilots)
        assertEquals("Fly-in to KSEA", eventRoute(flyIn))
    }

    @Test
    fun `live is worked out from the times`() {
        val group = FdvApi.interpretEvents(200, body)[0]
        assertFalse(group.isLiveAt(millis("2026-11-16T23:59:59Z")))
        assertTrue(group.isLiveAt(millis("2026-11-17T00:00:00Z")))
        assertFalse(group.isLiveAt(millis("2026-11-17T03:00:00Z")))   // the end doesn't count
        assertTrue(group.hasEndedAt(millis("2026-11-17T03:00:00Z")))
    }

    @Test
    fun `timing counts down in the last hour`() {
        val flyIn = FdvApi.interpretEvents(200, body)[1]
        val now = millis("2026-11-21T17:35:00Z")
        assertEquals("Starts in 25 min", eventTiming(flyIn, now, ZoneOffset.UTC))

        val dayBefore = millis("2026-11-20T12:00:00Z")
        assertTrue(eventTiming(flyIn, dayBefore, ZoneOffset.UTC).startsWith("Starts tomorrow"))
        assertTrue(eventTiming(flyIn, dayBefore, ZoneOffset.UTC).endsWith("(1800Z)"))
    }
}