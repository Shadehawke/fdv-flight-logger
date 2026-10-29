package com.fdv.fdvflightlogger.data.history

import com.fdv.fdvflightlogger.data.db.FlightLogEntity

/**
 * What the pilot has entered before, indexed for suggestions.
 * Built in memory from the logbook: no extra tables, and it works at any airport worldwide.
 */
class LogbookHistory private constructor(
    private val sids: Map<String, List<Use>>,
    private val stars: Map<String, List<Use>>,
    private val depGates: Map<String, List<Use>>,
    private val arrGates: Map<String, List<Use>>,
    private val routes: Map<String, List<Use>>
) {
    /** One remembered value: the runways it was used with, how often, and when last. */
    private data class Use(
        val value: String,
        val runways: Set<String>,
        val count: Int,
        val lastUsed: Long
    )

    fun sids(airport: String, runway: String?): List<String> = rank(sids[key(airport)], runway)
    fun stars(airport: String, runway: String?): List<String> = rank(stars[key(airport)], runway)
    fun departureGates(airport: String): List<String> = rank(depGates[key(airport)], null)
    fun arrivalGates(airport: String): List<String> = rank(arrGates[key(airport)], null)
    fun routes(dep: String, arr: String): List<String> = rank(routes[pairKey(dep, arr)], null)

    // Matching runway first, then most used, then most recent
    private fun rank(uses: List<Use>?, runway: String?): List<String> {
        if (uses == null) return emptyList()
        val rwy = normalized(runway)
        return uses
            .sortedWith(
                compareByDescending<Use> { rwy != null && rwy in it.runways }
                    .thenByDescending { it.count }
                    .thenByDescending { it.lastUsed }
            )
            .map { it.value }
    }

    companion object {
        val EMPTY = LogbookHistory(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())

        fun from(flights: List<FlightLogEntity>): LogbookHistory = LogbookHistory(
            sids = index(flights, groupKey = { key(it.dep) }, value = { it.sid }, runway = { it.depRwy }),
            stars = index(flights, groupKey = { key(it.arr) }, value = { it.star }, runway = { it.arrRwy }),
            depGates = index(flights, groupKey = { key(it.dep) }, value = { it.depGate }),
            arrGates = index(flights, groupKey = { key(it.arr) }, value = { it.arrGate }),
            routes = index(flights, groupKey = { pairKey(it.dep, it.arr) }, value = { it.route })
        )

        /**
         * Groups flights by airport (or city pair), then by the value entered,
         * and records how each value was used.
         */
        private fun index(
            flights: List<FlightLogEntity>,
            groupKey: (FlightLogEntity) -> String,
            value: (FlightLogEntity) -> String?,
            runway: (FlightLogEntity) -> String? = { null }
        ): Map<String, List<Use>> =
            flights
                .filter { normalized(value(it)) != null }
                .groupBy(groupKey)
                .mapValues { (_, atAirport) ->
                    // !! is safe: the filter above dropped every flight without a value
                    atAirport.groupBy { normalized(value(it))!! }
                        .map { (entered, uses) ->
                            Use(
                                value = entered,
                                runways = uses.mapNotNull { normalized(runway(it)) }.toSet(),
                                count = uses.size,
                                lastUsed = uses.maxOf { it.createdAtEpochMs }
                            )
                        }
                }
    }
}

private fun key(code: String): String = code.trim().uppercase()

private fun pairKey(dep: String, arr: String): String = "${key(dep)}|${key(arr)}"

private fun normalized(text: String?): String? = text?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }