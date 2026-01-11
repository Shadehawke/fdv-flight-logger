package com.fdv.fdvflightlogger.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightLogDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: FlightLogEntity): Long

    @Query("SELECT * FROM flight_logs ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<FlightLogEntity>>

    @Query("SELECT * FROM flight_logs ORDER BY createdAtEpochMs DESC LIMIT 1")
    suspend fun latest(): FlightLogEntity?

    @Query("SELECT * FROM flight_logs ORDER BY createdAtEpochMs DESC")
    suspend fun getAll(): List<FlightLogEntity>

    @Query("SELECT * FROM flight_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FlightLogEntity?
}
