package com.fdv.fdvflightlogger.data.sync

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.data.db.FlightType
import com.fdv.fdvflightlogger.data.fdv.FdvPirep
import com.fdv.fdvflightlogger.data.prefs.WeightUnit

/** What an import would do, worked out before anything is written so the pilot can decide. */
data class ImportPlan(
    val matched: List<Pair<FlightLogEntity, FdvPirep>>,  // logbook flights that get actuals
    val unmatched: List<FdvPirep>,                       // PIREPs with no flight in the logbook
    val alreadyLinked: Int,                              // PIREPs synced before; skipped
    val highestPirepId: Long?                            // where ongoing sync carries on from
) {
    val total: Int get() = matched.size + unmatched.size + alreadyLinked
}

internal object HistoryImport {

    fun plan(flights: List<FlightLogEntity>, pireps: List<FdvPirep>): ImportPlan {
        val linked = flights.mapNotNullTo(HashSet()) { it.fdvPirepId }
        val fresh = pireps.filter { it.id !in linked }

        // Grows as flights claim PIREPs, so no PIREP is ever handed to two flights
        val taken = HashSet(linked)
        val matched = mutableListOf<Pair<FlightLogEntity, FdvPirep>>()
        for (flight in flights) {
            if (flight.fdvPirepId != null) continue
            val pirep = PirepMatcher.match(flight, fresh, taken) ?: continue
            taken += pirep.id
            matched += flight to pirep
        }

        return ImportPlan(
            matched = matched,
            unmatched = fresh.filter { it.id !in taken },
            alreadyLinked = pireps.size - fresh.size,
            highestPirepId = pireps.maxOfOrNull { it.id }
        )
    }
}

/**
 * A new logbook flight built from a PIREP. It starts as offline-without-ATC (the only safe guess),
 * then gets the same actuals a synced flight would; FDV's online flag upgrades it to Online.
 */
internal fun FdvPirep.toImportedFlight(fuelUnit: WeightUnit, nowMillis: Long): FlightLogEntity =
    FlightLogEntity(
        createdAtEpochMs = submittedAtMillis ?: nowMillis,
        dep = dep,
        arr = arr,
        flightType = FlightType.OFFLINE_NO_ATC.name
    ).withActuals(this, fuelUnit).copy(importedFromFdv = true)