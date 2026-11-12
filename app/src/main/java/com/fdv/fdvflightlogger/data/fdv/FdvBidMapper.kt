package com.fdv.fdvflightlogger.data.fdv

import com.fdv.fdvflightlogger.data.prefs.WeightUnit
import com.fdv.fdvflightlogger.data.simbrief.SimBriefPlan

/** Turns FDV booking DTOs into what the app uses. Pure, so it's unit tested with real responses. */
internal object FdvBidMapper {

    // SID/STAR names: 3–5 letters, one digit, optional letter (POUNC2, WLEEE7, GLAVN2, RNAV1A).
    // Three letters minimum so airways like UL9 or J121 aren't mistaken for procedures.
    private val PROCEDURE = Regex("^[A-Z]{3,5}\\d[A-Z]?$")

    fun toBid(dto: BidDto): FdvBid? {
        val id = dto.id ?: return null
        val dep = dto.departure?.icao.clean() ?: return null
        val arr = dto.arrival?.icao.clean() ?: return null
        return FdvBid(
            id = id,
            callsign = dto.flight.clean() ?: "",
            dep = dep,
            arr = arr,
            aircraft = dto.aircraft?.icao.clean(),
            departUtc = dto.departUtc.clean(),
            hasSimBrief = dto.simbriefOfpId.clean() != null
        )
    }

    fun toBidPlan(dto: BidDto): FdvBidPlan? {
        val dep = dto.departure?.icao.clean() ?: return null
        val arr = dto.arrival?.icao.clean() ?: return null
        val simBrief = dto.simbrief

        // "DAL1060" is stored as airline DAL + flight number 1060, like the SimBrief import
        val airline = dto.airline?.icao.clean()
        val callsign = dto.flight.clean()
        val flightNumber = flightNumberOf(callsign, airline)

        // The SimBrief plan is the real flight; the timetable is the fallback
        val route = simBrief?.route.clean() ?: dto.route.clean()
        val waypoints = route?.split(Regex("\\s+")).orEmpty()

        val plan = SimBriefPlan(
            dep = dep,
            arr = arr,
            depRwy = simBrief?.departureRunway.clean(),
            arrRwy = simBrief?.arrivalRunway.clean(),
            altn = simBrief?.alternate?.icao.clean(),
            sid = waypoints.firstOrNull()?.takeIf { PROCEDURE.matches(it) },
            star = waypoints.takeIf { it.size > 1 }?.lastOrNull()?.takeIf { PROCEDURE.matches(it) },
            route = route,
            cruiseFl = (simBrief?.initialFl ?: dto.cruiseFl)?.toString(),
            costIndex = simBrief?.costIndex.clean(),
            airline = airline,
            flightNumber = flightNumber,
            aircraft = dto.aircraft?.icao.clean(),

            weightUnit = if (simBrief?.units.equals("kgs", ignoreCase = true)) WeightUnit.KG else WeightUnit.LB,
            blockFuel = simBrief?.fuel?.block,
            reserveFuel = simBrief?.fuel?.reserve,
            payload = null,
            zfw = simBrief?.weights?.zeroFuel,
            pax = simBrief?.load?.passengers?.toString(),

            blockTime = dto.duration.clean()?.hhmmDigits(),
            airTime = simBrief?.enrouteMinutes?.let { "%02d%02d".format(it / 60, it % 60) },

            // Not in FDV's bid; a SimBrief import afterwards can still add these
            windDir = null,
            windSpd = null,
            cruiseOatC = null,
            takeoffFlaps = null,
            v2 = null,
            landingFlaps = null,
            vref = null,
            depAltimeter = null,
            arrAltimeter = null
        )

        return FdvBidPlan(
            plan = plan,
            depGate = dto.gates?.departure?.gate.clean(),
            arrGate = dto.gates?.arrival?.gate.clean(),
            scheduleId = dto.scheduleId,
            charterId = dto.charterId
        )
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    // "02:26" → "0226", the digits TimeField stores
    private fun String.hhmmDigits(): String? {
        val parts = split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return "%02d%02d".format(hours, minutes)
    }
}