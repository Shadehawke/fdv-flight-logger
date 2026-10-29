package com.fdv.fdvflightlogger.data.simbrief

import com.fdv.fdvflightlogger.data.prefs.WeightUnit
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.roundToInt

object SimBriefParser {

    @Throws(SimBriefException::class)
    fun parse(json: String): SimBriefPlan {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw SimBriefException("SimBrief sent a response the app couldn't read.")
        }

        val origin = root.optJSONObject("origin")
            ?: throw SimBriefException("Your latest SimBrief plan has no departure airport.")
        val destination = root.optJSONObject("destination")
            ?: throw SimBriefException("Your latest SimBrief plan has no arrival airport.")
        val dep = origin.str("icao_code")
            ?: throw SimBriefException("Your latest SimBrief plan has no departure airport.")
        val arr = destination.str("icao_code")
            ?: throw SimBriefException("Your latest SimBrief plan has no arrival airport.")

        val general = root.optJSONObject("general")
        val fuel = root.optJSONObject("fuel")
        val weights = root.optJSONObject("weights")
        val times = root.optJSONObject("times")
        val aircraft = root.optJSONObject("aircraft")

        val depRwy = origin.str("plan_rwy")
        val takeoff = root.optJSONObject("tlr")?.optJSONObject("takeoff")
        val landing = root.optJSONObject("tlr")?.optJSONObject("landing")
        // Takeoff performance is listed for every runway; we want the one actually planned
        val takeoffRunway = takeoff?.objects("runway")?.firstOrNull { it.str("identifier") == depRwy }

        val units = root.optJSONObject("params")?.str("units")

        return SimBriefPlan(
            dep = dep,
            arr = arr,
            depRwy = depRwy,
            arrRwy = destination.str("plan_rwy"),
            altn = root.objects("alternate").firstOrNull()?.str("icao_code"),
            sid = general?.str("sid_ident"),
            star = general?.str("star_ident"),
            route = general?.str("route"),
            cruiseFl = general?.str("initial_altitude")?.toIntOrNull()?.let { (it / 100).toString() },
            costIndex = general?.str("costindex"),
            airline = general?.str("icao_airline"),
            flightNumber = general?.str("flight_number"),
            aircraft = aircraft?.str("icaocode") ?: aircraft?.str("icao_code"),

            weightUnit = if (units.equals("kgs", ignoreCase = true)) WeightUnit.KG else WeightUnit.LB,
            blockFuel = fuel?.num("plan_ramp"),
            reserveFuel = fuel?.num("reserve"),
            payload = weights?.num("payload"),
            zfw = weights?.num("est_zfw"),
            pax = weights?.str("pax_count"),

            blockTime = times?.str("est_block")?.toHhmm(),
            airTime = times?.str("est_time_enroute")?.toHhmm(),

            windDir = general?.str("avg_wind_dir")?.toIntOrNull(),
            windSpd = general?.str("avg_wind_spd")?.toIntOrNull(),
            cruiseOatC = averageCruiseOat(root),

            takeoffFlaps = takeoffRunway?.str("flap_setting"),
            v2 = takeoffRunway?.str("speeds_v2"),
            landingFlaps = landing?.optJSONObject("conditions")?.str("flap_setting"),
            vref = landing?.optJSONObject("distance_dry")?.str("speeds_vref"),
            depAltimeter = takeoff?.optJSONObject("conditions")?.str("altimeter"),
            arrAltimeter = landing?.optJSONObject("conditions")?.str("altimeter")
        )
    }

    // SimBrief has no single cruise OAT, so average the temperature at every cruise-stage waypoint
    private fun averageCruiseOat(root: JSONObject): Int? {
        // v2 sends navlog as a list; the older format wrapped it as navlog.fix
        val fixes = when (val navlog = root.opt("navlog")) {
            is JSONArray -> navlog.objectList()
            is JSONObject -> navlog.objects("fix")
            else -> emptyList()
        }
        val temps = fixes
            .filter { it.str("stage") == "CRZ" }
            .mapNotNull { it.str("oat")?.toIntOrNull() }

        return if (temps.isEmpty()) null else temps.average().roundToInt()
    }

    // "03:34:27" → "0334", matching how TimeField stores digits
    private fun String.toHhmm(): String? {
        val parts = split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return hours.toString().padStart(2, '0') + minutes.toString().padStart(2, '0')
    }

    // SimBrief sends values as strings, and may send {} or "" for "nothing"; treat both as absent
    private fun JSONObject.str(key: String): String? = when (val v = opt(key)) {
        is String -> v.trim().takeIf { it.isNotEmpty() }
        is Number -> v.toString()
        else -> null
    }

    private fun JSONObject.num(key: String): Double? = str(key)?.toDoubleOrNull()

    // A key that may hold one object or a list of them (SimBrief does both, e.g. alternates)
    private fun JSONObject.objects(key: String): List<JSONObject> = when (val v = opt(key)) {
        is JSONArray -> v.objectList()
        is JSONObject -> listOf(v)
        else -> emptyList()
    }

    private fun JSONArray.objectList(): List<JSONObject> =
        (0 until length()).mapNotNull { optJSONObject(it) }
}