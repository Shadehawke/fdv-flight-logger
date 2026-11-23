package com.fdv.fdvflightlogger.data.fdv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlightBonusesTest {

    private val hour = 60 * 60 * 1000L
    private val now = 1_000 * hour

    private fun event(
        id: Long, category: String, dep: String?, arr: String?,
        startsIn: Long = -hour, length: Long = 3 * hour, bonus: Boolean = true
    ) = FdvEvent(
        id = id, name = "Event $id", category = category,
        startsAtMillis = now + startsIn, endsAtMillis = now + startsIn + length,
        departureIcao = dep, departureName = null, arrivalIcao = arr, arrivalName = null,
        description = "", imageUrl = null, url = null, registered = false, registeredPilots = 0, maxPilots = null,
        categoryFree = bonus, doubleHours = bonus
    )

    private fun bonus(
        type: String, match: BonusMatch, dep: String?, arr: String?,
        startsIn: Long = -hour, length: Long = 24 * hour
    ) = FdvBonus(type, "$type label", dep, arr, now + startsIn, now + startsIn + length, true, true, match)

    private fun find(events: List<FdvEvent> = emptyList(), bonuses: List<FdvBonus> = emptyList(),
                     dep: String, arr: String, airMinutes: Int?) =
        FlightBonuses.find(events, bonuses, dep, arr, takeoff = now, airMinutes = airMinutes)

    @Test
    fun `events match by which airports they have`() {
        val group = event(1, "Group Flight", "KATL", "KJFK")
        assertTrue(FlightBonuses.eventMatches(group, "katl", "KJFK"))
        assertFalse(FlightBonuses.eventMatches(group, "KATL", "KBOS"))

        val flyInBoth = event(2, "Fly In", "KLAX", "KSEA")                 // a Fly In can have both
        assertFalse(FlightBonuses.eventMatches(flyInBoth, "KPDX", "KSEA"))
        assertTrue(FlightBonuses.eventMatches(event(3, "Fly In", null, "KSEA"), "KPDX", "KSEA"))
        assertTrue(FlightBonuses.eventMatches(event(4, "Fly Out", "KDTW", null), "KDTW", "KMSP"))

        val inOrOut = event(5, "In or Out", "KSLC", null)
        assertTrue(FlightBonuses.eventMatches(inOrOut, "KDEN", "KSLC"))
        assertTrue(FlightBonuses.eventMatches(inOrOut, "KSLC", "KDEN"))
        assertFalse(FlightBonuses.eventMatches(event(6, "Group Flight", null, null), "KATL", "KJFK"))
    }

    @Test
    fun `group flight timing`() {
        // Starts in 20 minutes: taking off now is inside the 30-minute lead-in
        val soon = event(7, "Group Flight", "KATL", "KJFK", startsIn = hour / 3)
        assertEquals(WindowStatus.QUALIFIES, find(listOf(soon), dep = "KATL", arr = "KJFK", airMinutes = 120).single().status)

        val inAnHour = event(8, "Group Flight", "KATL", "KJFK", startsIn = hour)
        assertEquals(WindowStatus.NOT_YET, find(listOf(inAnHour), dep = "KATL", arr = "KJFK", airMinutes = null).single().status)

        // Ends in an hour; a 4-hour flight lands 3 hours after the end: missed, so not shown
        val ending = event(9, "Group Flight", "KATL", "KJFK", startsIn = -2 * hour, length = 3 * hour)
        assertTrue(find(listOf(ending), dep = "KATL", arr = "KJFK", airMinutes = 240).isEmpty())
        assertEquals(WindowStatus.QUALIFIES, find(listOf(ending), dep = "KATL", arr = "KJFK", airMinutes = null).single().status)
    }

    @Test
    fun `fly in timing`() {
        val flyIn = event(10, "Fly In", null, "KSEA", startsIn = 3 * hour)
        // 3.5 h flight lands 30 minutes into the event, taking off within 4 h of the start
        assertEquals(WindowStatus.QUALIFIES, find(listOf(flyIn), dep = "KLAX", arr = "KSEA", airMinutes = 210).single().status)
        // 1 h flight would land before it starts
        assertEquals(WindowStatus.NOT_YET, find(listOf(flyIn), dep = "KLAX", arr = "KSEA", airMinutes = 60).single().status)
        // 7 h away: its window opens in 3 h, beyond the 2-hour look-ahead
        val later = event(11, "Fly In", null, "KSEA", startsIn = 7 * hour)
        assertTrue(find(listOf(later), dep = "KLAX", arr = "KSEA", airMinutes = 300).isEmpty())
    }

    @Test
    fun `in or out uses the rule for the direction flown`() {
        val inOrOut = event(12, "In or Out", "KSLC", null, startsIn = hour)
        assertEquals(inOrOut.startsAtMillis, FlightBonuses.eventWindow(inOrOut, dep = "KDEN").landFrom)   // arriving
        assertEquals(inOrOut.endsAtMillis, FlightBonuses.eventWindow(inOrOut, dep = "KSLC").takeoffBy)    // leaving
    }

    @Test
    fun `other events give nothing`() {
        assertTrue(find(listOf(event(13, "Other", "KATL", "KJFK", bonus = false)), dep = "KATL", arr = "KJFK", airMinutes = null).isEmpty())
    }

    @Test
    fun `bonuses match by their match rule`() {
        val fotd = bonus("flight_of_the_day", BonusMatch.ROUTE, "KATL", "KLAX")
        assertTrue(FlightBonuses.bonusMatches(fotd, "KATL", "KLAX"))
        assertFalse(FlightBonuses.bonusMatches(fotd, "KLAX", "KATL"))

        // Same airport at both ends: in or out of it, not a route
        val aotm = bonus("airport_of_the_month", BonusMatch.EITHER_END, "KBOS", "KBOS")
        assertTrue(FlightBonuses.bonusMatches(aotm, "KBOS", "KJFK"))
        assertTrue(FlightBonuses.bonusMatches(aotm, "KJFK", "KBOS"))
        assertFalse(FlightBonuses.bonusMatches(aotm, "KJFK", "KLGA"))

        assertTrue(FlightBonuses.bonusMatches(bonus("personal", BonusMatch.ANY, null, null), "KMSP", "KDTW"))
    }

    @Test
    fun `a bonus counts when the flight is in the air during it`() {
        val startsInAnHour = bonus("bonus", BonusMatch.ANY, null, null, startsIn = hour)
        assertEquals(WindowStatus.QUALIFIES, find(bonuses = listOf(startsInAnHour), dep = "A", arr = "B", airMinutes = 120).single().status)
        assertEquals(WindowStatus.NOT_YET, find(bonuses = listOf(startsInAnHour), dep = "A", arr = "B", airMinutes = 30).single().status)

        val over = bonus("bonus", BonusMatch.ANY, null, null, startsIn = -3 * hour, length = 2 * hour)
        assertTrue(find(bonuses = listOf(over), dep = "A", arr = "B", airMinutes = 30).isEmpty())
    }

    @Test
    fun `an event bonus repeating an event is shown once`() {
        val flyIn = event(14, "Fly In", null, "KSEA")
        val same = FdvBonus("event", "Seattle", null, "KSEA", flyIn.startsAtMillis, flyIn.endsAtMillis, true, true, BonusMatch.ARRIVAL)
        assertEquals(1, find(listOf(flyIn), listOf(same), dep = "KLAX", arr = "KSEA", airMinutes = null).size)
    }
}