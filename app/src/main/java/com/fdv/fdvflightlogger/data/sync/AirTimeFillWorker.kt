package com.fdv.fdvflightlogger.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.fdv.fdvflightlogger.data.auth.FdvSessionStore
import com.fdv.fdvflightlogger.data.db.FlightLogRepository
import com.fdv.fdvflightlogger.data.fdv.FdvApi
import com.fdv.fdvflightlogger.data.fdv.FdvApiException
import com.fdv.fdvflightlogger.data.fdv.FdvPirep
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Fills in air time for PIREP-linked flights, one /pireps/{id} at a time, in the background.
 * Safe to stop at any point: each run starts from whatever is still missing.
 */
class AirTimeFillWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val session = FdvSessionStore(applicationContext).session.first()
            ?.takeUnless { it.isExpired(System.currentTimeMillis()) }
            ?: return Result.failure()   // signed out: nothing this worker can do

        val flights = FlightLogRepository(applicationContext)
        val pending = flights.flightsMissingAirTime()
        val startedAt = System.currentTimeMillis()
        var failuresInARow = 0

        pending.forEachIndexed { index, flight ->
            // Android stops ordinary background work after ~10 minutes; hand back early and get rescheduled
            if (System.currentTimeMillis() - startedAt > RUN_BUDGET_MS) return Result.retry()
            setProgress(workDataOf(KEY_DONE to index, KEY_TOTAL to pending.size))

            val pirepId = flight.fdvPirepId ?: return@forEachIndexed
            val pirep = try {
                fetchPolitely(session.token, pirepId).also { failuresInARow = 0 }
            } catch (e: FdvApiException) {
                if (e.signedOut) return Result.failure()
                // Several failures in a row means the connection dropped; try the whole run again later
                if (++failuresInARow >= MAX_FAILURES_IN_A_ROW) return Result.retry()
                null
            }

            pirep?.airMinutes?.let { minutes ->
                // Re-read just before writing, so an edit the pilot made meanwhile is never overwritten
                val current = flights.getById(flight.id)
                if (current != null && current.airTime.isNullOrBlank()) {
                    flights.upsert(current.copy(airTime = hhmm(minutes)))
                }
            }
            delay(SPACING_MS)
        }
        return Result.success()
    }

    // On HTTP 429, wait as long as FDV asks, then try the same PIREP once more
    private suspend fun fetchPolitely(token: String, id: Long): FdvPirep = try {
        FdvApi.pirep(token, id)
    } catch (e: FdvApiException) {
        val wait = e.retryAfterSeconds ?: throw e
        delay(wait * 1000L)
        FdvApi.pirep(token, id)
    }

    companion object {
        const val NAME = "fdv-air-time-fill"
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"

        private const val SPACING_MS = 1_500L                 // 40 requests a minute; FDV allows 60
        private const val RUN_BUDGET_MS = 8L * 60 * 1000
        private const val MAX_FAILURES_IN_A_ROW = 3

        /** Starts the fill, or does nothing if one is already queued or running. */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<AirTimeFillWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                // Used both for "run budget reached" and for dropped connections
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}