package com.fdv.fdvflightlogger.data.fdv

import com.fdv.fdvflightlogger.data.auth.FdvAuth
import com.fdv.fdvflightlogger.data.network.AppHttp
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.IOException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray

/** [signedOut] is true when FDV rejected the token, so the app should drop the stored session. */
class FdvApiException(message: String, val signedOut: Boolean = false) : Exception(message)

/** Authenticated calls to the FDV App API. Every call takes the session token. */
object FdvApi {

    private const val UNREADABLE = "FlyDeltaVirtual sent a response the app couldn't read."
    private const val SIGNED_OUT = "Your FDV sign-in has expired. Sign in again."

    @Throws(FdvApiException::class)
    suspend fun me(token: String): FdvPilot {
        val (status, body) = get("/me", token)
        return interpretMe(status, body)
    }

    @Throws(FdvApiException::class)
    suspend fun bids(token: String): List<FdvBid> {
        val (status, body) = get("/bids", token)
        return interpretBids(status, body)
    }

    @Throws(FdvApiException::class)
    suspend fun bid(token: String, id: Long): FdvBidPlan {
        val (status, body) = get("/bids/$id", token)
        return interpretBid(status, body)
    }

    private suspend fun get(path: String, token: String): Pair<Int, String> = try {
        val response = AppHttp.client.get("${FdvAuth.API_BASE}$path") {
            bearerAuth(token)
            accept(ContentType.Application.Json)
        }
        response.status.value to response.bodyAsText()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        throw FdvApiException("Couldn't reach FlyDeltaVirtual. Check your connection and try again.")
    }

    internal fun interpretMe(status: Int, body: String): FdvPilot {
        val root = okObject(status, body)
        val me = try {
            AppHttp.json.decodeFromJsonElement(MeDto.serializer(), root)
        } catch (e: IllegalArgumentException) {
            throw FdvApiException(UNREADABLE)
        }
        val callsign = me.callsign?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw FdvApiException(UNREADABLE)

        return FdvPilot(
            callsign = callsign,
            name = listOfNotNull(me.firstname, me.lastname)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(" "),
            rank = me.rank?.name?.takeIf { it.isNotBlank() },
            hubIcao = me.hub?.icao?.takeIf { it.isNotBlank() },
            hubName = me.hub?.name?.takeIf { it.isNotBlank() },
            hours = me.hours,
            flights = me.flights,
            streakCurrent = me.streak?.current,
            streakBest = me.streak?.best
        )
    }

    internal fun interpretBids(status: Int, body: String): List<FdvBid> {
        // The list comes as {"data": [...]}; okObject only unwraps objects, so it hands back the root
        val list = okObject(status, body)["data"] as? JsonArray ?: throw FdvApiException(UNREADABLE)
        val dtos = try {
            AppHttp.json.decodeFromJsonElement(ListSerializer(BidDto.serializer()), list)
        } catch (e: IllegalArgumentException) {
            throw FdvApiException(UNREADABLE)
        }
        return dtos.mapNotNull(FdvBidMapper::toBid)
    }

    internal fun interpretBid(status: Int, body: String): FdvBidPlan {
        val dto = try {
            AppHttp.json.decodeFromJsonElement(BidDto.serializer(), okObject(status, body))
        } catch (e: IllegalArgumentException) {
            throw FdvApiException(UNREADABLE)
        }
        return FdvBidMapper.toBidPlan(dto) ?: throw FdvApiException(UNREADABLE)
    }

    /**
     * Shared by every endpoint: maps 401 and other errors, then returns the JSON object.
     * Laravel APIs often wrap results as {"data": {...}}, so that's unwrapped when present.
     */
    internal fun okObject(status: Int, body: String): JsonObject {
        if (status == 401) throw FdvApiException(SIGNED_OUT, signedOut = true)
        if (status !in 200..299) {
            throw FdvApiException(
                messageOf(body) ?: "FlyDeltaVirtual returned an error (HTTP $status). Try again in a moment."
            )
        }
        val root = try {
            AppHttp.json.parseToJsonElement(body) as? JsonObject
        } catch (e: SerializationException) {
            null
        } ?: throw FdvApiException(UNREADABLE)
        return root["data"] as? JsonObject ?: root
    }

    // FDV's 401/404 bodies and Laravel's validation errors all carry a "message"
    private fun messageOf(body: String): String? = try {
        val message = (AppHttp.json.parseToJsonElement(body) as? JsonObject)?.get("message")
        (message as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    } catch (e: SerializationException) {
        null
    }

}