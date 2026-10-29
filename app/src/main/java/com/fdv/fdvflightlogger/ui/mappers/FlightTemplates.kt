package com.fdv.fdvflightlogger.ui.mappers

import com.fdv.fdvflightlogger.ui.screens.FlightDraft

enum class FlightTemplate { RETURN_LEG, DUPLICATE }

/**
 * Builds a new, unsaved flight from an existing one.
 * Fields are listed explicitly rather than using copy(), so anything not named here
 * (including fields added in the future) starts blank instead of leaking over.
 */
fun FlightDraft.asTemplate(template: FlightTemplate): FlightDraft = when (template) {
    FlightTemplate.RETURN_LEG -> FlightDraft(
        dep = arr,
        arr = dep,
        flightType = flightType,
        airline = airline,
        aircraft = aircraft,
        costIndex = costIndex
    )

    FlightTemplate.DUPLICATE -> FlightDraft(
        dep = dep,
        arr = arr,
        flightType = flightType,
        depRwy = depRwy,
        sid = sid,
        cruiseFl = cruiseFl,
        route = route,
        arrRwy = arrRwy,
        star = star,
        altn = altn,
        airline = airline,
        flightNumber = flightNumber,
        aircraft = aircraft,
        costIndex = costIndex
    )
}