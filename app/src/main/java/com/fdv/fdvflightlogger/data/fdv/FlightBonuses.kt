package com.fdv.fdvflightlogger.data.fdv

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** When a flight has to take off and land to count. A null bound is open. */
data class BonusWindow(
    val takeoffFrom: Long? = null,
    val takeoffBy: Long? = null,
    val landFrom: Long? = null,
    val landBy: Long? = null
)

/** Where a flight stands against a window, judged from when it takes off and, if known, lands. */
enum class WindowStatus { QUALIFIES, NOT_YET, MISSED }

/** An FDV event or bonus a flight matches, and what it gives. */
data class FlightBonus(
    val key: String,
    val title: String,           // the event's name, or the bonus's label
    val kind: String,            // "Group Flight", "Flight of the Day", …
    val categoryFree: Boolean,
    val doubleHours: Boolean,
    val window: BonusWindow,
    val status: WindowStatus
) {
    /** FDV skips the rank check on CAT Free and on double-hours flights alike. */
    val waivesRank: Boolean get() = categoryFree || doubleHours
}

/** FDV's rules for which flights earn an event's or a bonus's CAT Free and double hours. */
object FlightBonuses {

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE

    // Pilots often log before pushback: a window opening within this counts as "coming up"
    private const val LOOKAHEAD_MS = 2 * HOUR

    // Category names compared as lowercase letters only, so "Fly In", "Fly-In" and "fly in" all match
    private const val GROUP_FLIGHT = "groupflight"
    private const val FLY_IN = "flyin"
    private const val FLY_OUT = "flyout"
    private const val IN_OR_OUT = "inorout"
    private val BONUS_CATEGORIES = setOf(GROUP_FLIGHT, FLY_IN, FLY_OUT, IN_OR_OUT)

    private val ZULU: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmm'Z'").withZone(ZoneOffset.UTC)
    private val DAY_ZULU: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d HHmm'Z'").withZone(ZoneOffset.UTC)

    /** Fallback for an event without FDV's own flags: the four flying categories are CAT Free with double hours. */
    fun isBonusCategory(category: String?): Boolean = key(category) in BONUS_CATEGORIES

    /** "CAT Free · double hours", "CAT Free", "Double hours", or null when it gives neither. */
    fun perks(categoryFree: Boolean, doubleHours: Boolean): String? = when {
        categoryFree && doubleHours -> "CAT Free · double hours"
        categoryFree -> "CAT Free"
        doubleHours -> "Double hours"
        else -> null
    }

    fun typeLabel(type: String): String = when (type) {
        "flight_of_the_day" -> "Flight of the Day"
        "airport_of_the_month" -> "Airport of the Month"
        "event" -> "Event bonus"
        "personal" -> "Personal bonus"
        else -> "Bonus"
    }

    /** "KATL → KLAX", "Into or out of KBOS", "From KATL", "To KSEA", "Any flight". */
    fun where(bonus: FdvBonus): String? = when (bonus.match) {
        BonusMatch.ROUTE -> if (bonus.departureIcao != null && bonus.arrivalIcao != null) {
            "${bonus.departureIcao} → ${bonus.arrivalIcao}"
        } else null
        BonusMatch.EITHER_END -> (bonus.departureIcao ?: bonus.arrivalIcao)?.let { "Into or out of $it" }
        BonusMatch.DEPARTURE -> bonus.departureIcao?.let { "From $it" }
        BonusMatch.ARRIVAL -> bonus.arrivalIcao?.let { "To $it" }
        BonusMatch.ANY -> "Any flight"
    }

    // ---------------- Events ----------------

    /**
     * FDV goes by which airports the event has: both set means both must match; only a departure,
     * the departure; only an arrival, the arrival. In or Out: either end at the event airport.
     */
    fun eventMatches(event: FdvEvent, dep: String?, arr: String?): Boolean {
        val from = dep.code()
        val to = arr.code()
        val eventDep = event.departureIcao
        val eventArr = event.arrivalIcao
        if (key(event.category) == IN_OR_OUT) {
            val airport = eventDep ?: eventArr ?: return false
            return from == airport || to == airport
        }
        return when {
            eventDep != null && eventArr != null -> eventDep == from && eventArr == to
            eventDep != null -> eventDep == from
            eventArr != null -> eventArr == to
            else -> false
        }
    }

    /**
     * Leaving (Group Flight, Fly Out, In or Out from the event airport): take off from 30 minutes
     * before the start until the end, and land within 2 hours of the end.
     * Arriving (Fly In, In or Out to the event airport): land during the event, having taken off
     * no more than 4 hours before the start.
     */
    fun eventWindow(event: FdvEvent, dep: String?): BonusWindow {
        val start = event.startsAtMillis
        val end = event.endsAtMillis
        val leaving = when (key(event.category)) {
            FLY_IN -> false
            IN_OR_OUT -> dep.code() == (event.departureIcao ?: event.arrivalIcao)
            else -> true
        }
        return if (leaving) {
            BonusWindow(takeoffFrom = start - 30 * MINUTE, takeoffBy = end, landBy = end + 2 * HOUR)
        } else {
            BonusWindow(takeoffFrom = start - 4 * HOUR, landFrom = start, landBy = end)
        }
    }

    // ---------------- Bonuses ----------------

