package com.fdv.fdvflightlogger.export

import com.fdv.fdvflightlogger.data.db.FlightLogEntity

object ExportCsv {

    // Keep columns stable for Excel import
    private val headers = listOf(
        "CreatedAtEpochMs",
        "FlightNumber",
        "Aircraft",
        "DEP","ARR",
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
        "Scratchpad"
    )

    fun buildCsv(rows: List<FlightLogEntity>): String {
        val sb = StringBuilder()
        sb.append(headers.joinToString(",")).append("\n")

        for (f in rows) {
            val values = listOf(
                f.createdAtEpochMs.toString(),
                f.flightNumber,
                f.aircraft,
                f.dep, f.arr,
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
                f.scratchpad
            ).map(::csvEscape)

            sb.append(values.joinToString(",")).append("\n")
        }

        return sb.toString()
    }

    private fun csvEscape(value: String?): String {
        val v = (value ?: "")
        val needsQuotes = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")
        if (!needsQuotes) return v
        return "\"" + v.replace("\"", "\"\"") + "\""
    }
}
