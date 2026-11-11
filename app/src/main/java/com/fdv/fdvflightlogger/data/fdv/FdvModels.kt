package com.fdv.fdvflightlogger.data.fdv

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import com.fdv.fdvflightlogger.data.simbrief.SimBriefPlan

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

// ---- /bids and /bids/{id} ----

@Serializable
internal data class BidDto(
    val id: Long? = null,
    val flight: String? = null,
    val airline: CodeNameDto? = null,
    val departure: CodeNameDto? = null,
    val arrival: CodeNameDto? = null,
    val aircraft: CodeNameDto? = null,
    @SerialName("depart_utc") val departUtc: String? = null,
    val duration: String? = null,
    @SerialName("cruise_fl") val cruiseFl: Int? = null,
    val route: String? = null,
    val gates: GatesDto? = null,
    @SerialName("simbrief_ofp_id") val simbriefOfpId: String? = null,
    // Only in /bids/{id}, and only when a plan exists for this exact route
    val simbrief: BidSimBriefDto? = null
)

/** FDV's {icao, name} shape, used for airlines, airports, aircraft and alternates. */
@Serializable
internal data class CodeNameDto(val icao: String? = null, val name: String? = null)

@Serializable
internal data class GatesDto(
    val departure: GateDto? = null,
    val arrival: GateDto? = null,
    val alternate: GateDto? = null
)

@Serializable
internal data class GateDto(val terminal: String? = null, val gate: String? = null)

@Serializable
internal data class BidSimBriefDto(
    val units: String? = null,
    val route: String? = null,
    @SerialName("initial_fl") val initialFl: Int? = null,
    @SerialName("cost_index") val costIndex: String? = null,
    @SerialName("enroute_minutes") val enrouteMinutes: Int? = null,
    @SerialName("departure_runway") val departureRunway: String? = null,
    @SerialName("arrival_runway") val arrivalRunway: String? = null,
    val alternate: CodeNameDto? = null,
    val fuel: BidFuelDto? = null,
    val load: BidLoadDto? = null,
    val weights: BidWeightsDto? = null
)

// Ints on the wire, Doubles here because SimBriefPlan and the weight conversion use Doubles
@Serializable
internal data class BidFuelDto(val block: Double? = null, val reserve: Double? = null)

@Serializable
internal data class BidLoadDto(val passengers: Int? = null)

@Serializable
internal data class BidWeightsDto(@SerialName("zero_fuel") val zeroFuel: Double? = null)

/** One booking, as the bid picker lists it. */
data class FdvBid(
    val id: Long,
    val callsign: String,
    val dep: String,
    val arr: String,
    val aircraft: String?,
    val departUtc: String?,   // "21:16", the timetable's time of day
    val hasSimBrief: Boolean
)

/** A booking ready to apply to a flight: the plan goes through the SimBrief mapper, gates are added on top. */
data class FdvBidPlan(
    val plan: SimBriefPlan,
    val depGate: String?,
    val arrGate: String?
)