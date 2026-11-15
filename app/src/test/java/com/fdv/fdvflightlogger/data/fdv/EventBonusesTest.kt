package com.fdv.fdvflightlogger.data.fdv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventBonusesTest {

    private val hour = 60 * 60 * 1000L
    private val now = 1_000 * hour

    private fun event(
        id: Long, category: String, dep: String?, arr: String?,
        startsIn: Long = -hour, length: Long = 3 * hour
    ) = FdvEvent(
        id = id, name = "Event $id", category = category,
        startsAtMillis = now + startsIn, endsAtMillis = now + startsIn + length,
        departureIcao = dep, departureName = null, arrivalIcao = arr, arrivalName = null,
        description = "", imageUrl = null, url = null,
        registered = false, registeredPilots = 0, maxPilots = null
    )

    @Test
    fun `each category matches the airports its own way`() {
        val group = event(1, "Group Flight", "KATL", "KJFK")
        assertTrue(EventBonuses.matches(group, "katl", "KJFK"))
        assertFalse(EventBonuses.matches(group, "KATL", "KBOS"))

        assertTrue(EventBonuses.matches(event(2, "Fly In", null, "KSEA"), "KLAX", "KSEA"))
        assertTrue(EventBonuses.matches(event(3, "Fly-Out", "KDTW", null), "KDTW", "KMSP"))   // punctuation ignored
        assertTrue(EventBonuses.matches(event(4, "In or Out", "KSLC", null), "KDEN", "KSLC")) // arriving counts

        // A group flight with no airports must not match everything
        assertFalse(EventBonuses.matches(event(5, "Group Flight", null, null), "KATL", "KJFK"))
    }

    @Test
    fun `other events don't qualify`() {
        val other = event(6, "Other", "KATL", "KJFK")
        assertFalse(EventBonuses.qualifies(other))
        assertNull(EventBonuses.find(listOf(other), "KATL", "KJFK", now))
    }

    @Test
    fun `only live events or ones starting within 2 hours count, live first`() {
        val live = event(7, "Fly In", null, "KSEA")                                  // started an hour ago
        val soon = event(8, "Fly In", null, "KSEA", startsIn = hour)
        val later = event(9, "Fly In", null, "KSEA", startsIn = 3 * hour)
        val ended = event(10, "Fly In", null, "KSEA", startsIn = -5 * hour)

        assertEquals(EventBonus(live, live = true), EventBonuses.find(listOf(soon, ended, live), "KLAX", "KSEA", now))
        assertEquals(EventBonus(soon, live = false), EventBonuses.find(listOf(soon, later), "KLAX", "KSEA", now))
        assertNull(EventBonuses.find(listOf(later, ended), "KLAX", "KSEA", now))
    }
}