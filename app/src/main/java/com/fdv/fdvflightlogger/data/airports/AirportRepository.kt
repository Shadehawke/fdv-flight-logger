package com.fdv.fdvflightlogger.data.airports

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

object AirportRepository {

    private const val ASSET_NAME = "airports.json"

    @Volatile
    private var airports: List<Airport> = emptyList()

    @Volatile
    private var byIcao: Map<String, Airport> = emptyMap()

    // Guards against two callers parsing the file at the same time.
    private val loadLock = Mutex()

    val isLoaded: Boolean get() = airports.isNotEmpty()

    /** Loads airports.json once; later calls return immediately. */
    suspend fun ensureLoaded(context: Context) {
        if (isLoaded) return
        loadLock.withLock {
            if (isLoaded) return
            val parsed = withContext(Dispatchers.IO) { parse(context.applicationContext) }
            byIcao = parsed.associateBy { it.icao }
            airports = parsed
        }
    }

    fun getByIcao(icao: String?): Airport? =
        icao?.trim()?.uppercase()?.let { byIcao[it] }

    fun runwaysFor(icao: String?): List<String> =
        getByIcao(icao)?.runways.orEmpty()

    /**
     * Ranked search across ICAO, IATA, name and city.
     * Code matches rank above name matches so typing "ATL" surfaces KATL first.
     */
    fun search(query: String, limit: Int = 8): List<Airport> {
        val q = query.trim().uppercase()
        if (q.length < 2) return emptyList()

        return airports
            .mapNotNull { airport -> rank(airport, q)?.let { it to airport } }
            .sortedWith(compareBy({ it.first }, { it.second.icao }))
            .take(limit)
            .map { it.second }
    }

    // Lower rank = better match; null = no match.
    private fun rank(a: Airport, q: String): Int? = when {
        a.icao == q -> 0
        a.iata == q -> 1
        a.icao.startsWith(q) -> 2
        a.iata.isNotEmpty() && a.iata.startsWith(q) -> 3
        a.cityKey.startsWith(q) -> 4
        a.nameKey.startsWith(q) -> 5
        a.cityKey.contains(q) || a.nameKey.contains(q) -> 6
        else -> null
    }

    private fun parse(context: Context): List<Airport> {
        val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val array = JSONArray(text)

        return List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            val rwys = obj.optJSONArray("rwys")
            Airport(
                icao = obj.getString("icao"),
                iata = obj.optString("iata"),
                name = obj.optString("name"),
                city = obj.optString("city"),
                country = obj.optString("country"),
                runways = if (rwys == null) emptyList() else List(rwys.length()) { rwys.getString(it) }
            )
        }
    }
}