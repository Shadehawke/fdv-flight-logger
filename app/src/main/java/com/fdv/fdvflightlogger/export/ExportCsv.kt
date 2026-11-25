package com.fdv.fdvflightlogger.export

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import java.time.Instant

object ExportCsv {

    // New columns are only ever added at the end, so anything reading by position keeps working
    private val headers = listOf(
        "CreatedAtEpochMs",
        "FlightNumber",
        "Airline",
        "Aircraft",
        "DEP","ARR",
        "FlightType",
        "DepRWY","DepGate","SID",
        "CruiseFL","DepFlaps","V2","Route",
        "DepQNH",
        "ArrRWY","ArrGate","STAR",
        "ALTN","QNH","Vref",
        "ArrFlaps",
        "Fuel","PAX","Payload",
        "AirTime","BlockTime","CostIndex",
        "ReserveFuel","ZFW",
        "CrzWind","CrzOAT",
        "ATISInfo","InitAlt","Squawk",
        "Scratchpad",
        // Added in 1.2: FDV sync, plus a readable date for spreadsheets
        "FuelUsed","LandingRateFpm",
        "FdvPirepId","ImportedFromFdv",
        "DateUtc",
        // Added in 1.3
        "FdvStatus"
    )

    fun buildCsv(rows: List<FlightLogEntity>): String {
        val sb = StringBuilder()
        sb.append(headers.joinToString(",")).append("\n")

        for (f in rows) {
            val values = listOf(
                f.createdAtEpochMs.toString(),
                f.flightNumber,
                f.airline,
                f.aircraft,
                f.dep, f.arr,
                f.flightType,
                f.depRwy, f.depGate, f.sid,
                f.cruiseFl, f.depFlaps, f.v2, f.route,
                f.depQnh,
                f.arrRwy, f.arrGate, f.star,
                f.altn, f.qnh, f.vref,
                f.arrFlaps,
                f.fuel, f.pax, f.payload,
                f.airTime, f.blockTime, f.costIndex,
                f.reserveFuel, f.zfw,
                f.crzWind, f.crzOat,
                f.info, f.initAlt, f.squawk,
                f.scratchpad,
                f.fuelUsed, f.landingRateFpm?.toString(),
                f.fdvPirepId?.toString(), f.importedFromFdv.toString(),
                // ISO-8601 in UTC ("2026-10-22T20:06:41Z"); spreadsheets recognise it as a date
                Instant.ofEpochMilli(f.createdAtEpochMs).toString(),
                f.fdvStatus
            ).map(::csvEscape)

            sb.append(values.joinToString(",")).append("\n")
        }

        return sb.toString()
    }

    // A cell starting with these runs as a formula in Excel and Sheets
    private val FORMULA_START = setOf('=', '+', '-', '@', '\t', '\r')

    internal fun csvEscape(value: String?): String {
        val raw = value ?: ""
        // Numbers such as a -285 landing rate stay numbers; anything else gets a leading ' so it shows as text
        val v = if (raw.isNotEmpty() && raw[0] in FORMULA_START && raw.toDoubleOrNull() == null) "'$raw" else raw
        val needsQuotes = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")
        if (!needsQuotes) return v
        return "\"" + v.replace("\"", "\"\"") + "\""
    }
}