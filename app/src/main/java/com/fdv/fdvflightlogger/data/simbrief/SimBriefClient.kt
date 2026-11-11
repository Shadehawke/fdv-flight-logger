package com.fdv.fdvflightlogger.data.simbrief

import android.util.Log
import com.fdv.fdvflightlogger.data.network.AppHttp
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

/** A failure with a message written for the pilot, safe to show as-is. */
class SimBriefException(message: String) : Exception(message)

object SimBriefClient {

    private const val ENDPOINT = "https://www.simbrief.com/api/xml.fetcher.php"

    /**
     * Downloads the pilot's most recent OFP as JSON.
     * SimBrief monitors this endpoint: call it only from a user action, never on a timer.
     */
    @Throws(SimBriefException::class)
    suspend fun fetchLatestOfp(alias: String, appVersion: String): String {
        val (status, body) = try {
            val response = AppHttp.client.get(ENDPOINT) {
                // parameter() URL-encodes each value, so spaces or '&' in an alias are safe
                parameter("username", alias.trim())
                parameter("client", "fdv-flight-logger-android-$appVersion")
                parameter("json", "v2")
                accept(ContentType.Application.Json)
            }
            response.status to response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            // No connection, DNS failure, or timeout (Ktor's timeout exceptions are IOExceptions)
            throw offline()
        }

        if (status == HttpStatusCode.OK) return body

        // Logged so a failed import can be diagnosed in Logcat; truncated to keep the log readable
        Log.w("SimBrief", "HTTP ${status.value}: ${body.take(300)}")
        throw SimBriefException(errorMessage(body, status.value))
    }

    // Errors may arrive as {"fetch":{"status":"Error: ..."}} or, despite json=v2, as XML <status>...</status>
    private fun errorMessage(body: String, httpStatus: Int): String {
        val status = runCatching {
            AppHttp.json.parseToJsonElement(body)
                .jsonObject["fetch"]?.jsonObject
                ?.get("status")?.jsonPrimitive?.contentOrNull
        }.getOrNull()
            ?: Regex("<status>(.*?)</status>").find(body)?.groupValues?.get(1)

        val detail = status?.removePrefix("Error:")?.trim().orEmpty()
        return if (detail.isNotEmpty()) {
            // SimBrief's own wording is more accurate than any guess we could make
            "SimBrief: $detail"
        } else {
            "SimBrief couldn't return a flight plan (HTTP $httpStatus). Try again in a moment."
        }
    }

    private fun offline() =
        SimBriefException("Couldn't reach SimBrief. Check your internet connection.")
}