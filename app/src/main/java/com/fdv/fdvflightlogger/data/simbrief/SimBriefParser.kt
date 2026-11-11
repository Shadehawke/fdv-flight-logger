package com.fdv.fdvflightlogger.data.simbrief

import com.fdv.fdvflightlogger.data.network.AppHttp
import com.fdv.fdvflightlogger.data.prefs.WeightUnit
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.roundToInt

object SimBriefParser {

    @Throws(SimBriefException::class)
    fun parse(json: String): SimBriefPlan {
        // SimBrief's shapes are too loose for @Serializable classes, so walk the JSON tree by hand
        val root = try {
            AppHttp.json.parseToJsonElement(json) as? JsonObject
        } catch (e: SerializationException) {
            null
        } ?: throw SimBriefException("SimBrief sent a response the app couldn't read.")

        val origin = root.obj("origin")
            ?: throw SimBriefException("Your latest SimBrief plan has no departure airport.")
        val destination = root.obj("destination")
            ?: throw SimBriefException("Your latest SimBrief plan has no arrival airport.")
        val dep = origin.str("icao_code")
            ?: throw SimBriefException("Your latest SimBrief plan has no departure airport.")
        val arr = destination.str("icao_code")
            ?: throw SimBriefException("Your latest SimBrief plan has no arrival airport.")

        val general = root.obj("general")
        val fuel = root.obj("fuel")
        val weights = root.obj("weights")
        val times = root.obj("times")
        val aircraft = root.obj("aircraft")

        val depRwy = origin.str("plan_rwy")
        val takeoff = root.obj("tlr")?.obj("takeoff")
        val landing = root.obj("tlr")?.obj("landing")
        // Takeoff performance is listed for every runway; we want the one actually planned
        val takeoffRunway = takeoff?.objects("runway")?.firstOrNull { it.str("identifier") == depRwy }

        val units = root.obj("params")?.str("units")

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
            landingFlaps = landing?.obj("conditions")?.str("flap_setting"),
            vref = landing?.obj("distance_dry")?.str("speeds_vref"),
            depAltimeter = takeoff?.obj("conditions")?.str("altimeter"),
            arrAltimeter = landing?.obj("conditions")?.str("altimeter")
        )
    }

    // SimBrief has no single cruise OAT, so average the temperature at every cruise-stage waypoint
    private fun averageCruiseOat(root: JsonObject): Int? {
        // v2 sends navlog as a list; the older format wrapped it as navlog.fix
        val fixes = when (val navlog = root["navlog"]) {
            is JsonArray -> navlog.filterIsInstance<JsonObject>()
            is JsonObject -> navlog.objects("fix")
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

    // SimBrief sends values as strings, and may send {} or "" for "nothing"; treat both as absent.
    // JsonNull is itself a JsonPrimitive (its content is the text "null"), so it's excluded explicitly.
    private fun JsonObject.str(key: String): String? {
        val value = this[key] as? JsonPrimitive ?: return null
        if (value is JsonNull) return null
        return value.content.trim().takeIf { it.isNotEmpty() }
    }

    private fun JsonObject.num(key: String): Double? = str(key)?.toDoubleOrNull()

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    // A key that may hold one object or a list of them (SimBrief does both, e.g. alternates)
    private fun JsonObject.objects(key: String): List<JsonObject> = when (val value = this[key]) {
        is JsonArray -> value.filterIsInstance<JsonObject>()
        is JsonObject -> listOf(value)
        else -> emptyList()
    }
}