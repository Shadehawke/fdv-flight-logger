package com.fdv.fdvflightlogger.data.history

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class HistoryFilterTest {

    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 23)

    private fun at(day: LocalDate) = day.atStartOfDay(zone).toInstant().toEpochMilli() + 3_600_000

    private val flights = listOf(
        FlightLogEntity(id = 1, createdAtEpochMs = at(today), dep = "KATL", arr = "KJFK",
            airline = "DAL", aircraft = "A321", fdvPirepId = 5, fdvStatus = "approved"),
        FlightLogEntity(id = 2, createdAtEpochMs = at(today.minusDays(6)), dep = "KATL", arr = "KJFK",
            airline = "dal ", aircraft = "B738", fdvPirepId = 6, fdvStatus = "pending"),
        FlightLogEntity(id = 3, createdAtEpochMs = at(today.minusDays(7)), dep = "LFPG", arr = "KATL",
            airline = "AFR", aircraft = "A321", flightType = "OFFLINE_NO_ATC"),
        FlightLogEntity(id = 4, createdAtEpochMs = at(LocalDate.of(2025, 12, 31)), dep = "KATL", arr = "KMCO",
            airline = "DAL", fdvPirepId = 7)                       // linked, not checked yet
    )

    private fun ids(filter: HistoryFilter) = flights.filter { filter.matches(it, today, zone) }.map { it.id }

    @Test
    fun `no filter shows everything`() {
        assertEquals(listOf(1L, 2L, 3L, 4L), ids(HistoryFilter()))
        assertFalse(HistoryFilter().isActive)
    }

    @Test
    fun `date presets include today and count back`() {
        assertEquals(listOf(1L, 2L), ids(HistoryFilter(datePreset = DatePreset.LAST_7)))      // 6 days ago is in, 7 is out
        assertEquals(listOf(1L, 2L, 3L), ids(HistoryFilter(datePreset = DatePreset.THIS_YEAR)))
    }

    @Test
    fun `custom ranges are inclusive and can be open-ended`() {
        val week = HistoryFilter(
            datePreset = DatePreset.CUSTOM,
            fromDay = today.minusDays(7).toEpochDay(),
            toDay = today.minusDays(6).toEpochDay()
        )
        assertEquals(listOf(2L, 3L), ids(week))
        val untilNewYear = HistoryFilter(datePreset = DatePreset.CUSTOM, toDay = LocalDate.of(2026, 1, 1).toEpochDay())
        assertEquals(listOf(4L), ids(untilNewYear))
    }

    @Test
    fun `values in one filter widen it, separate filters narrow it`() {
        assertEquals(listOf(1L, 2L, 3L, 4L), ids(HistoryFilter(airlines = setOf("DAL", "AFR"))))
        assertEquals(listOf(1L, 2L, 4L), ids(HistoryFilter(airlines = setOf("DAL"))))            // "dal " counts as DAL
        assertEquals(listOf(1L), ids(HistoryFilter(airlines = setOf("DAL"), aircraft = setOf("A321"))))
        assertEquals(listOf(3L), ids(HistoryFilter(flightTypes = setOf("OFFLINE_NO_ATC"))))
    }

    @Test
    fun `FDV status treats linked-but-unchecked flights as approved`() {
        assertEquals(listOf(1L, 4L), ids(HistoryFilter(standings = setOf(FdvStanding.APPROVED))))
        assertEquals(listOf(2L), ids(HistoryFilter(standings = setOf(FdvStanding.PENDING))))
        assertEquals(listOf(3L), ids(HistoryFilter(standings = setOf(FdvStanding.NOT_FILED))))
    }

    @Test
    fun `choices are counted and most-flown first`() {
        assertEquals(listOf("DAL" to 3, "AFR" to 1), HistoryFilter.choices(flights.map { it.airline }))
    }

    @Test
    fun `a filter survives being saved as text`() {
        val filter = HistoryFilter(datePreset = DatePreset.LAST_30, airlines = setOf("DAL"), standings = setOf(FdvStanding.PENDING))
        val text = Json.encodeToString(HistoryFilter.serializer(), filter)
        assertEquals(filter, Json.decodeFromString(HistoryFilter.serializer(), text))
        assertTrue(filter.isActive)
    }
}