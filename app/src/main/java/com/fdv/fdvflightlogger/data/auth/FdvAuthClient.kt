package com.fdv.fdvflightlogger.data.auth

import com.fdv.fdvflightlogger.data.network.AppHttp
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import java.io.IOException

class FdvAuthException(message: String) : Exception(message)

object FdvAuthClient {

    private const val UNREADABLE = "FlyDeltaVirtual sent a sign-in response the app couldn't read."

    // The API documents 30 days; used only if a response ever leaves expires_in out
    private const val DEFAULT_LIFETIME_SECONDS = 30L * 24 * 60 * 60

    @Throws(FdvAuthException::class)
    suspend fun exchangeCode(code: String, verifier: String): FdvSession {
        val (status, body) = try {
            val response = AppHttp.client.post("${FdvAuth.API_BASE}/auth/token") {
                contentType(ContentType.Application.Json)
                // Without this, Laravel answers validation errors with an HTML redirect
                accept(ContentType.Application.Json)
                setBody(TokenRequest(code, verifier, FdvAuth.REDIRECT_URI))
            }
            response.status.value to response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw FdvAuthException("Couldn't reach FlyDeltaVirtual. Check your connection and try again.")
        }
        return interpret(status, body, System.currentTimeMillis())
    }

    /** Revokes the token on FDV's side. Best effort: if offline, the token still expires on its own. */
    suspend fun logout(token: String) {
        try {
            AppHttp.client.post("${FdvAuth.API_BASE}/auth/logout") {
                bearerAuth(token)
                accept(ContentType.Application.Json)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            // Nothing to do; the local copy is already gone
        }
    }

    /** Turns FDV's reply into a session or a readable error. Pure, so it's unit tested directly. */
    internal fun interpret(status: Int, body: String, nowMillis: Long): FdvSession {
        val json = AppHttp.json

        if (status in 200..299) {
            val reply = try {
                json.decodeFromString(TokenResponse.serializer(), body)
            } catch (e: IllegalArgumentException) {
                throw FdvAuthException(UNREADABLE)
            }
            val token = reply.token?.takeIf { it.isNotBlank() && reply.success != false }
                ?: throw FdvAuthException(reply.message?.takeIf { it.isNotBlank() } ?: UNREADABLE)

            return FdvSession(
                token = token,
                expiresAtMillis = nowMillis + (reply.expiresIn ?: DEFAULT_LIFETIME_SECONDS) * 1000,
                callsign = reply.pilot?.callsign,
                firstName = reply.pilot?.firstname,
                lastName = reply.pilot?.lastname
            )
        }

        // Error replies: show FDV's own message when there is one
        val error = try {
            json.decodeFromString(ApiError.serializer(), body)
        } catch (e: IllegalArgumentException) {
            null
        }
        throw FdvAuthException(
            error?.message?.takeIf { it.isNotBlank() }
                ?: "FDV sign-in failed (HTTP $status). Tap Sign in to try again."
        )
    }
}