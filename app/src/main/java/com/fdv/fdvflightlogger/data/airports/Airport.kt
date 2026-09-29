package com.fdv.fdvflightlogger.data.airports

data class Airport(
    val icao: String,
    val iata: String,
    val name: String,
    val city: String,
    val country: String,
    val runways: List<String>
) {
    // Precomputed once so search doesn't uppercase 5,800 strings on every keystroke.
    // Declared in the body, so they're excluded from equals/hashCode.
    internal val nameKey: String = name.uppercase()
    internal val cityKey: String = city.uppercase()

    /** Short label for dropdown rows, e.g. "KATL · Hartsfield-Jackson Atlanta International Airport" */
    val displayLabel: String get() = "$icao · $name"
}