package com.fdv.fdvflightlogger.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class FdvAuthClientTest {

    private val now = 1_000_000L

    @Test
    fun `successful exchange becomes a session`() {
        val body = """
            {"success": true, "token": "jwt.abc.def", "token_type": "bearer", "expires_in": 2592000,
             "pilot": {"id": 42, "callsign": "DAL1234", "firstname": "Shawn", "lastname": "Pilot"}}
        """.trimIndent()

        val session = FdvAuthClient.interpret(200, body, now)

        assertEquals("jwt.abc.def", session.token)
        assertEquals(now + 2_592_000_000L, session.expiresAtMillis)
        assertEquals("DAL1234", session.callsign)
        assertEquals("Shawn", session.firstName)
        assertEquals("Pilot", session.lastName)
    }

    @Test
    fun `toString never shows the token`() {
        val session = FdvAuthClient.interpret(200, """{"token": "secret-jwt"}""", now)
        assertFalse(session.toString().contains("secret-jwt"))
    }

    @Test
    fun `missing expires_in falls back to 30 days`() {
        val session = FdvAuthClient.interpret(200, """{"success": true, "token": "t"}""", now)
        assertEquals(now + 30L * 24 * 60 * 60 * 1000, session.expiresAtMillis)
    }

    @Test
    fun `success false with a 200 shows FDV's message`() {
        val e = assertThrows(FdvAuthException::class.java) {
            FdvAuthClient.interpret(200, """{"success": false, "message": "Code already used."}""", now)
        }
        assertEquals("Code already used.", e.message)
    }

    @Test
    fun `error status shows FDV's message`() {
        val e = assertThrows(FdvAuthException::class.java) {
            FdvAuthClient.interpret(
                400,
                """{"success": false, "error": "invalid_grant", "message": "The code has expired."}""",
                now
            )
        }
        assertEquals("The code has expired.", e.message)
    }

    @Test
    fun `Laravel validation error shows its message`() {
        val e = assertThrows(FdvAuthException::class.java) {
            FdvAuthClient.interpret(
                422,
                """{"message": "The code verifier field is required.", "errors": {"code_verifier": ["required"]}}""",
                now
            )
        }
        assertEquals("The code verifier field is required.", e.message)
    }

    @Test
    fun `non-JSON error falls back to the status code`() {
        val e = assertThrows(FdvAuthException::class.java) {
            FdvAuthClient.interpret(502, "<html>Bad Gateway</html>", now)
        }
        assertEquals("FDV sign-in failed (HTTP 502). Tap Sign in to try again.", e.message)
    }

    @Test
    fun `200 without a token is unreadable`() {
        val e = assertThrows(FdvAuthException::class.java) {
            FdvAuthClient.interpret(200, """{"success": true}""", now)
        }
        assertEquals("FlyDeltaVirtual sent a sign-in response the app couldn't read.", e.message)
    }
}