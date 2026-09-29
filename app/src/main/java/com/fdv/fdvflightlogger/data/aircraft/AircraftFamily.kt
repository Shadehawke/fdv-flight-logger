package com.fdv.fdvflightlogger.data.aircraft

/**
 * Groups aircraft that share flap settings.
 * Values are the settings a pilot would select, as shown on the flap lever or ECAM.
 * An empty list means "unknown": the flap fields fall back to free text.
 */
enum class AircraftFamily(
    val takeoffFlaps: List<String>,
    val landingFlaps: List<String>
) {
    A220(listOf("1", "2", "3", "4"), listOf("4", "5")),
    A320(listOf("1+F", "2", "3"), listOf("3", "FULL")),
    A330(listOf("1+F", "2", "3"), listOf("3", "FULL")),
    A350(listOf("1+F", "2", "3"), listOf("3", "FULL")),
    A380(listOf("1+F", "2", "3"), listOf("3", "FULL")),

    B717(listOf("5", "13", "18", "24"), listOf("30", "40")),
    B737(listOf("1", "5", "10", "15", "25"), listOf("15", "30", "40")),
    B747(listOf("10", "20"), listOf("25", "30")),
    B757(listOf("1", "5", "15", "20"), listOf("25", "30")),
    B767(listOf("1", "5", "15", "20"), listOf("25", "30")),
    B777(listOf("5", "15", "20"), listOf("25", "30")),
    B787(listOf("5", "10", "15", "17", "18", "20"), listOf("25", "30")),

    EJET(listOf("1", "2", "3", "4"), listOf("5", "FULL")),
    EJET_E2(listOf("1", "2", "3", "4"), listOf("5", "FULL")),
    CRJ(listOf("8", "20"), listOf("30", "45")),
    DASH8(listOf("5", "10", "15"), listOf("10", "15", "35")),
    ATR72(listOf("15"), listOf("30")),

    ARJ21(emptyList(), emptyList()),
    C919(emptyList(), emptyList())
}