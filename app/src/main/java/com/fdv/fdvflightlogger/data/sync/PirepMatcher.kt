package com.fdv.fdvflightlogger.data.sync

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.data.fdv.FdvPirep
import com.fdv.fdvflightlogger.data.fdv.flightNumberOf
import kotlin.math.abs

/** Finds the PIREP that belongs to a logbook flight. Pure, so every rule is unit tested. */
internal object PirepMatcher {

    // A PIREP is filed within hours of the flight; two days allows for logging the entry late
    const val WINDOW_MS = 2L * 24 * 60 * 60 * 1000

    fun match(flight: FlightLogEntity, candidates: List<FdvPirep>, taken: Set<Long>): FdvPirep? {
        val nearby = candidates.filter { pirep ->
            pirep.id !in taken &&
                    pirep.dep.equals(flight.dep.trim(), ignoreCase = true) &&
                    pirep.arr.equals(flight.arr.trim(), ignoreCase = true) &&
                    pirep.submittedAtMillis?.let { abs(it - flight.createdAtEpochMs) <= WINDOW_MS } == true
        }

        // 1. Started from this booking (schedule IDs repeat daily, hence the time window above)
        nearby.filter { it.sameBookingAs(flight) }.closestTo(flight)?.let { return it }

        // 3. No flight number to compare: accept only an unambiguous single candidate
        val ourNumber = flightNumberOf(flight.flightNumber, flight.airline) ?: return nearby.singleOrNull()

        // 2. Same flight number, and airline when both sides know it
        return nearby.filter { pirep ->
            pirep.flightNumber.equals(ourNumber, ignoreCase = true) &&
                    (flight.airline.isNullOrBlank() || pirep.airline == null ||
                            pirep.airline.equals(flight.airline.trim(), ignoreCase = true))
        }.closestTo(flight)
    }

    private fun FdvPirep.sameBookingAs(flight: FlightLogEntity): Boolean =
        (flight.fdvScheduleId != null && scheduleId == flight.fdvScheduleId) ||
                (flight.fdvCharterId != null && charterId == flight.fdvCharterId)

    // Every PIREP left here has submittedAtMillis (the window filter required it)
    private fun List<FdvPirep>.closestTo(flight: FlightLogEntity): FdvPirep? =
        minByOrNull { abs(it.submittedAtMillis!! - flight.createdAtEpochMs) }
}