    fun bonusMatches(bonus: FdvBonus, dep: String?, arr: String?): Boolean {
        val from = dep.code()
        val to = arr.code()
        return when (bonus.match) {
            BonusMatch.ROUTE -> bonus.departureIcao != null && bonus.arrivalIcao != null &&
                    bonus.departureIcao == from && bonus.arrivalIcao == to
            // Airport of the Month lists the same airport at both ends: in or out of it, not a route
            BonusMatch.EITHER_END -> {
                val airport = bonus.departureIcao ?: bonus.arrivalIcao
                airport != null && (from == airport || to == airport)
            }
            BonusMatch.DEPARTURE -> bonus.departureIcao != null && bonus.departureIcao == from
            BonusMatch.ARRIVAL -> bonus.arrivalIcao != null && bonus.arrivalIcao == to
            BonusMatch.ANY -> true
        }
    }

    /** In the air during the window: take off before it ends, land after it starts. */
    fun bonusWindow(bonus: FdvBonus): BonusWindow =
        BonusWindow(takeoffBy = bonus.endsAtMillis, landFrom = bonus.startsAtMillis)

    // ---------------- Timing ----------------

    /**
     * [landing] null means the air time isn't known yet. A flight can't land before it takes off, so
     * the take-off time stands in for it: enough to rule a flight out, or to say it's too early.
     */
    fun status(window: BonusWindow, takeoff: Long, landing: Long?): WindowStatus {
        val land = landing ?: takeoff
        if (window.takeoffBy != null && takeoff > window.takeoffBy) return WindowStatus.MISSED
        if (window.landBy != null && land > window.landBy) return WindowStatus.MISSED
        if (window.takeoffFrom != null && takeoff < window.takeoffFrom) return WindowStatus.NOT_YET
        if (window.landFrom != null && land < window.landFrom) return WindowStatus.NOT_YET
        return WindowStatus.QUALIFIES
    }

    /**
     * Everything a flight from [dep] to [arr] would earn, judged as if it takes off at [takeoff] and,
     * when [airMinutes] is known, lands that long after. Missed ones, and ones whose window opens more
     * than 2 hours later, are left out. Qualifying ones first.
     */
    fun find(
        events: List<FdvEvent>,
        bonuses: List<FdvBonus>,
        dep: String?,
        arr: String?,
        takeoff: Long,
        airMinutes: Int?
    ): List<FlightBonus> {
        val landing = airMinutes?.let { takeoff + it * MINUTE }

        val matchedEvents = events.filter { (it.categoryFree || it.doubleHours) && eventMatches(it, dep, arr) }
        val fromEvents = matchedEvents.map { event ->
            val window = eventWindow(event, dep)
            FlightBonus(
                key = "event-${event.id}",
                title = event.name,
                kind = event.category ?: "Event",
                categoryFree = event.categoryFree,
                doubleHours = event.doubleHours,
                window = window,
                status = status(window, takeoff, landing)
            )
        }

        // An "event" bonus can repeat an event already listed; keep the event, which has the fuller rules
        val eventTimes = matchedEvents.map { it.startsAtMillis to it.endsAtMillis }.toSet()
        val fromBonuses = bonuses
            .filter { (it.categoryFree || it.doubleHours) && bonusMatches(it, dep, arr) }
            .filterNot { it.type == "event" && (it.startsAtMillis to it.endsAtMillis) in eventTimes }
            .map { bonus ->
                val window = bonusWindow(bonus)
                FlightBonus(
                    key = "bonus-${bonus.type}-${bonus.startsAtMillis}-${bonus.label}",
                    title = bonus.label,
                    kind = typeLabel(bonus.type),
                    categoryFree = bonus.categoryFree,
                    doubleHours = bonus.doubleHours,
                    window = window,
                    status = status(window, takeoff, landing)
                )
            }

        return (fromEvents + fromBonuses)
            .filter { it.status != WindowStatus.MISSED && opensSoon(it.window, takeoff) }
            .sortedBy { if (it.status == WindowStatus.QUALIFIES) 0 else 1 }
    }

    /** "Take off 1830Z–2100Z · land by 2300Z", in Zulu, with the date when it isn't [now]'s day. */
    fun describe(window: BonusWindow, now: Long): String {
        fun at(millis: Long): String {
            val sameDay = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate() ==
                    Instant.ofEpochMilli(now).atOffset(ZoneOffset.UTC).toLocalDate()
            return (if (sameDay) ZULU else DAY_ZULU).format(Instant.ofEpochMilli(millis))
        }
        fun range(verb: String, from: Long?, by: Long?): String? = when {
            from != null && by != null -> "$verb ${at(from)}–${at(by)}"
            from != null -> "$verb after ${at(from)}"
            by != null -> "$verb by ${at(by)}"
            else -> null
        }
        return listOfNotNull(
            range("Take off", window.takeoffFrom, window.takeoffBy),
            range("land", window.landFrom, window.landBy)
        ).joinToString(" · ").replaceFirstChar { it.uppercase() }
    }

    private fun opensSoon(window: BonusWindow, now: Long): Boolean {
        val opens = window.takeoffFrom ?: window.landFrom ?: return true
        return opens - now <= LOOKAHEAD_MS
    }

    private fun key(category: String?): String = category.orEmpty().lowercase().filter { it.isLetter() }

    private fun String?.code(): String? = this?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
}