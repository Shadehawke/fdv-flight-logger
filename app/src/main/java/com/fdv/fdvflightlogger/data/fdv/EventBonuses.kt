package com.fdv.fdvflightlogger.data.fdv

/** An FDV event a flight qualifies for: CAT Free (rank limits waived) and double hours. */
data class EventBonus(val event: FdvEvent, val live: Boolean)

/**
 * FDV's rule: a flight matching a Group Flight, Fly In, Fly Out or In or Out event, flown inside its
 * window, is CAT Free with double hours. "Other" events don't qualify on their own.
 */
object EventBonuses {

    // Category names compared as lowercase letters only, so "Fly In", "Fly-In" and "fly in" all match
    private const val GROUP_FLIGHT = "groupflight"
    private const val FLY_IN = "flyin"
    private const val FLY_OUT = "flyout"
    private const val IN_OR_OUT = "inorout"
    private val QUALIFYING = setOf(GROUP_FLIGHT, FLY_IN, FLY_OUT, IN_OR_OUT)

    // Pilots often log the flight before pushing back: an event starting soon still counts
    private const val LOOKAHEAD_MS = 2L * 60 * 60 * 1000

    fun qualifies(event: FdvEvent): Boolean = key(event.category) in QUALIFYING

    /** Whether a flight from [dep] to [arr] matches [event]'s airports for its category. */
    fun matches(event: FdvEvent, dep: String?, arr: String?): Boolean {
        val from = dep.code()
        val to = arr.code()
        val eventDep = event.departureIcao
        val eventArr = event.arrivalIcao
        return when (key(event.category)) {
            GROUP_FLIGHT -> (eventDep != null || eventArr != null) &&
                    (eventDep == null || eventDep == from) &&
                    (eventArr == null || eventArr == to)
            FLY_IN -> eventArr != null && eventArr == to
            FLY_OUT -> eventDep != null && eventDep == from
            IN_OR_OUT -> listOfNotNull(eventDep, eventArr).any { it == from || it == to }
            else -> false
        }
    }

    /** The qualifying event this flight matches that's live now or starts within 2 hours; live ones first. */
    fun find(events: List<FdvEvent>, dep: String?, arr: String?, now: Long): EventBonus? =
        events
            .filter { qualifies(it) && !it.hasEndedAt(now) && it.startsAtMillis - now <= LOOKAHEAD_MS }
            .filter { matches(it, dep, arr) }
            .minByOrNull { it.startsAtMillis }       // a live event started earlier than any upcoming one
            ?.let { EventBonus(event = it, live = it.isLiveAt(now)) }

    private fun key(category: String?): String = category.orEmpty().lowercase().filter { it.isLetter() }

    private fun String?.code(): String? = this?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
}