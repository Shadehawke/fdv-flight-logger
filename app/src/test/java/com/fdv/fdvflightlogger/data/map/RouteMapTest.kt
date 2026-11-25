package com.fdv.fdvflightlogger.data.map

import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.abs

class RouteMapTest {

    private val positions = mapOf(
        "KATL" to LatLon(33.64, -84.43),
        "KJFK" to LatLon(40.64, -73.78),
        "KSEA" to LatLon(47.45, -122.31),
        "RJAA" to LatLon(35.77, 140.39),
        "LFPG" to LatLon(49.01, 2.55)
    )

    private fun flight(id: Long, dep: String, arr: String) = FlightLogEntity(id = id, dep = dep, arr = arr)

    @Test
    fun `routes merge both directions and unknown airports are counted, not drawn`() {
        val network = RouteNetwork.build(
            listOf(flight(1, "KATL", "KJFK"), flight(2, "KJFK", "katl"), flight(3, "KSEA", "RJAA"), flight(4, "KATL", "XXXX"))
        ) { positions[it] }

        assertEquals(listOf(MapRoute("KSEA", "RJAA", 1), MapRoute("KATL", "KJFK", 2)), network.routes)   // busiest last
        assertEquals("KATL", network.airports.first().icao)
        assertEquals(2, network.airports.first().visits)
        assertEquals(3, network.flightsShown)
        assertEquals(1, network.flightsWithoutPosition)
    }

    @Test
    fun `a Pacific great circle is unwrapped and arcs toward the pole`() {
        val points = GreatCircle.points(positions.getValue("KSEA"), positions.getValue("RJAA"))
        val lons = points.filterIndexed { i, _ -> i % 2 == 0 }
        val lats = points.filterIndexed { i, _ -> i % 2 == 1 }

        assertEquals(-122.31f, lons.first(), 0.01f)
        assertEquals(140.39f - 360f, lons.last(), 0.01f)                 // continues west past -180
        assertTrue(lons.zipWithNext().all { (a, b) -> abs(b - a) < 10 })  // no jumps across the map
        assertTrue(lats.max() > 54f)                                      // higher than either airport
    }

    @Test
    fun `fitting a Pacific route centres on the Pacific`() {
        val view = MapFit.fit(listOf(positions.getValue("KSEA"), positions.getValue("RJAA")))!!
        assertTrue(abs(view.centerLon) > 150)          // over the Pacific, not over Europe
        assertTrue(view.spanLon < 180)                 // the short way round
    }

    @Test
    fun `a single airport still gets a sensible view`() {
        val view = MapFit.fit(listOf(positions.getValue("KATL")))!!
        assertEquals(-84.43, view.centerLon, 0.001)
        assertEquals(8.0, view.spanLon, 0.001)
    }

    @Test
    fun `world file reads back what was written`() {
        val bytes = ByteArrayOutputStream().also { buffer ->
            DataOutputStream(buffer).apply {
                write("FDVW".toByteArray())
                writeByte(1)
                writeInt(1); writeInt(3)                                    // one land ring, 3 points
                writeShort(-8443); writeShort(3364)
                writeShort(-7378); writeShort(4064)
                writeShort(255); writeShort(4901)
                writeInt(0)                                                 // no borders
            }
        }.toByteArray()

        val world = WorldShapes.parse(bytes)
        assertEquals(1, world.land.size)
        assertArrayEquals(floatArrayOf(-84.43f, 33.64f, -73.78f, 40.64f, 2.55f, 49.01f), world.land[0], 0.001f)
        assertTrue(world.borders.isEmpty())
        assertThrows(IOException::class.java) { WorldShapes.parse("NOPE!".toByteArray()) }
    }
}