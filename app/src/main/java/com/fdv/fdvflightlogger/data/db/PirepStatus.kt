package com.fdv.fdvflightlogger.data.db

/** FDV's review of a filed flight. Stored in the database as FDV's own lowercase word. */
enum class PirepStatus(val wire: String, val label: String) {
    APPROVED("approved", "Approved"),
    PENDING("pending", "Pending"),
    REJECTED("rejected", "Rejected");

    companion object {
        fun of(raw: String?): PirepStatus? {
            val word = raw?.trim()?.lowercase() ?: return null
            return entries.firstOrNull { it.wire == word }
        }
    }
}

val FlightLogEntity.pirepStatus: PirepStatus? get() = PirepStatus.of(fdvStatus)

/**
 * FDV counts approved flights only. A flight that was never filed, or hasn't been checked yet,
 * is the pilot's own logbook and counts.
 */
val FlightLogEntity.countsTowardStats: Boolean
    get() = when (pirepStatus) {
        PirepStatus.PENDING, PirepStatus.REJECTED -> false
        PirepStatus.APPROVED, null -> true
    }