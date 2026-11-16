package com.fdv.fdvflightlogger.ui.nav

import com.fdv.fdvflightlogger.ui.mappers.FlightTemplate
import android.net.Uri

object Routes {
    const val SETUP = "setup"
    const val FLIGHT_LOG = "flight_log"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val SPLASH = "splash"
    const val LANDING = "landing"
    const val LOG_FROM = "log/from/{id}/{template}"
    const val TROPHIES = "trophies"
    const val EVENTS = "events"
    const val HELP = "help"
    const val HELP_SECTION = "help/{section}"
    const val HELP_TOPIC = "help/{section}/{topic}?q={q}"

    fun helpSection(section: String) = "help/$section"

    /** [query] carries the search words over, so the topic opens with them highlighted. */
    fun helpTopic(section: String, topic: String, query: String = "") =
        if (query.isBlank()) "help/$section/$topic" else "help/$section/$topic?q=${Uri.encode(query.trim())}"

    fun logFrom(id: Long, template: FlightTemplate) = "log/from/$id/${template.name}"

}
