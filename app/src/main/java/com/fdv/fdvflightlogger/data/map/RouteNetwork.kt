package com.fdv.fdvflightlogger.data.map

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

data class LatLon(val lat: Double, val lon: Double)

/** An airport on the map, and how often the pilot has been there (departures plus arrivals). */
data class MapAirport(val icao: String, val position: LatLon, val visits: Int)

/** A route flown in either direction (KATL–KJFK includes KJFK–KATL), and how many times. */
data class MapRoute(val from: String, val to: String, val flights: Int)

/** Everything the route map draws, worked out from a list of flights. */
data class RouteNetwork(
    val airports: List<MapAirport>,     // busiest first
    val routes: List<MapRoute>,         // least flown first, so busy routes draw on top
    val flightsShown: Int,
    val flightsWithoutPosition: Int     // flights with an airport the app has no coordinates for
) {
    val maxVisits: Int get() = airports.maxOfOrNull { it.visits } ?: 0
    val maxFlights: Int get() = routes.maxOfOrNull { it.flights } ?: 0

    companion object {
        fun build(flights: List<FlightLogEntity>, positionOf: (String) -> LatLon?): RouteNetwork {
            val positions = mutableMapOf<String, LatLon>()
            val visits = mutableMapOf<String, Int>()
            val routes = mutableMapOf<Pair<String, String>, Int>()
            var missing = 0

            for (flight in flights) {
                val dep = flight.dep.trim().uppercase()
                val arr = flight.arr.trim().uppercase()
                val depPos = positions[dep] ?: positionOf(dep)?.also { positions[dep] = it }
                val arrPos = positions[arr] ?: positionOf(arr)?.also { positions[arr] = it }
                if (dep.isEmpty() || arr.isEmpty() || depPos == null || arrPos == null) {
                    missing++
                    continue
                }
                visits[dep] = (visits[dep] ?: 0) + 1
                visits[arr] = (visits[arr] ?: 0) + 1
                if (dep != arr) {
                    // Sorted, so both directions count as one route
                    val key = if (dep < arr) dep to arr else arr to dep
                    routes[key] = (routes[key] ?: 0) + 1
                }
            }

            return RouteNetwork(
                airports = visits.map { (icao, count) -> MapAirport(icao, positions.getValue(icao), count) }
                    .sortedWith(compareByDescending<MapAirport> { it.visits }.thenBy { it.icao }),
                routes = routes.map { (key, count) -> MapRoute(key.first, key.second, count) }
                    .sortedWith(compareBy<MapRoute> { it.flights }.thenBy { it.from }.thenBy { it.to }),
                flightsShown = flights.size - missing,
                flightsWithoutPosition = missing
            )
        }
    }
}

object GreatCircle {

    /**
     * Points along the shortest path over the globe from [a] to [b], as lon, lat pairs. Longitudes are
     * "unwrapped" (they may go past ±180) so the line never jumps across the map; the map draws a copy
     * of the world either side, which shows such a line whole.
     */
    fun points(a: LatLon, b: LatLon): FloatArray {
        val v1 = unit(a)
        val v2 = unit(b)
        val angle = acos((v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2]).coerceIn(-1.0, 1.0))
        // About one point every 2 degrees of arc, at least a few for short hops
        val steps = max(4, (Math.toDegrees(angle) / 2).toInt())
        val out = FloatArray((steps + 1) * 2)
        var previousLon = a.lon
        for (i in 0..steps) {
            val t = i.toDouble() / steps
            val (lat, rawLon) = if (angle < 1e-9) {
                a.lat to a.lon
            } else {
                // Spherical interpolation: walk along the circle between the two points
                val s1 = sin((1 - t) * angle) / sin(angle)
                val s2 = sin(t * angle) / sin(angle)
                val x = s1 * v1[0] + s2 * v2[0]
                val y = s1 * v1[1] + s2 * v2[1]
                val z = s1 * v1[2] + s2 * v2[2]
                Math.toDegrees(atan2(z, hypot(x, y))) to Math.toDegrees(atan2(y, x))
            }
            var lon = rawLon
            while (lon - previousLon > 180) lon -= 360
            while (lon - previousLon < -180) lon += 360
            previousLon = lon
            out[i * 2] = lon.toFloat()
            out[i * 2 + 1] = lat.toFloat()
        }
        return out
    }

    // A position as a point on a sphere of radius 1
    private fun unit(p: LatLon): DoubleArray {
        val lat = Math.toRadians(p.lat)
        val lon = Math.toRadians(p.lon)
        return doubleArrayOf(cos(lat) * cos(lon), cos(lat) * sin(lon), sin(lat))
    }
}

/** What part of the world to show: a centre (lon, lat) and how many degrees across and down. */
data class MapView(val centerLon: Double, val centerLat: Double, val spanLon: Double, val spanLat: Double)

object MapFit {

    private const val PADDING = 1.25      // a margin around everything shown
    private const val MIN_SPAN = 8.0      // never fit closer than this many degrees across

    /**
     * The smallest view that shows every position. Longitude is a circle, so the widest gap between
     * positions is treated as "outside": routes across the Pacific fit as one view, not two halves.
     */
    fun fit(positions: List<LatLon>): MapView? {
        if (positions.isEmpty()) return null
        val lons = positions.map { normalise(it.lon) }.sorted()
        var gapStart = lons.last()
        var widestGap = lons.first() + 360 - lons.last()       // the gap that wraps around ±180
        for (i in 1 until lons.size) {
            val gap = lons[i] - lons[i - 1]
            if (gap > widestGap) {
                widestGap = gap
                gapStart = lons[i - 1]
            }
        }
        val west = gapStart + widestGap                        // the first position after the gap
        val spanLon = 360 - widestGap
        val south = positions.minOf { it.lat }
        val north = positions.maxOf { it.lat }
        return MapView(
            centerLon = normalise(west + spanLon / 2),
            centerLat = (south + north) / 2,
            spanLon = max(MIN_SPAN, spanLon * PADDING),
            spanLat = max(MIN_SPAN / 2, (north - south) * PADDING)
        )
    }

    /** Any longitude into -180..180. */
    fun normalise(lon: Double): Double {
        var l = lon % 360
        if (l > 180) l -= 360
        if (l < -180) l += 360
        return l
    }
}