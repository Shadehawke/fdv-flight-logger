package com.fdv.fdvflightlogger.data.db

enum class FlightType {
    ONLINE,
    OFFLINE_WITH_ATC,
    OFFLINE_NO_ATC;

    fun displayName(): String = when (this) {
        ONLINE -> "Online"
        OFFLINE_WITH_ATC -> "Offline (w/ ATC)"
        OFFLINE_NO_ATC -> "Offline (No ATC)"
    }
}