package com.fdv.fdvflightlogger.data.db

import android.content.Context
import com.fdv.fdvflightlogger.ui.screens.FlightDraft
import kotlinx.coroutines.flow.Flow

class FlightLogRepository(context: Context) {

    private val dao = FdvDatabase.get(context).flightLogDao()

    fun observeAll(): Flow<List<FlightLogEntity>> = dao.observeAll()

    suspend fun saveDraft(d: FlightDraft): Long {
        val entity = FlightLogEntity(
            dep = d.dep, arr = d.arr,
            depRwy = d.depRwy, depGate = d.depGate, sid = d.sid,
            cruiseFl = d.cruiseFl, depFlaps = d.depFlaps, v2 = d.v2, route = d.route,
            arrRwy = d.arrRwy, arrGate = d.arrGate, star = d.star,
            altn = d.altn, qnh = d.qnh, vref = d.vref,
            flightNumber = d.flightNumber, aircraft = d.aircraft, fuel = d.fuel,
            pax = d.pax, payload = d.payload, airTime = d.airTime, blockTime = d.blockTime,
            costIndex = d.costIndex, reserveFuel = d.reserveFuel, zfw = d.zfw,
            crzWind = d.crzWind, crzOat = d.crzOat,
            info = d.info, initAlt = d.initAlt, squawk = d.squawk,
            scratchpad = d.scratchpad
        )
        return dao.insert(entity)
    }

    suspend fun getAll(): List<FlightLogEntity> = dao.getAll()

    suspend fun getById(id: Long): FlightLogEntity? = dao.getById(id)

    suspend fun delete(flight: FlightLogEntity) = dao.deleteFlight(flight)
}
