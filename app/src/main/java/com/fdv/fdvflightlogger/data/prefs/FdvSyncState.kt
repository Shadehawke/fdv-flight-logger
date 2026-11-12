package com.fdv.fdvflightlogger.data.prefs

/** How the logbook follows FDV: every PIREP becomes a flight, or only flights the pilot logged get filled in. */
enum class FdvImportMode { ALL, MATCH_ONLY }

/** Null mode means history has never been imported (or the import was removed). */
data class FdvSyncState(
    val mode: FdvImportMode? = null,
    val lastPirepId: Long? = null
)