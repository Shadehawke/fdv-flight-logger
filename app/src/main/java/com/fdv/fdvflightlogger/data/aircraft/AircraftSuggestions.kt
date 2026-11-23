package com.fdv.fdvflightlogger.data.aircraft

import com.fdv.fdvflightlogger.data.fdv.FdvFleetType

/** One row in the aircraft picker: the type, and FDV's entry for it (null when signed out or not in the fleet). */
data class AircraftSuggestion(val icao: String, val name: String, val fleet: FdvFleetType?)

object AircraftSuggestions {

    /** Flyable on this flight: the rank allows it, it's an any-rank (Wheels Up) type, or the rank check is waived. */
    fun flyable(type: FdvFleetType?, rankWaived: Boolean): Boolean =
        type != null && (type.canFly || type.anyRank || rankWaived)

    /**
     * The built-in list plus any FDV types it doesn't have. With a fleet loaded: flyable first,
     * then fleet types above the pilot's rank, then types FDV doesn't fly; search order kept within each.
     */
    fun search(
        query: String,
        fleet: Map<String, FdvFleetType>,
        rankWaived: Boolean = false,
        limit: Int = 8
    ): List<AircraftSuggestion> {
        val q = query.trim().uppercase()
        if (q.isEmpty()) return emptyList()

        // Look further than [limit] so regrouping can't push a flyable type out of the results
        val known = AircraftTypes.search(q, limit = limit * 3)
            .map { AircraftSuggestion(it.icao, it.name, fleet[it.icao]) }
        if (fleet.isEmpty()) return known.take(limit)

        val fdvOnly = fleet.values
            .filter { AircraftTypes.getByIcao(it.icao) == null }
            .filter { it.icao.startsWith(q) || it.name.uppercase().contains(q) }
            .sortedBy { it.icao }
            .map { AircraftSuggestion(it.icao, it.name, it) }

        // sortedBy is stable: items in the same group keep the order search gave them
        return (known + fdvOnly)
            .sortedBy { s ->
                when {
                    flyable(s.fleet, rankWaived) -> 0
                    s.fleet != null -> 1
                    else -> 2
                }
            }
            .take(limit)
    }

    /**
     * The line under the field: "· FDV fleet", "· any rank", "· rank waived", "· rank: …" (with a note
     * when a waiver pass would be used), "· not in FDV fleet".
     */
    fun describe(
        icao: String?,
        fleet: Map<String, FdvFleetType>,
        rankWaived: Boolean = false,
        rankWaivers: Int = 0
    ): String? {
        val code = icao?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: return null
        val onFdv = fleet[code]
        val name = AircraftTypes.getByIcao(code)?.name ?: onFdv?.name ?: return null
        if (fleet.isEmpty()) return name
        val note = when {
            onFdv == null -> "not in FDV fleet"
            onFdv.anyRank -> "FDV fleet · any rank"
            onFdv.canFly -> "FDV fleet"
            rankWaived -> "FDV fleet · rank waived"
            else -> {
                // Information, not a rule. A pass would still let the flight auto-approve, but it gets used up.
                val rank = onFdv.minRank?.let { "rank: $it" } ?: "above your FDV rank"
                when {
                    rankWaivers == 1 -> "$rank · would use your waiver pass"
                    rankWaivers > 1 -> "$rank · would use 1 of $rankWaivers waiver passes"
                    else -> rank
                }
            }
        }
        return "$name · $note"
    }
}