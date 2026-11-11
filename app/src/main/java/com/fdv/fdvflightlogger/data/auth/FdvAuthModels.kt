package com.fdv.fdvflightlogger.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body of POST /auth/token. */
@Serializable
internal data class TokenRequest(
    val code: String,
    @SerialName("code_verifier") val codeVerifier: String,
    @SerialName("redirect_uri") val redirectUri: String
)

/** Reply from POST /auth/token. Everything is optional so a surprise shape is a clean error, not a crash. */
@Serializable
internal data class TokenResponse(
    val success: Boolean? = null,
    val token: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    val pilot: AuthPilot? = null,
    val message: String? = null
)

@Serializable
internal data class AuthPilot(
    val callsign: String? = null,
    val firstname: String? = null,
    val lastname: String? = null
)

/** FDV's error shape ({success, error, message}); Laravel validation errors also carry "message". */
@Serializable
internal data class ApiError(
    val error: String? = null,
    val message: String? = null
)

/** A signed-in pilot, as the app keeps it (encrypted at rest by FdvSessionStore). */
@Serializable
data class FdvSession(
    val token: String,
    val expiresAtMillis: Long,
    val callsign: String?,
    val firstName: String?,
    val lastName: String?
) {
    fun isExpired(nowMillis: Long): Boolean = nowMillis >= expiresAtMillis

    // Data classes print every field by default; keep the token out of logs and crash reports
    override fun toString(): String =
        "FdvSession(callsign=$callsign, expiresAtMillis=$expiresAtMillis, token=***)"
}