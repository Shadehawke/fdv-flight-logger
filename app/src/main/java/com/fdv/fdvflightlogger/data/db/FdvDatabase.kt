package com.fdv.fdvflightlogger.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FlightLogEntity::class],
    version = 1,
    exportSchema = true
)
abstract class FdvDatabase : RoomDatabase() {
    abstract fun flightLogDao(): FlightLogDao

    companion object {
        @Volatile private var INSTANCE: FdvDatabase? = null

        fun get(context: Context): FdvDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FdvDatabase::class.java,
                    "fdv.db"
                ).build().also { INSTANCE = it }
            }
    }
}
