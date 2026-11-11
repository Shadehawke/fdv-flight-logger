package com.fdv.fdvflightlogger.data.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** One sign-in attempt's secrets. Lives only in memory until the browser comes back. */
data class PkceSession(
    val verifier: String,
    val challenge: String,
    val state: String
)

object Pkce {

    // SecureRandom, not Random: these values guard the sign-in, so they must be unpredictable
    private val random = SecureRandom()

    // RFC 7636 wants base64url (- and _ instead of + and /) with the trailing = padding removed
    private val base64Url = Base64.getUrlEncoder().withoutPadding()

    fun newSession(): PkceSession {
        // 32 random bytes encode to exactly 43 characters, the shortest verifier the spec allows
        val verifier = randomToken(32)
        return PkceSession(
            verifier = verifier,
            challenge = challengeFor(verifier),
            state = randomToken(16)
        )
    }

    fun challengeFor(verifier: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return base64Url.encodeToString(hash)
    }

    private fun randomToken(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        random.nextBytes(bytes)
        return base64Url.encodeToString(bytes)
    }
}