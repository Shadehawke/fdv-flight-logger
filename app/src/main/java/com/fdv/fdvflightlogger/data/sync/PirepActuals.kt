package com.fdv.fdvflightlogger.data.sync

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.data.db.FlightType
import com.fdv.fdvflightlogger.data.fdv.FdvPirep
import com.fdv.fdvflightlogger.data.prefs.WeightUnit
import kotlin.math.roundToInt

private const val LB_PER_KG = 2.20462

/**
 * Copies what was actually flown onto a logbook flight. Actuals replace planned values;
 * descriptive fields (route, airline, aircraft) are only filled in where the pilot left them blank.
 */
internal fun FlightLogEntity.withActuals(pirep: FdvPirep, fuelUnit: WeightUnit): FlightLogEntity = copy(
    blockTime = pirep.blockMinutes?.let(::hhmm) ?: blockTime,
    airTime = pirep.airMinutes?.let(::hhmm) ?: airTime,
    fuelUsed = pirep.fuelUsedLbs?.let { lbs -> formatFuel(lbs, fuelUnit) } ?: fuelUsed,
    pax = pirep.pax?.toString() ?: pax,
    landingRateFpm = pirep.landingRateFpm ?: landingRateFpm,
    // Online is certain when FDV says so; offline can't tell "with ATC" from "without", so keep the pilot's choice
    flightType = if (pirep.online) FlightType.ONLINE.name else flightType,

    route = route?.takeIf { it.isNotBlank() } ?: pirep.route,
    airline = airline?.takeIf { it.isNotBlank() } ?: pirep.airline,
    flightNumber = flightNumber?.takeIf { it.isNotBlank() } ?: pirep.flightNumber,
    aircraft = aircraft?.takeIf { it.isNotBlank() } ?: pirep.aircraft,

    fdvPirepId = pirep.id,
    fdvScheduleId = fdvScheduleId ?: pirep.scheduleId,
    fdvCharterId = fdvCharterId ?: pirep.charterId
)

// 209 → "0329", the digits TimeField stores
internal fun hhmm(minutes: Int): String = "%02d%02d".format(minutes / 60, minutes % 60)

// FDV always sends pounds; store digits in the pilot's fuel unit, like every other fuel field
private fun formatFuel(lbs: Double, unit: WeightUnit): String = when (unit) {
    WeightUnit.LB -> lbs.roundToInt().toString()
    WeightUnit.KG -> (lbs / LB_PER_KG).roundToInt().toString()
}