package com.fdv.fdvflightlogger.ui.mappers

import com.fdv.fdvflightlogger.ui.screens.FlightDraft
import com.fdv.fdvflightlogger.data.db.FlightLogEntity

fun FlightLogEntity.toDraft(): FlightDraft = FlightDraft(
    id = id,
    dep = dep,
    arr = arr,
    depRwy = depRwy,
    depGate = depGate,
    sid = sid,
    cruiseFl = cruiseFl,
    depFlaps = depFlaps,
    v2 = v2,
    route = route,
    depQnh = depQnh,

    arrRwy = arrRwy,
    arrGate = arrGate,
    star = star,
    altn = altn,
    qnh = qnh,
    vref = vref,
    arrFlaps = arrFlaps,

    flightNumber = flightNumber,
    aircraft = aircraft,
    fuel = fuel,
    pax = pax,
    payload = payload,
    airTime = airTime,
    blockTime = blockTime,
    costIndex = costIndex,
    reserveFuel = reserveFuel,
    zfw = zfw,
    crzWind = crzWind,
    crzOat = crzOat,
    info = info,
    initAlt = initAlt,
    squawk = squawk,
    scratchpad = scratchpad
)
