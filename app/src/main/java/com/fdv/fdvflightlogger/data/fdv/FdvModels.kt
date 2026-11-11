package com.fdv.fdvflightlogger.data.fdv

import kotlinx.serialization.Serializable

/** GET /me, as FDV sends it. id and vatsim_cid are left out: their types aren't confirmed yet. */
@Serializable
internal data class MeDto(
    val callsign: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val rank: RankDto? = null,
    val hub: HubDto? = null,
    val hours: Double? = null,
    val flights: Int? = null,
    val streak: StreakDto? = null
)

@Serializable
internal data class HubDto(val icao: String? = null, val name: String? = null)

@Serializable
internal data class RankDto(val name: String? = null)

@Serializable
internal data class StreakDto(val current: Int? = null, val best: Int? = null)

/** The signed-in pilot's FDV profile, as the app shows it. */
data class FdvPilot(
    val callsign: String,
    val name: String,
    val rank: String?,
    val hubIcao: String?,
    val hubName: String?,
    val hours: Double?,
    val flights: Int?,
    val streakCurrent: Int?,
    val streakBest: Int?
)