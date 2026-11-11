package com.fdv.fdvflightlogger.data.auth

import com.fdv.fdvflightlogger.data.network.AppHttp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FdvSessionTest {

    private val session = FdvSession("jwt.abc", expiresAtMillis = 1_000L, "DAL1234", "Shawn", "Pilot")

    @Test
    fun `expires exactly at its expiry time`() {
        assertFalse(session.isExpired(999L))
        assertTrue(session.isExpired(1_000L))
    }

    @Test
    fun `survives the JSON round trip the store uses`() {
        val json = AppHttp.json.encodeToString(FdvSession.serializer(), session)
        assertEquals(session, AppHttp.json.decodeFromString(FdvSession.serializer(), json))
    }
}