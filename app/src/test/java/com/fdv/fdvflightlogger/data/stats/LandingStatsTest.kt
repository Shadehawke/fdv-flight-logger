package com.fdv.fdvflightlogger.data.stats

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LandingStatsTest {

    private fun flight(id: Long, rate: Int?, aircraft: String? = "A321") = FlightLogEntity(
        id = id, createdAtEpochMs = id, dep = "KATL", arr = "KJFK", aircraft = aircraft, landingRateFpm = rate
    )

    @Test
    fun `ratings include their upper limit and ignore the sign`() {
        assertEquals(LandingRating.FLOATED, LandingRating.of(-59))
        assertEquals(LandingRating.BUTTER, LandingRating.of(60))
        assertEquals(LandingRating.BUTTER, LandingRating.of(-100))
        assertEquals(LandingRating.SMOOTH, LandingRating.of(101))
        assertEquals(LandingRating.SMOOTH, LandingRating.of(-250))
        assertEquals(LandingRating.FIRM, LandingRating.of(251))
        assertEquals(LandingRating.FIRM, LandingRating.of(-400))
        assertEquals(LandingRating.HARD, LandingRating.of(401))
        assertEquals(LandingRating.HARD, LandingRating.of(-600))
        assertEquals(LandingRating.VERY_HARD, LandingRating.of(601))
        assertNull(LandingRating.of(0))          // FDV's "not captured"
        assertNull(LandingRating.of(null))
    }

    @Test
    fun `stats skip flights without a landing rate`() {
        val stats = LandingStatsCalculator.compute(
            listOf(
                flight(1, -120), flight(2, -80), flight(3, 0), flight(4, null),
                flight(5, 250, "B738"), flight(6, -300, "B738"), flight(7, -90, "B738"), flight(8, -600, "E75L")
            )
        )
        assertEquals(6, stats.landings)
        assertEquals(240, stats.averageFpm)        // (120 + 80 + 250 + 300 + 90 + 600) / 6
        assertEquals(2L, stats.best?.id)           // 80 fpm
        // 80 and 90 are Butter; 120 and 250 Smooth; 300 Firm; 600 Hard
        assertEquals(
            mapOf(LandingRating.BUTTER to 2, LandingRating.SMOOTH to 2, LandingRating.FIRM to 1, LandingRating.HARD to 1),
            stats.bands
        )
        // Only the B738 has 3+ landings: (250 + 300 + 90) / 3
        assertEquals(listOf(AircraftLandings("B738", 213, 3)), stats.byAircraft)
    }

    @Test
    fun `a float isn't the best landing`() {
        val stats = LandingStatsCalculator.compute(listOf(flight(1, -40), flight(2, -85), flight(3, -180)))
        assertEquals(2L, stats.best?.id)           // 85 fpm, not the 40 fpm float
        assertEquals(1, stats.bands[LandingRating.FLOATED])
    }

    @Test
    fun `last 10 are the most recent landings`() {
        // 10 older 300 fpm landings, then 10 recent 100 fpm ones
        val flights = (1L..10L).map { flight(it, -300) } + (11L..20L).map { flight(it, -100) }
        val stats = LandingStatsCalculator.compute(flights)
        assertEquals(200, stats.averageFpm)
        assertEquals(100, stats.recentAverageFpm)
    }

    @Test
    fun `no landing rates, no stats`() {
        assertEquals(LandingStats(), LandingStatsCalculator.compute(listOf(flight(1, null), flight(2, 0))))
    }
}