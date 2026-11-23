package com.fdv.fdvflightlogger.data.history

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.data.db.PirepStatus
import com.fdv.fdvflightlogger.data.db.pirepStatus
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Date choices in the History filter. CUSTOM uses the filter's own from/to days. */
enum class DatePreset(val label: String) {
    ANY("Any time"),
    LAST_7("Last 7 days"),
    LAST_30("Last 30 days"),
    LAST_90("Last 90 days"),
    THIS_YEAR("This year"),
    CUSTOM("Custom range")
}

/** A flight's standing on FDV, for filtering: its review status, or never filed. */
enum class FdvStanding(val label: String) {
    APPROVED("Approved"),
    PENDING("Pending"),
    REJECTED("Rejected"),
    NOT_FILED("Not on FDV");

    companion object {
        fun of(flight: FlightLogEntity): FdvStanding = when (flight.pirepStatus) {
            PirepStatus.APPROVED -> APPROVED
            PirepStatus.PENDING -> PENDING
            PirepStatus.REJECTED -> REJECTED
            // Linked but not checked yet counts as approved until the status check says otherwise,
            // the same way the dashboard counts it
            null -> if (flight.fdvPirepId != null) APPROVED else NOT_FILED
        }
    }
}

/**
 * What History shows. Empty sets mean "any". Days are LocalDate epoch days, so the filter saves as
 * plain numbers and means the same calendar days in any time zone.
 */
@Serializable
data class HistoryFilter(
    val datePreset: DatePreset = DatePreset.ANY,
    val fromDay: Long? = null,                 // CUSTOM only, inclusive
    val toDay: Long? = null,                   // CUSTOM only, inclusive
    val airlines: Set<String> = emptySet(),
    val aircraft: Set<String> = emptySet(),
    val flightTypes: Set<String> = emptySet(), // FlightType names
    val standings: Set<FdvStanding> = emptySet()
) {
    val isActive: Boolean get() = this != HistoryFilter()

    /** The inclusive day range this filter means [today], or null for any time. */
    fun dayRange(today: LocalDate): ClosedRange<LocalDate>? = when (datePreset) {
        DatePreset.ANY -> null
        DatePreset.LAST_7 -> today.minusDays(6)..today
        DatePreset.LAST_30 -> today.minusDays(29)..today
        DatePreset.LAST_90 -> today.minusDays(89)..today
        DatePreset.THIS_YEAR -> today.withDayOfYear(1)..today
        DatePreset.CUSTOM -> {
            val from = fromDay?.let(LocalDate::ofEpochDay) ?: LocalDate.MIN
            val to = toDay?.let(LocalDate::ofEpochDay) ?: LocalDate.MAX
            from..to
        }
    }

    /** Within a filter any chosen value matches (DAL or AFR); separate filters must all match. */
    fun matches(flight: FlightLogEntity, today: LocalDate, zone: ZoneId): Boolean {
        dayRange(today)?.let { range ->
            val day = Instant.ofEpochMilli(flight.createdAtEpochMs).atZone(zone).toLocalDate()
            if (day !in range) return false
        }
        if (airlines.isNotEmpty() && flight.airline.code() !in airlines) return false
        if (aircraft.isNotEmpty() && flight.aircraft.code() !in aircraft) return false
        if (flightTypes.isNotEmpty() && flight.flightType !in flightTypes) return false
        if (standings.isNotEmpty() && FdvStanding.of(flight) !in standings) return false
        return true
    }

    companion object {
        /** Each value in the logbook with how many flights have it, most-flown first: a filter's choices. */
        fun choices(values: List<String?>): List<Pair<String, Int>> =
            values.mapNotNull { it.code() }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .map { it.key to it.value }
    }
}

// Airlines and aircraft are compared as trimmed upper-case codes, like everywhere else in the app
private fun String?.code(): String? = this?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }