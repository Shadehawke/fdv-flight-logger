package com.fdv.fdvflightlogger.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FlightLogEntity): Long

    @Query("SELECT * FROM flight_logs ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<FlightLogEntity>>

    @Query("SELECT * FROM flight_logs ORDER BY createdAtEpochMs DESC LIMIT 1")
    suspend fun latest(): FlightLogEntity?

    @Query("SELECT * FROM flight_logs ORDER BY createdAtEpochMs DESC")
    suspend fun getAll(): List<FlightLogEntity>

    @Query("SELECT * FROM flight_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FlightLogEntity?

    @Query("SELECT * FROM flight_logs ORDER BY id DESC LIMIT 1")
    suspend fun getLatestFlight(): FlightLogEntity?

    @Delete
    suspend fun deleteFlight(flight: FlightLogEntity)
}
