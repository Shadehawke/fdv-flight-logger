package com.fdv.fdvflightlogger.data.stats

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * How a touchdown rates, by how hard it was (fpm, either sign). Each band includes its upper limit.
 * Based on airline practice: 100–250 fpm is the normal target, above 600 fpm is the certification line
 * for a hard-landing inspection, and under 60 fpm usually means the aircraft floated.
 */
enum class LandingRating(val label: String, val maxFpm: Int) {
    FLOATED("Floated", 59),
    BUTTER("Butter", 100),
    SMOOTH("Smooth", 250),
    FIRM("Firm", 400),
    HARD("Hard", 600),
    VERY_HARD("Very hard", Int.MAX_VALUE);

    companion object {
        /** Null when there's no landing rate (FDV sends 0 when it wasn't captured). */
        fun of(fpm: Int?): LandingRating? {
            val rate = LandingStatsCalculator.softness(fpm) ?: return null
            return entries.first { rate <= it.maxFpm }
        }
    }
}

/** Average touchdown for one aircraft type. */
data class AircraftLandings(val aircraft: String, val averageFpm: Int, val landings: Int)

data class LandingStats(
    val landings: Int = 0,
    val averageFpm: Int? = null,
    val recentAverageFpm: Int? = null,                      // the last 10 landings
    val best: FlightLogEntity? = null,                      // the softest landing that didn't float
    val bands: Map<LandingRating, Int> = emptyMap(),        // how many landings in each rating
    val byAircraft: List<AircraftLandings> = emptyList()    // lowest average first; types with 3+ landings
)

object LandingStatsCalculator {

    const val RECENT = 10
    private const val MIN_FOR_AIRCRAFT = 3
    private const val AIRCRAFT_SHOWN = 5

    /** The touchdown rate as a positive number: FDV sends -142, a pilot may type 142. Null when missing or 0. */
    fun softness(fpm: Int?): Int? = fpm?.let { abs(it) }?.takeIf { it > 0 }

    fun compute(flights: List<FlightLogEntity>): LandingStats {
        val landed = flights.mapNotNull { flight -> softness(flight.landingRateFpm)?.let { flight to it } }
        if (landed.isEmpty()) return LandingStats()

        val recent = landed.sortedByDescending { (flight, _) -> flight.createdAtEpochMs }.take(RECENT)
        val byAircraft = landed
            .mapNotNull { (flight, fpm) ->
                flight.aircraft?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }?.let { it to fpm }
            }
            .groupBy({ it.first }, { it.second })
            .filterValues { it.size >= MIN_FOR_AIRCRAFT }      // one lucky landing isn't a trend
            .map { (aircraft, rates) -> AircraftLandings(aircraft, rates.average().roundToInt(), rates.size) }
            .sortedWith(compareBy<AircraftLandings> { it.averageFpm }.thenByDescending { it.landings })
            .take(AIRCRAFT_SHOWN)

        return LandingStats(
            landings = landed.size,
            averageFpm = landed.map { it.second }.average().roundToInt(),
            recentAverageFpm = recent.map { it.second }.average().roundToInt(),
            // The softest landing that didn't float: under 60 fpm usually means held off too long
            best = landed.filter { (_, fpm) -> fpm > LandingRating.FLOATED.maxFpm }.minByOrNull { it.second }?.first,
            bands = landed.groupingBy { (_, fpm) -> LandingRating.of(fpm)!! }.eachCount(),
            byAircraft = byAircraft
        )
    }
}