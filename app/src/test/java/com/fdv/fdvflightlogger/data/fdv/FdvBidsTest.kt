package com.fdv.fdvflightlogger.data.fdv

import com.fdv.fdvflightlogger.data.prefs.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FdvBidsTest {

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource(name)) { "Missing test resource $name" }.readText()

    @Test
    fun `bid list maps the real response`() {
        val bids = FdvApi.interpretBids(200, fixture("fdv_bids.json"))

        assertEquals(1, bids.size)
        val bid = bids.single()
        assertEquals(181225L, bid.id)
        assertEquals("DAL1060", bid.callsign)
        assertEquals("KATL", bid.dep)
        assertEquals("KAUS", bid.arr)
        assertEquals("A321", bid.aircraft)
        assertEquals("21:16", bid.departUtc)
        assertTrue(bid.hasSimBrief)
    }

    @Test
    fun `empty bid list is not an error`() {
        val bids = FdvApi.interpretBids(200, """{"data": []}""")
        assertTrue(bids.isEmpty())
    }

    @Test
    fun `bid detail with a SimBrief plan maps the real response`() {
        val bidPlan = FdvApi.interpretBid(200, fixture("fdv_bid_detail.json"))
        val plan = bidPlan.plan

        assertEquals("KATL", plan.dep)
        assertEquals("KAUS", plan.arr)
        assertEquals("DAL", plan.airline)
        assertEquals("1060", plan.flightNumber)
        assertEquals("A321", plan.aircraft)
        assertEquals("POUNC2 STEIT DCT MEI DCT LCH DCT LUKKN WLEEE7", plan.route)
        assertEquals("POUNC2", plan.sid)
        assertEquals("WLEEE7", plan.star)
        assertEquals("280", plan.cruiseFl)       // SimBrief's plan, not the timetable's 380
        assertEquals("31", plan.costIndex)
        assertEquals("26L", plan.depRwy)
        assertEquals("36L", plan.arrRwy)
        assertEquals("KDFW", plan.altn)

        assertEquals(WeightUnit.LB, plan.weightUnit)
        assertEquals(24221.0, plan.blockFuel!!, 0.0)
        assertEquals(4215.0, plan.reserveFuel!!, 0.0)
        assertEquals(150541.0, plan.zfw!!, 0.0)
        assertNull(plan.payload)
        assertEquals("180", plan.pax)

        assertEquals("0226", plan.blockTime)
        assertEquals("0203", plan.airTime)       // 123 minutes enroute

        assertEquals("F13", bidPlan.depGate)
        assertEquals("A1", bidPlan.arrGate)

        assertEquals(170187L, bidPlan.scheduleId)
        assertNull(bidPlan.charterId)
    }

    @Test
    fun `bid detail without a plan or gates falls back to the timetable`() {
        // The shape of your first 4a log, before the SimBrief plan was generated
        val body = """
            {"id": 181225, "flight": "DAL1060", "airline": {"icao": "DAL"},
             "departure": {"icao": "KATL"}, "arrival": {"icao": "KAUS"}, "aircraft": {"icao": "A321"},
             "duration": "02:26", "cruise_fl": 380, "route": null,
             "gates": {"departure": null, "arrival": null, "alternate": null},
             "simbrief_ofp_id": null, "simbrief": null}
        """.trimIndent()

        val bidPlan = FdvApi.interpretBid(200, body)

        assertEquals("380", bidPlan.plan.cruiseFl)
        assertEquals("0226", bidPlan.plan.blockTime)
        assertNull(bidPlan.plan.route)
        assertNull(bidPlan.plan.sid)
        assertNull(bidPlan.plan.blockFuel)
        assertNull(bidPlan.depGate)
        assertNull(bidPlan.arrGate)
    }

    @Test
    fun `route starting with a fix has no SID`() {
        val body = """
            {"id": 1, "departure": {"icao": "KATL"}, "arrival": {"icao": "KAUS"},
             "simbrief": {"route": "STEIT DCT MEI DCT LUKKN WLEEE7"}}
        """.trimIndent()

        val plan = FdvApi.interpretBid(200, body).plan
        assertNull(plan.sid)
        assertEquals("WLEEE7", plan.star)
    }

    @Test
    fun `cancelled booking shows FDV's 404 message`() {
        val e = assertThrows(FdvApiException::class.java) {
            FdvApi.interpretBid(404, """{"message": "Booking not found."}""")
        }
        assertEquals("Booking not found.", e.message)
    }
}