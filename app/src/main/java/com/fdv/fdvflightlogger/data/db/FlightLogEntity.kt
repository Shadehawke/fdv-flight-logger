package com.fdv.fdvflightlogger.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flight_logs")
data class FlightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val createdAtEpochMs: Long = System.currentTimeMillis(),

    val dep: String,
    val arr: String,

    val depRwy: String,
    val depGate: String,
    val sid: String,
    val cruiseFl: String,
    val depFlaps: String,
    val v2: String,
    val route: String,

    val arrRwy: String,
    val arrGate: String,
    val star: String,
    val altn: String,
    val qnh: String,
    val vref: String,

    val flightNumber: String,
    val aircraft: String,
    val fuel: String,
    val pax: String,
    val payload: String,
    val airTime: String,
    val blockTime: String,
    val costIndex: String,
    val reserveFuel: String,
    val zfw: String,
    val crzWind: String,
    val crzOat: String,

    val info: String,
    val initAlt: String,
    val squawk: String,

    val scratchpad: String
)
