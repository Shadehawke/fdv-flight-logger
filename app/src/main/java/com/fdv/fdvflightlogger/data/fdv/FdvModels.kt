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
    @SerialName("schedule_id") val scheduleId: Long? = null,
    @SerialName("charter_id") val charterId: Long? = null,
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
    val arrGate: String?,
    // The same IDs PIREPs carry from 2026-11-11 on, so sync can match this flight exactly
    val scheduleId: Long?,
    val charterId: Long?
)

// ---- /pireps and /pireps/{id} ----

@Serializable
internal data class PirepDto(
    val id: Long? = null,
    val flight: String? = null,
    val airline: CodeNameDto? = null,
    val departure: CodeNameDto? = null,
    val arrival: CodeNameDto? = null,
    val aircraft: CodeNameDto? = null,
    val status: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("fuel_used_lbs") val fuelUsedLbs: Double? = null,
    val passengers: Int? = null,
    @SerialName("landing_rate_fpm") val landingRateFpm: Int? = null,
    @SerialName("online_percent") val onlinePercent: Int? = null,
    val route: String? = null,
    @SerialName("schedule_id") val scheduleId: Long? = null,
    @SerialName("charter_id") val charterId: Long? = null,
    // Only in /pireps/{id}
    val log: List<PirepLogDto>? = null
)

@Serializable
internal data class PirepLogDto(val time: String? = null, val text: String? = null)

@Serializable
internal data class PageMetaDto(
    val page: Int? = null,
    @SerialName("last_page") val lastPage: Int? = null,
    val total: Int? = null
)

/** One filed flight, in the units and shapes the logbook uses. */
data class FdvPirep(
    val id: Long,
    val callsign: String,
    val airline: String?,
    val flightNumber: String?,
    val dep: String,
    val arr: String,
    val aircraft: String?,
    val status: String?,              // approved | pending | rejected
    val submittedAtMillis: Long?,
    val blockMinutes: Int?,           // duration_minutes: pushback to arrival
    val airMinutes: Int?,             // from the Black Box log, so only on /pireps/{id}
    val fuelUsedLbs: Double?,         // always pounds; null when Black Box didn't capture it
    val pax: Int?,
    val landingRateFpm: Int?,
    val online: Boolean,              // FDV counts any online_percent above 0
    val route: String?,
    val scheduleId: Long?,            // set on flights filed from 2026-11-11 on
    val charterId: Long?
)

data class FdvPirepPage(
    val pireps: List<FdvPirep>,
    val page: Int,
    val lastPage: Int,
    val total: Int?
) {
    val hasMore: Boolean get() = page < lastPage
}