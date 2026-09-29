package com.fdv.fdvflightlogger.data.aircraft

/**
 * Groups aircraft that share flap settings.
 * Step 5 attaches takeoff/landing flap detents to each family.
 */
enum class AircraftFamily {
    A220, A320, A330, A350, A380,
    B717, B737, B747, B757, B767, B777, B787,
    EJET, EJET_E2, CRJ, DASH8, ATR72,
    ARJ21, C919
}