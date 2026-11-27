package com.fdv.fdvflightlogger.ui

import com.fdv.fdvflightlogger.data.sync.ImportPlan

/** Steps of the history import offered after signing in; null in the ViewModel means nothing to show. */
sealed interface ImportOffer {
    data object Asking : ImportOffer                      // "Bring in your FDV flights?"
    data object Reading : ImportOffer                     // logbook being read; progress shown
    data class Ready(val plan: ImportPlan) : ImportOffer  // what the import would do; pilot chooses
}