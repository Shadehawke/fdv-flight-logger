package com.fdv.fdvflightlogger.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FlightLogEntity::class],
    version = 3,
    exportSchema = true
)
abstract class FdvDatabase : RoomDatabase() {
    abstract fun flightLogDao(): FlightLogDao

    companion object {
        @Volatile
        private var INSTANCE: FdvDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns
                db.execSQL("ALTER TABLE flight_logs ADD COLUMN depQnh TEXT")
                db.execSQL("ALTER TABLE flight_logs ADD COLUMN arrFlaps TEXT")
            }
        }

        fun getDatabase(context: Context): FdvDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FdvDatabase::class.java,
                    "fdv_flight_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}