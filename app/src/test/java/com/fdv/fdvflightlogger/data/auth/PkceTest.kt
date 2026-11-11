package com.fdv.fdvflightlogger.data.auth

import io.ktor.http.Url
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PkceTest {

    @Test
    fun `challenge matches the RFC 7636 example`() {
        // Appendix B of the PKCE spec: if this passes, FDV's server will compute the same hash
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            Pkce.challengeFor("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
        )
    }

    @Test
    fun `new session has a valid verifier and matching challenge`() {
        val session = Pkce.newSession()

        assertEquals(43, session.verifier.length)
        // Only the spec's unreserved characters; padding would make this fail
        assertTrue(session.verifier.matches(Regex("[A-Za-z0-9_-]+")))
        assertEquals(Pkce.challengeFor(session.verifier), session.challenge)
        assertTrue(session.state.isNotEmpty())
    }

    @Test
    fun `every session is different`() {
        val a = Pkce.newSession()
        val b = Pkce.newSession()
        assertNotEquals(a.verifier, b.verifier)
        assertNotEquals(a.state, b.state)
    }

    @Test
    fun `authorize URL carries every PKCE parameter`() {
        val session = Pkce.newSession()
        val url = Url(FdvAuth.authorizeUrl(session))

        assertEquals("flydeltavirtual.com", url.host)
        assertEquals("/oauth/authorize", url.encodedPath)
        // Url decodes for us, so these compare against the raw values
        assertEquals(FdvAuth.REDIRECT_URI, url.parameters["redirect_uri"])
        assertEquals(session.state, url.parameters["state"])
        assertEquals(session.challenge, url.parameters["code_challenge"])
        assertEquals("S256", url.parameters["code_challenge_method"])
    }

    @Test
    fun `VATSIM shortcut uses the API route`() {
        val url = Url(FdvAuth.authorizeUrl(Pkce.newSession(), viaVatsim = true))
        assertEquals("/api/v1/auth/vatsim/authorize", url.encodedPath)
    }
}