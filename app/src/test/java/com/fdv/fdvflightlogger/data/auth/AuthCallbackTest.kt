package com.fdv.fdvflightlogger.data.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthCallbackTest {

    private val state = "expected-state"

    @Test
    fun `matching state with a code succeeds`() {
        val result = AuthCallback.parse(mapOf("code" to "abc123", "state" to state), state)
        assertEquals(AuthCallback.Code("abc123"), result)
    }

    @Test
    fun `wrong state is rejected even with a code`() {
        val result = AuthCallback.parse(mapOf("code" to "abc123", "state" to "forged"), state)
        assertEquals(
            AuthCallback.Failure("Sign-in didn't match this request. Tap Sign in to try again."),
            result
        )
    }

    @Test
    fun `no sign-in in progress is reported as expired`() {
        val result = AuthCallback.parse(mapOf("code" to "abc123", "state" to state), null)
        assertEquals(AuthCallback.Failure("That sign-in has expired. Tap Sign in to try again."), result)
    }

    @Test
    fun `FDV error description is shown as sent`() {
        val result = AuthCallback.parse(
            mapOf("error" to "server_error", "error_description" to "Pilot account is on leave.", "state" to state),
            state
        )
        assertEquals(AuthCallback.Failure("Pilot account is on leave."), result)
    }

    @Test
    fun `known error without a description gets friendly text`() {
        val result = AuthCallback.parse(mapOf("error" to "access_denied", "state" to state), state)
        assertEquals(AuthCallback.Failure("Sign-in was cancelled."), result)
    }

    @Test
    fun `missing code is a failure`() {
        val result = AuthCallback.parse(mapOf("state" to state), state)
        assertEquals(AuthCallback.Failure("FDV didn't send a sign-in code. Try again."), result)
    }
}