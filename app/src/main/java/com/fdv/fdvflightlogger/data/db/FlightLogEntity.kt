package com.fdv.fdvflightlogger.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flight_logs")
data class FlightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtEpochMs: Long = System.currentTimeMillis(),

    // Required fields (non-nullable)
    val dep: String,
    val arr: String,

    // Optional departure fields (nullable with defaults)
    val depRwy: String? = null,
    val depGate: String? = null,
    val sid: String? = null,
    val cruiseFl: String? = null,
    val depFlaps: String? = null,
    val v2: String? = null,
    val route: String? = null,

    // Optional arrival fields
    val arrRwy: String? = null,
    val arrGate: String? = null,
    val star: String? = null,
    val altn: String? = null,
    val qnh: String? = null,
    val vref: String? = null,

    // Optional flight info
    val flightNumber: String? = null,
    val aircraft: String? = null,
    val fuel: String? = null,
    val pax: String? = null,
    val payload: String? = null,
    val airTime: String? = null,
    val blockTime: String? = null,
    val costIndex: String? = null,
    val reserveFuel: String? = null,
    val zfw: String? = null,
    val crzWind: String? = null,
    val crzOat: String? = null,

    // Optional ATC fields
    val info: String? = null,
    val initAlt: String? = null,
    val squawk: String? = null,

    // Optional notes
    val scratchpad: String? = null
)
