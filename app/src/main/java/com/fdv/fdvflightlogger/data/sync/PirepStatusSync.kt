package com.fdv.fdvflightlogger.data.sync

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.data.db.PirepStatus
import com.fdv.fdvflightlogger.data.db.pirepStatus
import com.fdv.fdvflightlogger.data.fdv.FdvPirep

internal object PirepStatusSync {

    /** Linked flights whose review could still change: pending, or never checked. */
    fun awaiting(flights: List<FlightLogEntity>): List<FlightLogEntity> =
        flights.filter { it.fdvPirepId != null && (it.fdvStatus == null || it.pirepStatus == PirepStatus.PENDING) }

    /** Flight id → FDV's current status, for [open] flights whose status has changed. */
    fun changes(open: List<FlightLogEntity>, pireps: List<FdvPirep>): Map<Long, String> {
        val onFdv = pireps
            .mapNotNull { p -> p.status?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let { p.id to it } }
            .toMap()
        return open
            .mapNotNull { flight ->
                val latest = onFdv[flight.fdvPirepId] ?: return@mapNotNull null
                if (latest == flight.fdvStatus) null else flight.id to latest
            }
            .toMap()
    }

    /** PIREPs come newest first: once a page reaches the oldest flight we're waiting on, later pages can't help. */
    fun reachedOldest(page: List<FdvPirep>, oldestWanted: Long): Boolean = page.any { it.id <= oldestWanted }
}