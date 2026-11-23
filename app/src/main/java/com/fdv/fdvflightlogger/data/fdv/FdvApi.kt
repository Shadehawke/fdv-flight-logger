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
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException


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

    /** Every award and trophy, earned or not (include=all), newest first. */
    @Throws(FdvApiException::class)
    suspend fun awards(token: String): List<FdvAward> {
        val (status, body) = get("/me/awards?include=all", token)
        return interpretAwards(status, body)
    }

    /** Current and upcoming events (FDV sends at most 50), soonest first. */
    @Throws(FdvApiException::class)
    suspend fun events(token: String): List<FdvEvent> {
        val (status, body) = get("/events", token)
        return interpretEvents(status, body)
    }

    /** FDV's current and upcoming bonuses. */
    @Throws(FdvApiException::class)
    suspend fun bonuses(token: String): List<FdvBonus> {
        val (status, body) = get("/bonuses", token)
        return interpretBonuses(status, body)
    }

    /** A gate at [icao] chosen the way FDV does for bookings. Random on each call: ask once per flight. */
    @Throws(FdvApiException::class)
    suspend fun assignGate(token: String, icao: String, airline: String?, aircraft: String?): GateAssignment {
        val (status, body) = get(gateAssignPath(icao, airline, aircraft), token)
        return interpretGateAssign(status, body)
    }

    /** FDV's fleet keyed by ICAO type, with can-fly for this pilot's rank. */
    @Throws(FdvApiException::class)
    suspend fun fleet(token: String): Map<String, FdvFleetType> {
        val (status, body) = get("/fleet", token)
        return interpretFleet(status, body)
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
            hoursTransfer = me.hoursBreakdown?.transferApplied,
            rankWaivers = me.rankWaivers?.coerceAtLeast(0)
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

    internal fun interpretAwards(status: Int, body: String): List<FdvAward> {
        val list = okObject(status, body)["data"] as? JsonArray ?: throw FdvApiException(UNREADABLE)
        val dtos = try {
            AppHttp.json.decodeFromJsonElement(ListSerializer(AwardDto.serializer()), list)
        } catch (e: IllegalArgumentException) {
            throw FdvApiException(UNREADABLE)
        }
        return dtos.mapNotNull { dto ->
            val id = dto.id ?: return@mapNotNull null
            val name = dto.name?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            // An unknown future type is skipped rather than shown on the wrong shelf
            val kind = when (dto.type) {
                "award" -> AwardKind.AWARD
                "trophy" -> AwardKind.TROPHY
                else -> return@mapNotNull null
            }
            FdvAward(
                kind = kind,
                id = id,
                name = name,
                description = dto.description?.trim().orEmpty(),
                category = dto.category?.trim()?.takeIf { it.isNotEmpty() },
                requirement = dto.requirement?.trim()?.takeIf { it.isNotEmpty() },
                imageUrl = dto.imageUrl?.trim()?.takeIf { it.isNotEmpty() },
                earned = dto.earned,
                earnedAtMillis = dto.earnedAt?.let(::isoMillis),
                timesEarned = dto.timesEarned
            )
        }
    }

    internal fun interpretEvents(status: Int, body: String): List<FdvEvent> {
        val list = okObject(status, body)["data"] as? JsonArray ?: throw FdvApiException(UNREADABLE)
        val dtos = try {
            AppHttp.json.decodeFromJsonElement(ListSerializer(EventDto.serializer()), list)
        } catch (e: IllegalArgumentException) {
            throw FdvApiException(UNREADABLE)
        }
        return dtos.mapNotNull { dto ->
            val id = dto.id ?: return@mapNotNull null
            val name = dto.name?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            // Without a usable time window the app can't say when it runs, so it's skipped
            val starts = dto.startsAt?.let(::isoMillis) ?: return@mapNotNull null
            val ends = dto.endsAt?.let(::isoMillis)?.takeIf { it > starts } ?: return@mapNotNull null
            FdvEvent(
                id = id,
                name = name,
                category = dto.category?.trim()?.takeIf { it.isNotEmpty() },
                startsAtMillis = starts,
                endsAtMillis = ends,
                departureIcao = dto.departure?.icao?.trim()?.uppercase()?.takeIf { it.isNotEmpty() },
                departureName = dto.departure?.name?.trim()?.takeIf { it.isNotEmpty() },
                arrivalIcao = dto.arrival?.icao?.trim()?.uppercase()?.takeIf { it.isNotEmpty() },
                arrivalName = dto.arrival?.name?.trim()?.takeIf { it.isNotEmpty() },
                // FDV writes Windows line breaks; Compose wants plain \n
                description = dto.description.orEmpty().replace("\r\n", "\n").trim(),
                imageUrl = dto.imageUrl?.trim()?.takeIf { it.isNotEmpty() },
                url = dto.url?.trim()?.takeIf { it.isNotEmpty() },
                registered = dto.registered,
                registeredPilots = dto.registeredPilots ?: 0,
                maxPilots = dto.maxPilots,
                // FDV's own flags; a response without them falls back to the category rule
                categoryFree = dto.categoryFree ?: FlightBonuses.isBonusCategory(dto.category),
                doubleHours = dto.doubleHours ?: FlightBonuses.isBonusCategory(dto.category)
            )
        }.sortedBy { it.startsAtMillis }
    }

    internal fun interpretBonuses(status: Int, body: String): List<FdvBonus> {
        val list = okObject(status, body)["data"] as? JsonArray ?: throw FdvApiException(UNREADABLE)
        val dtos = try {
            AppHttp.json.decodeFromJsonElement(ListSerializer(BonusDto.serializer()), list)
        } catch (e: IllegalArgumentException) {
            throw FdvApiException(UNREADABLE)
        }
        return dtos.mapNotNull { dto ->
            // A match rule the app doesn't know can't be judged, so that bonus is left out rather than guessed
            val match = when (dto.match?.trim()?.lowercase()) {
                "route" -> BonusMatch.ROUTE
                "either_end" -> BonusMatch.EITHER_END
                "departure" -> BonusMatch.DEPARTURE
                "arrival" -> BonusMatch.ARRIVAL
                "any" -> BonusMatch.ANY
                else -> return@mapNotNull null
            }
            val starts = dto.startsAt?.let(::isoMillis) ?: return@mapNotNull null
            val ends = dto.endsAt?.let(::isoMillis)?.takeIf { it > starts } ?: return@mapNotNull null
            val type = dto.type?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: "bonus"
            FdvBonus(
                type = type,
                label = dto.label?.trim()?.takeIf { it.isNotEmpty() } ?: FlightBonuses.typeLabel(type),
                departureIcao = dto.departure?.icao?.trim()?.uppercase()?.takeIf { it.isNotEmpty() },
                arrivalIcao = dto.arrival?.icao?.trim()?.uppercase()?.takeIf { it.isNotEmpty() },
                startsAtMillis = starts,
                endsAtMillis = ends,
                categoryFree = dto.categoryFree,
                doubleHours = dto.doubleHours,
                match = match
            )
        }.sortedBy { it.startsAtMillis }
    }

    /**
     * FDV wants an airline ICAO code and an aircraft ICAO type. A typed airline name falls back to
     * Delta, and an aircraft that isn't a type code is left out, so neither turns into a 422.
     */
    internal fun gateAssignPath(icao: String, airline: String?, aircraft: String?): String {
        val carrier = airline?.trim()?.uppercase()?.takeIf { AIRLINE_CODE.matches(it) } ?: "DAL"
        val type = aircraft?.trim()?.uppercase()?.takeIf { AIRCRAFT_CODE.matches(it) }
        return buildString {
            append("/airports/").append(icao.trim().uppercase()).append("/gates/assign")
            append("?airline=").append(carrier)
            if (type != null) append("&aircraft=").append(type)
        }
    }

    internal fun interpretGateAssign(status: Int, body: String): GateAssignment {
        // An airport FDV doesn't know has no gates to give: that's an answer, not an error
        if (status == 404) return GateAssignment.NoGates
        val root = okObject(status, body)
        // "gate": null (no gates on file) isn't a JsonObject, so it lands here too
        val gateJson = root["gate"] as? JsonObject ?: return GateAssignment.NoGates
        val dto = try {
            AppHttp.json.decodeFromJsonElement(GateDto.serializer(), gateJson)
        } catch (e: IllegalArgumentException) {
            throw FdvApiException(UNREADABLE)
        }
        val gate = dto.gate?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: return GateAssignment.NoGates
        return GateAssignment.Assigned(FdvGate(terminal = dto.terminal?.trim()?.takeIf { it.isNotEmpty() }, gate = gate))
    }

    internal fun interpretFleet(status: Int, body: String): Map<String, FdvFleetType> {
        val list = okObject(status, body)["data"] as? JsonArray ?: throw FdvApiException(UNREADABLE)
        val dtos = try {
            AppHttp.json.decodeFromJsonElement(ListSerializer(FleetDto.serializer()), list)
        } catch (e: IllegalArgumentException) {
            throw FdvApiException(UNREADABLE)
        }
        val types = dtos.mapNotNull { dto ->
            val icao = dto.icao?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            FdvFleetType(
                icao = icao,
                name = dto.name?.trim()?.takeIf { it.isNotEmpty() } ?: icao,
                manufacturer = dto.manufacturer?.trim()?.takeIf { it.isNotEmpty() },
                passengers = dto.passengers,
                rangeNm = dto.rangeNm,
                minRank = dto.minRank?.name?.trim()?.takeIf { it.isNotEmpty() },
                canFly = dto.canFly,
                registrations = dto.registrations,
                anyRank = dto.anyRank
            )
        }
        // Keyed by type so the picker can look one up instantly. If FDV ever lists a type twice,
        // the first one wins (associateBy alone would keep the last).
        return types.distinctBy { it.icao }.associateBy { it.icao }
    }

    private val AIRLINE_CODE = Regex("[A-Z]{3}")
    private val AIRCRAFT_CODE = Regex("[A-Z0-9]{2,4}")

    private fun isoMillis(raw: String): Long? = try {
        OffsetDateTime.parse(raw).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
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