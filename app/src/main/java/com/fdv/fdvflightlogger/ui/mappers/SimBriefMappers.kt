package com.fdv.fdvflightlogger.ui.mappers

import com.fdv.fdvflightlogger.data.aircraft.AircraftTypes
import com.fdv.fdvflightlogger.data.prefs.AppSettings
import com.fdv.fdvflightlogger.data.prefs.QnhUnit
import com.fdv.fdvflightlogger.data.prefs.TempUnit
import com.fdv.fdvflightlogger.data.prefs.WeightUnit
import com.fdv.fdvflightlogger.data.simbrief.SimBriefPlan
import com.fdv.fdvflightlogger.ui.screens.FlightDraft
import kotlin.math.roundToInt

private const val LB_PER_KG = 2.20462
private const val HPA_PER_INHG = 33.8639

/**
 * Copies a SimBrief plan onto the draft being edited. Planned values replace what's there;
 * anything SimBrief doesn't plan (gates, ATC, scratchpad, flight type, the draft's ID) is kept.
 */
fun SimBriefPlan.applyTo(draft: FlightDraft, settings: AppSettings): FlightDraft {
    // Flap normalization depends on the aircraft, so resolve it first
    val aircraftIcao = aircraft ?: draft.aircraft

    return draft.copy(
        dep = dep,
        arr = arr,

        depRwy = depRwy ?: draft.depRwy,
        sid = sid ?: draft.sid,
        cruiseFl = cruiseFl ?: draft.cruiseFl,
        depFlaps = takeoffFlaps?.let { normalizeTakeoffFlaps(it, aircraftIcao) } ?: draft.depFlaps,
        v2 = v2 ?: draft.v2,
        route = route ?: draft.route,
        depQnh = depAltimeter?.let { qnhDigits(it, settings.qnhUnit) } ?: draft.depQnh,

        arrRwy = arrRwy ?: draft.arrRwy,
        star = star ?: draft.star,
        altn = altn ?: draft.altn,
        qnh = arrAltimeter?.let { qnhDigits(it, settings.qnhUnit) } ?: draft.qnh,
        vref = vref ?: draft.vref,
        arrFlaps = landingFlaps?.trim()?.uppercase() ?: draft.arrFlaps,

        airline = airline ?: draft.airline,
        flightNumber = flightNumber ?: draft.flightNumber,
        aircraft = aircraftIcao,
        fuel = blockFuel?.let { formatWeight(it, weightUnit, settings.fuelUnit) } ?: draft.fuel,
        reserveFuel = reserveFuel?.let { formatWeight(it, weightUnit, settings.fuelUnit) } ?: draft.reserveFuel,
        payload = payload?.let { formatWeight(it, weightUnit, settings.weightUnit) } ?: draft.payload,
        zfw = zfw?.let { formatWeight(it, weightUnit, settings.weightUnit) } ?: draft.zfw,
        pax = pax ?: draft.pax,
        blockTime = blockTime ?: draft.blockTime,
        airTime = airTime ?: draft.airTime,
        costIndex = costIndex ?: draft.costIndex,
        crzWind = windDigits(windDir, windSpd) ?: draft.crzWind,
        crzOat = cruiseOatC?.let { formatOat(it, settings.tempUnit) } ?: draft.crzOat
    )
}

private fun formatWeight(value: Double, from: WeightUnit, to: WeightUnit): String {
    val converted = when {
        from == to -> value
        from == WeightUnit.KG -> value * LB_PER_KG
        else -> value / LB_PER_KG
    }
    return converted.roundToInt().toString()
}

// Stored as plain digits ("28324"); CrzWindVisualTransformation displays it as 283/24
private fun windDigits(dir: Int?, spd: Int?): String? {
    if (dir == null || spd == null) return null
    return dir.toString().padStart(3, '0') + spd.toString()
}

private fun formatOat(celsius: Int, unit: TempUnit): String = when (unit) {
    TempUnit.C -> celsius.toString()
    TempUnit.F -> (celsius * 9.0 / 5.0 + 32).roundToInt().toString()
}

// SimBrief gives altimeters as "29.87" (inHg) or "1013" (hPa); QnhField stores digits only
private fun qnhDigits(raw: String, unit: QnhUnit): String? {
    val value = raw.toDoubleOrNull() ?: return null
    val isHpa = value > 100 // inHg is ~28–31, hPa ~950–1050, so the ranges can't overlap

    return when (unit) {
        QnhUnit.INHG -> {
            val inHg = if (isHpa) value / HPA_PER_INHG else value
            (inHg * 100).roundToInt().toString()
        }
        QnhUnit.HPA -> {
            val hpa = if (isHpa) value else value * HPA_PER_INHG
            hpa.roundToInt().toString()
        }
    }
}

// Airbus takeoff CONF 1 is always 1+F, but SimBrief reports it as "1"; match the aircraft's flap list
private fun normalizeTakeoffFlaps(raw: String, aircraftIcao: String?): String {
    val value = raw.trim().uppercase()
    val options = AircraftTypes.takeoffFlapsFor(aircraftIcao)
    return if (value == "1" && "1+F" in options) "1+F" else value
}