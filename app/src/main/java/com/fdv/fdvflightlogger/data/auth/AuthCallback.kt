package com.fdv.fdvflightlogger.data.auth

/** What FDV's redirect back to the app means, once checked against the sign-in we started. */
sealed interface AuthCallback {

    data class Code(val code: String) : AuthCallback

    data class Failure(val message: String) : AuthCallback

    companion object {

        /**
         * [params] are the query parameters of fdvlogger://auth/callback.
         * [expectedState] is the state we sent, or null if no sign-in is in progress.
         */
        fun parse(params: Map<String, String?>, expectedState: String?): AuthCallback {
            // No sign-in in progress: the app was restarted mid-login, or the link was replayed
            if (expectedState == null) {
                return Failure("That sign-in has expired. Tap Sign in to try again.")
            }
            // A callback we didn't start: reject it before looking at anything else
            if (params["state"] != expectedState) {
                return Failure("Sign-in didn't match this request. Tap Sign in to try again.")
            }

            params["error"]?.let { error ->
                // FDV's own wording is the most useful thing to show, when it sends one
                val description = params["error_description"]?.takeIf { it.isNotBlank() }
                return Failure(description ?: friendlyError(error))
            }

            val code = params["code"]?.takeIf { it.isNotBlank() }
                ?: return Failure("FDV didn't send a sign-in code. Try again.")
            return Code(code)
        }

        private fun friendlyError(error: String): String = when (error) {
            "access_denied" -> "Sign-in was cancelled."
            "no_account" -> "No FDV account is linked to that login."
            "cid_mismatch" -> "That VATSIM account isn't the one linked to your FDV profile."
            else -> "FDV sign-in failed ($error)."
        }
    }
}