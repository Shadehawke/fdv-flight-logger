package com.fdv.fdvflightlogger.data.db

import android.content.Context
import com.fdv.fdvflightlogger.ui.screens.FlightDraft
import kotlinx.coroutines.flow.Flow

class FlightLogRepository(context: Context) {

    private val dao = FdvDatabase.getDatabase(context).flightLogDao()

    fun observeAll(): Flow<List<FlightLogEntity>> = dao.observeAll()

    suspend fun saveDraft(draft: FlightDraft, createdAtEpochMs: Long) {
        val entity = FlightLogEntity(
            id = draft.id ?: 0L,
            createdAtEpochMs = createdAtEpochMs,
            dep = draft.dep,
            arr = draft.arr,
            depRwy = draft.depRwy,
            depGate = draft.depGate,
            sid = draft.sid,
            cruiseFl = draft.cruiseFl,
            depFlaps = draft.depFlaps,
            v2 = draft.v2,
            route = draft.route,
            depQnh = draft.depQnh,

            arrRwy = draft.arrRwy,
            arrGate = draft.arrGate,
            star = draft.star,
            altn = draft.altn,
            qnh = draft.qnh,
            vref = draft.vref,
            arrFlaps = draft.arrFlaps,

            flightNumber = draft.flightNumber,
            aircraft = draft.aircraft,
            fuel = draft.fuel,
            pax = draft.pax,
            payload = draft.payload,
            airTime = draft.airTime,
            blockTime = draft.blockTime,
            costIndex = draft.costIndex,
            reserveFuel = draft.reserveFuel,
            zfw = draft.zfw,
            crzWind = draft.crzWind,
            crzOat = draft.crzOat,
            info = draft.info,
            initAlt = draft.initAlt,
            squawk = draft.squawk,
            scratchpad = draft.scratchpad
        )

        upsert(entity) // <- call the repo wrapper
    }


    suspend fun getAll(): List<FlightLogEntity> = dao.getAll()

    suspend fun getById(id: Long): FlightLogEntity? = dao.getById(id)

    suspend fun delete(flight: FlightLogEntity) = dao.deleteFlight(flight)

    suspend fun upsert(entity: FlightLogEntity): Long = dao.upsert(entity)
}
