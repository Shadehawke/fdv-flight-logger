package com.fdv.fdvflightlogger.data.fdv

import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.util.Locale

/** Turns PIREP DTOs into what the app uses, including air time from the Black Box log. */
internal object FdvPirepMapper {

    // Black Box always writes US-style log times, whatever the PC's locale: "10/22/2026 3:02 PM".
    // Single-letter M, d and h accept both "3" and "03", so either form parses.
    private val LOG_TIME: DateTimeFormatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("M/d/yyyy h:mm a")
        .toFormatter(Locale.US)

    // Anything outside this is a garbled log, not a real flight
    private const val MAX_FLIGHT_MINUTES = 24L * 60

    fun toPirep(dto: PirepDto): FdvPirep? {
        val id = dto.id ?: return null
        val dep = dto.departure?.icao.clean() ?: return null
        val arr = dto.arrival?.icao.clean() ?: return null
        val airline = dto.airline?.icao.clean()
        val callsign = dto.flight.clean()

        return FdvPirep(
            id = id,
            callsign = callsign.orEmpty(),
            airline = airline,
            flightNumber = flightNumberOf(callsign, airline),
            dep = dep,
            arr = arr,
            aircraft = dto.aircraft?.icao.clean(),
            status = dto.status.clean(),
            submittedAtMillis = dto.submittedAt.clean()?.let(::isoToMillis),
            blockMinutes = dto.durationMinutes?.takeIf { it > 0 },
            airMinutes = dto.log?.let(::airMinutes),
            fuelUsedLbs = dto.fuelUsedLbs?.takeIf { it > 0 },
            pax = dto.passengers,
            landingRateFpm = dto.landingRateFpm,
            online = (dto.onlinePercent ?: 0) > 0,
            route = dto.route.clean(),
            scheduleId = dto.scheduleId,
            charterId = dto.charterId
        )
    }

    /**
     * Takeoff to the first touchdown. Both times come from the same log, so the result is right
     * whether the pilot's Black Box logs in UTC or local time; no offset is needed.
     */
    fun airMinutes(log: List<PirepLogDto>): Int? {
        val takeoff = log.firstTimeOf("Taking Off") ?: return null
        // "Touched down" is the wheels-on moment; the first one, so a bounce doesn't count.
        // Not "Landing": "Landing lights on" starts with that too.
        val touchdown = log.firstTimeOf("Touched down")
            ?: log.firstTimeOf("Landed on runway")
            ?: return null

        val minutes = Duration.between(takeoff, touchdown).toMinutes()
        return minutes.takeIf { it in 1L..MAX_FLIGHT_MINUTES }?.toInt()
    }

    // IT confirmed Black Box's phase names are fixed, and some lines add detail after them
    private fun List<PirepLogDto>.firstTimeOf(prefix: String): LocalDateTime? =
        firstOrNull { it.text?.trimStart()?.startsWith(prefix, ignoreCase = true) == true }
            ?.time
            ?.let(::logTime)

    private fun logTime(raw: String): LocalDateTime? = try {
        LocalDateTime.parse(raw.trim(), LOG_TIME)
    } catch (e: DateTimeParseException) {
        null
    }

    // submitted_at is true ISO-8601 with an offset: "2026-10-22T20:06:41+00:00"
    private fun isoToMillis(raw: String): Long? = try {
        OffsetDateTime.parse(raw).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}

/** "DAL1060" with airline DAL → "1060". Shared by bids and PIREPs. */
internal fun flightNumberOf(callsign: String?, airline: String?): String? {
    val flight = callsign?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (airline.isNullOrBlank() || !flight.startsWith(airline, ignoreCase = true)) return flight
    return flight.drop(airline.length).trim().takeIf { it.isNotEmpty() }
}