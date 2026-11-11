package com.fdv.fdvflightlogger.data.auth

import io.ktor.http.URLBuilder

/** Where FDV sign-in lives and how to build the URL that opens it. */
object FdvAuth {

    const val SITE = "https://flydeltavirtual.com"
    const val API_BASE = "$SITE/api/v1"

    // Must match the intent filter in AndroidManifest.xml (step 3b) and what FDV IT registered
    const val REDIRECT_URI = "fdvlogger://auth/callback"

    /**
     * The page to open in the browser. [viaVatsim] skips FDV's own login form and goes
     * straight to VATSIM (the pilot's FDV account must already be linked to their CID).
     */
    fun authorizeUrl(session: PkceSession, viaVatsim: Boolean = false): String {
        val base = if (viaVatsim) "$API_BASE/auth/vatsim/authorize" else "$SITE/oauth/authorize"
        return URLBuilder(base).apply {
            parameters.append("redirect_uri", REDIRECT_URI)
            parameters.append("state", session.state)
            parameters.append("code_challenge", session.challenge)
            parameters.append("code_challenge_method", "S256")
        }.buildString()
    }
}