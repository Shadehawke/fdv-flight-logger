package com.fdv.fdvflightlogger.data.fdv

import com.fdv.fdvflightlogger.data.auth.FdvAuth
import com.fdv.fdvflightlogger.data.network.AppHttp
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.IOException


/**
 * [signedOut] is true when FDV rejected the token, so the app should drop the stored session.
 * [retryAfterSeconds] is set on HTTP 429, so a long-running import can wait and carry on.
 */
class FdvApiException(
    message: String,
    val signedOut: Boolean = false,
    val retryAfterSeconds: Int? = null
) : Exception(message)

/** Authenticated calls to the FDV App API. Every call takes the session token. */
object FdvApi {

    private const val UNREADABLE = "FlyDeltaVirtual sent a response the app couldn't read."
    private const val PIREP_PAGE_SIZE = 50   // the API's maximum
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

    /** One page of the logbook, newest first. [afterId] limits it to flights newer than that one. */
    @Throws(FdvApiException::class)
    suspend fun pireps(token: String, page: Int = 1, afterId: Long? = null): FdvPirepPage {
        val path = buildString {
            append("/pireps?per_page=$PIREP_PAGE_SIZE&page=$page")
            if (afterId != null) append("&after_id=$afterId")
        }
        val (status, body) = get(path, token)
        return interpretPireps(status, body)
    }

    /** One PIREP in full; the only source of air time. */
    @Throws(FdvApiException::class)
    suspend fun pirep(token: String, id: Long): FdvPirep {
        val (status, body) = get("/pireps/$id", token)
        return interpretPirep(status, body)
    }

    private suspend fun get(path: String, token: String): Pair<Int, String> = try {
        val response = AppHttp.client.get("${FdvAuth.API_BASE}$path") {
            bearerAuth(token)
            accept(ContentType.Application.Json)
        }
        // 60 requests a minute per pilot; FDV says how long to wait
        if (response.status.value == 429) throw rateLimited(response.headers[HttpHeaders.RetryAfter])
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
            streakBest = me.streak?.best,
            rankHoursRequired = me.rank?.hoursRequired,
            nextRank = me.nextRank?.name?.takeIf { it.isNotBlank() },
            nextRankHoursRequired = me.nextRank?.hoursRequired,
            nextRankHoursRemaining = me.nextRank?.hoursRemaining,
            // Only a real last flight counts as "last landed"; the hub fallback isn't a landing
            lastFlightLocation = me.location
                ?.takeIf { it.source == "last_flight" }
                ?.icao?.takeIf { it.isNotBlank() },
            hoursFlown = me.hoursBreakdown?.flown,
            hoursBonus = me.hoursBreakdown?.bonus,
            hoursTransfer = me.hoursBreakdown?.transferApplied
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

    internal fun interpretPireps(status: Int, body: String): FdvPirepPage {
        val root = okObject(status, body)
        val list = root["data"] as? JsonArray ?: throw FdvApiException(UNREADABLE)
        val (dtos, meta) = try {
            AppHttp.json.decodeFromJsonElement(ListSerializer(PirepDto.serializer()), list) to
                    root["meta"]?.let { AppHttp.json.decodeFromJsonElement(PageMetaDto.serializer(), it) }
        } catch (e: IllegalArgumentException) {
            throw FdvApiException(UNREADABLE)
        }
        val page = meta?.page ?: 1
        return FdvPirepPage(
            pireps = dtos.mapNotNull(FdvPirepMapper::toPirep),
            page = page,
            // Without meta, treat this as the last page so a loop can never run forever
            lastPage = meta?.lastPage ?: page,
            total = meta?.total
        )
    }

    internal fun interpretPirep(status: Int, body: String): FdvPirep {
        val dto = try {
            AppHttp.json.decodeFromJsonElement(PirepDto.serializer(), okObject(status, body))
        } catch (e: IllegalArgumentException) {
            throw FdvApiException(UNREADABLE)
        }
        return FdvPirepMapper.toPirep(dto) ?: throw FdvApiException(UNREADABLE)
    }

    internal fun rateLimited(retryAfter: String?): FdvApiException {
        val seconds = retryAfter?.trim()?.toIntOrNull()?.takeIf { it > 0 }
        val wait = seconds?.let { "in $it seconds" } ?: "in a minute"
        return FdvApiException(
            "FlyDeltaVirtual is limiting requests. Try again $wait.",
            retryAfterSeconds = seconds
        )
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