package com.fdv.fdvflightlogger.data.simbrief

import com.fdv.fdvflightlogger.data.prefs.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SimBriefParserTest {

    // Reads a file from src/test/resources, which Gradle puts on the test classpath
    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource(name)) { "Missing test resource $name" }.readText()

    @Test
    fun `parses the real KPHX to KATL sample`() {
        val plan = SimBriefParser.parse(fixture("simbrief_sample.json"))

        assertEquals("KPHX", plan.dep)
        assertEquals("KATL", plan.arr)
        assertEquals("25R", plan.depRwy)
        assertEquals("26R", plan.arrRwy)
        assertEquals("KHSV", plan.altn)
        assertEquals("BROAK1", plan.sid)
        assertEquals("GLAVN2", plan.star)
        assertEquals("350", plan.cruiseFl)
        assertEquals("21", plan.costIndex)
        assertEquals("DAL", plan.airline)
        assertEquals("366", plan.flightNumber)
        assertEquals("A21N", plan.aircraft)

        assertEquals(WeightUnit.LB, plan.weightUnit)
        assertEquals(25696.0, plan.blockFuel!!, 0.0)
        assertEquals(1316.0, plan.reserveFuel!!, 0.0)
        assertEquals(53793.0, plan.payload!!, 0.0)
        assertEquals(163098.0, plan.zfw!!, 0.0)
        assertEquals("244", plan.pax)

        assertEquals("0334", plan.blockTime)
        assertEquals("0306", plan.airTime)

        assertEquals(283, plan.windDir)
        assertEquals(24, plan.windSpd)       // "024" from SimBrief
        assertEquals(-45, plan.cruiseOatC)   // mean of the 12 CRZ fixes is -44.67

        assertEquals("2", plan.takeoffFlaps) // 25R's row, not the first runway listed
        assertEquals("154", plan.v2)
        assertEquals("FULL", plan.landingFlaps)
        assertEquals("141", plan.vref)
        assertEquals("29.87", plan.depAltimeter)
        assertEquals("30.16", plan.arrAltimeter)
    }

    @Test
    fun `treats empty objects and blank strings as missing`() {
        // Older-format quirks: alternate as a single object, navlog wrapped in "fix", numbers as numbers
        val json = """
            {
              "params": { "units": "kgs" },
              "origin": { "icao_code": "EGLL", "plan_rwy": {} },
              "destination": { "icao_code": "KJFK", "plan_rwy": "" },
              "alternate": { "icao_code": "KEWR" },
              "general": { "sid_ident": {}, "route": "   ", "costindex": 30 },
              "weights": { "est_zfw": 60000, "pax_count": 150 },
              "navlog": { "fix": [
                { "stage": "CRZ", "oat": "-50" },
                { "stage": "CRZ", "oat": {} },
                { "stage": "DSC", "oat": "10" }
              ] }
            }
        """.trimIndent()

        val plan = SimBriefParser.parse(json)

        assertEquals(WeightUnit.KG, plan.weightUnit)
        assertNull(plan.depRwy)
        assertNull(plan.arrRwy)
        assertEquals("KEWR", plan.altn)
        assertNull(plan.sid)
        assertNull(plan.route)
        assertEquals("30", plan.costIndex)
        assertEquals(60000.0, plan.zfw!!, 0.0)
        assertEquals("150", plan.pax)
        assertEquals(-50, plan.cruiseOatC)   // the {} fix is skipped, the DSC fix ignored
        assertNull(plan.takeoffFlaps)        // no tlr block at all
    }

    @Test
    fun `rejects text that is not JSON`() {
        val e = assertThrows(SimBriefException::class.java) { SimBriefParser.parse("<html>502</html>") }
        assertEquals("SimBrief sent a response the app couldn't read.", e.message)
    }

    @Test
    fun `rejects JSON that is not an object`() {
        val e = assertThrows(SimBriefException::class.java) { SimBriefParser.parse("[]") }
        assertEquals("SimBrief sent a response the app couldn't read.", e.message)
    }

    @Test
    fun `reports a plan with no departure airport`() {
        val e = assertThrows(SimBriefException::class.java) {
            SimBriefParser.parse("""{ "origin": {}, "destination": { "icao_code": "KATL" } }""")
        }
        assertEquals("Your latest SimBrief plan has no departure airport.", e.message)
    }
}