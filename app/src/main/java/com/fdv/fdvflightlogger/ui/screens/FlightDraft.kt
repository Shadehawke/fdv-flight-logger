package com.fdv.fdvflightlogger.ui.screens

data class FlightDraft(
    val dep: String = "",
    val arr: String = "",

    val depRwy: String = "",
    val depGate: String = "",
    val sid: String = "",
    val cruiseFl: String = "",
    val depFlaps: String = "",
    val v2: String = "",
    val route: String = "",

    val arrRwy: String = "",
    val arrGate: String = "",
    val star: String = "",
    val altn: String = "",
    val qnh: String = "",
    val vref: String = "",

    val flightNumber: String = "",
    val aircraft: String = "",
    val fuel: String = "",
    val pax: String = "",
    val payload: String = "",
    val airTime: String = "",
    val blockTime: String = "",
    val costIndex: String = "",
    val reserveFuel: String = "",
    val zfw: String = "",
    val crzWind: String = "",
    val crzOat: String = "",

    val info: String = "",
    val initAlt: String = "",
    val squawk: String = "",

    val scratchpad: String = ""
)
