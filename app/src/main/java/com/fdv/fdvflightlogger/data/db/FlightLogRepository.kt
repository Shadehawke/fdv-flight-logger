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
            flightType = draft.flightType.name,
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
            airline = draft.airline,
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

    suspend fun getLatestFlight(): FlightLogEntity? {
        return dao.getLatestFlight()
    }

    data class RestoreResult(val added: Int, val skipped: Int)

    /**
     * Merges backed-up flights into the logbook. A flight already present (same creation
     * time and route) is skipped, so restoring the same backup twice adds nothing.
     */
    suspend fun restore(flights: List<FlightLogEntity>): RestoreResult {
        val existing = dao.getAll().mapTo(HashSet()) { it.dedupeKey() }
        val seenInBackup = HashSet<String>()

        val toInsert = flights.filter { f ->
            val key = f.dedupeKey()
            key !in existing && seenInBackup.add(key)
        }

        if (toInsert.isNotEmpty()) dao.insertAll(toInsert)
        return RestoreResult(added = toInsert.size, skipped = flights.size - toInsert.size)
    }

    // createdAtEpochMs survives edits, so it identifies a flight across installs
    private fun FlightLogEntity.dedupeKey(): String = "$createdAtEpochMs|$dep|$arr"
}
