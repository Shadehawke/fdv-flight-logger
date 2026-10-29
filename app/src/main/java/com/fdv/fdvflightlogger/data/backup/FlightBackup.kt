package com.fdv.fdvflightlogger.data.backup

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.data.db.FlightType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Full-fidelity logbook backup as JSON.
 *
 * Unlike the CSV export, this is meant to be read back in: every column is kept,
 * and the format is versioned so older backups stay restorable after schema changes.
 */
object FlightBackup {

    private const val FORMAT_ID = "fdv-flight-logger-backup"
    private const val FORMAT_VERSION = 1

    class InvalidBackupException(message: String) : Exception(message)

    fun toJson(flights: List<FlightLogEntity>, appVersion: String): String {
        val array = JSONArray()
        flights.forEach { array.put(it.toJsonObject()) }

        return JSONObject()
            .put("format", FORMAT_ID)
            .put("formatVersion", FORMAT_VERSION)
            .put("appVersion", appVersion)
            .put("exportedAtEpochMs", System.currentTimeMillis())
            .put("flightCount", flights.size)
            .put("flights", array)
            .toString(2)
    }

    /**
     * Parses a backup. Returned entities have id = 0 so Room assigns fresh IDs on insert;
     * the original IDs from another install would collide with existing rows.
     */
    @Throws(InvalidBackupException::class)
    fun fromJson(json: String): List<FlightLogEntity> {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw InvalidBackupException("This file isn't a valid FDV backup.")
        }

        if (root.optString("format") != FORMAT_ID) {
            throw InvalidBackupException("This file isn't an FDV Flight Logger backup.")
        }
        val version = root.optInt("formatVersion", -1)
        if (version < 1 || version > FORMAT_VERSION) {
            throw InvalidBackupException(
                "This backup was made by a newer version of the app. Update the app and try again."
            )
        }

        val array = root.optJSONArray("flights")
            ?: throw InvalidBackupException("The backup contains no flights.")

        return (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.toEntityOrNull()
        }
    }

    private fun FlightLogEntity.toJsonObject(): JSONObject = JSONObject().apply {
        put("createdAtEpochMs", createdAtEpochMs)
        put("dep", dep)
        put("arr", arr)
        put("flightType", flightType)

        putOpt("depRwy", depRwy)
        putOpt("depGate", depGate)
        putOpt("sid", sid)
        putOpt("cruiseFl", cruiseFl)
        putOpt("depFlaps", depFlaps)
        putOpt("v2", v2)
        putOpt("route", route)
        putOpt("depQnh", depQnh)

        putOpt("arrRwy", arrRwy)
        putOpt("arrGate", arrGate)
        putOpt("star", star)
        putOpt("altn", altn)
        putOpt("qnh", qnh)
        putOpt("vref", vref)
        putOpt("arrFlaps", arrFlaps)

        putOpt("flightNumber", flightNumber)
        putOpt("airline", airline)
        putOpt("aircraft", aircraft)
        putOpt("fuel", fuel)
        putOpt("pax", pax)
        putOpt("payload", payload)
        putOpt("airTime", airTime)
        putOpt("blockTime", blockTime)
        putOpt("costIndex", costIndex)
        putOpt("reserveFuel", reserveFuel)
        putOpt("zfw", zfw)
        putOpt("crzWind", crzWind)
        putOpt("crzOat", crzOat)

        putOpt("info", info)
        putOpt("initAlt", initAlt)
        putOpt("squawk", squawk)

        putOpt("scratchpad", scratchpad)
    }

    private fun JSONObject.toEntityOrNull(): FlightLogEntity? {
        // DEP and ARR are the only required columns; a row without them can't be shown anywhere
        val dep = str("dep") ?: return null
        val arr = str("arr") ?: return null

        val flightType = str("flightType")
            ?.takeIf { name -> FlightType.entries.any { it.name == name } }
            ?: FlightType.ONLINE.name

        return FlightLogEntity(
            id = 0,
            createdAtEpochMs = optLong("createdAtEpochMs", System.currentTimeMillis()),
            dep = dep,
            arr = arr,
            flightType = flightType,

            depRwy = str("depRwy"),
            depGate = str("depGate"),
            sid = str("sid"),
            cruiseFl = str("cruiseFl"),
            depFlaps = str("depFlaps"),
            v2 = str("v2"),
            route = str("route"),
            depQnh = str("depQnh"),

            arrRwy = str("arrRwy"),
            arrGate = str("arrGate"),
            star = str("star"),
            altn = str("altn"),
            qnh = str("qnh"),
            vref = str("vref"),
            arrFlaps = str("arrFlaps"),

            flightNumber = str("flightNumber"),
            airline = str("airline"),
            aircraft = str("aircraft"),
            fuel = str("fuel"),
            pax = str("pax"),
            payload = str("payload"),
            airTime = str("airTime"),
            blockTime = str("blockTime"),
            costIndex = str("costIndex"),
            reserveFuel = str("reserveFuel"),
            zfw = str("zfw"),
            crzWind = str("crzWind"),
            crzOat = str("crzOat"),

            info = str("info"),
            initAlt = str("initAlt"),
            squawk = str("squawk"),

            scratchpad = str("scratchpad")
        )
    }

    // optString returns "" for missing keys and "null" for JSON nulls; normalize both to null
    private fun JSONObject.str(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}