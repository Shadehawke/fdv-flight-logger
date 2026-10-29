package com.fdv.fdvflightlogger.data.simbrief

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

/** A failure with a message written for the pilot, safe to show as-is. */
class SimBriefException(message: String) : Exception(message)

object SimBriefClient {

    private const val ENDPOINT = "https://www.simbrief.com/api/xml.fetcher.php"
    private const val TIMEOUT_MS = 15_000

    /**
     * Downloads the pilot's most recent OFP as JSON.
     * SimBrief monitors this endpoint: call it only from a user action, never on a timer.
     */
    @Throws(SimBriefException::class)
    suspend fun fetchLatestOfp(alias: String, appVersion: String): String =
        withContext(Dispatchers.IO) {
            val url = URI.create(
                ENDPOINT +
                        "?username=" + URLEncoder.encode(alias.trim(), "UTF-8") +
                        "&client=" + URLEncoder.encode("fdv-flight-logger-android-$appVersion", "UTF-8") +
                        "&json=v2"
            ).toURL()

            val connection = try {
                url.openConnection() as HttpURLConnection
            } catch (e: IOException) {
                throw offline()
            }

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.setRequestProperty("Accept", "application/json")

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    // 400 responses carry a short explanation in the error stream
                    val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    throw SimBriefException(errorMessage(body, alias))
                }
            } catch (e: IOException) {
                throw offline()
            } finally {
                connection.disconnect()
            }
        }

    // Errors may arrive as {"fetch":{"status":"Error: ..."}} or, despite json=v2, as XML <status>...</status>
    private fun errorMessage(body: String, alias: String): String {
        val status = runCatching {
            JSONObject(body).optJSONObject("fetch")?.optString("status")
        }.getOrNull()
            ?: Regex("<status>(.*?)</status>").find(body)?.groupValues?.get(1)

        val detail = status?.removePrefix("Error:")?.trim().orEmpty()
        return when {
            detail.contains("user", ignoreCase = true) ->
                "SimBrief doesn't recognize the alias \"${alias.trim()}\". Check it in Settings."
            detail.isNotEmpty() -> "SimBrief: $detail"
            else -> "SimBrief couldn't return a flight plan. Try again in a moment."
        }
    }

    private fun offline() =
        SimBriefException("Couldn't reach SimBrief. Check your internet connection.")
}