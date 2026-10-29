package com.fdv.fdvflightlogger.ui.nav

import com.fdv.fdvflightlogger.ui.mappers.FlightTemplate

object Routes {
    const val SETUP = "setup"
    const val FLIGHT_LOG = "flight_log"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val SPLASH = "splash"
    const val LANDING = "landing"
    const val LOG_FROM = "log/from/{id}/{template}"

    fun logFrom(id: Long, template: FlightTemplate) = "log/from/$id/${template.name}"

}
