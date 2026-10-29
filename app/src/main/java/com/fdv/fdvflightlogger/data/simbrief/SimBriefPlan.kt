package com.fdv.fdvflightlogger.data.simbrief

import com.fdv.fdvflightlogger.data.prefs.WeightUnit

/**
 * The parts of a SimBrief OFP the logbook uses, exactly as SimBrief planned them.
 * Weights are in [weightUnit]; converting to the pilot's units happens when mapping to a draft.
 */
data class SimBriefPlan(
    val dep: String,
    val arr: String,
    val depRwy: String?,
    val arrRwy: String?,
    val altn: String?,
    val sid: String?,
    val star: String?,
    val route: String?,
    val cruiseFl: String?,
    val costIndex: String?,
    val airline: String?,
    val flightNumber: String?,
    val aircraft: String?,

    val weightUnit: WeightUnit,
    val blockFuel: Double?,
    val reserveFuel: Double?,
    val payload: Double?,
    val zfw: Double?,
    val pax: String?,

    val blockTime: String?,     // HHMM digits, the same format the logbook stores
    val airTime: String?,

    val windDir: Int?,
    val windSpd: Int?,
    val cruiseOatC: Int?,       // always Celsius from SimBrief

    val takeoffFlaps: String?,
    val v2: String?,
    val landingFlaps: String?,
    val vref: String?,
    val depAltimeter: String?,  // as SimBrief gives it: "29.87" (inHg) or "1013" (hPa)
    val arrAltimeter: String?
)