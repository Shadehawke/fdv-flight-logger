package com.fdv.fdvflightlogger.data.fdv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FdvGateAssignTest {

    @Test
    fun `assigned gate maps from the live shape`() {
        val body = """
            {"airport": {"icao": "KATL", "name": "Hartsfield-Jackson Atlanta Intl"},
             "gate": {"id": 812, "terminal": "Concourse F", "gate": "f13", "max_wingspan_ft": 214,
                      "airlines": ["DAL"]}}
        """.trimIndent()
        assertEquals(
            GateAssignment.Assigned(FdvGate(terminal = "Concourse F", gate = "F13")),
            FdvApi.interpretGateAssign(200, body)
        )
    }

    @Test
    fun `no gates on file, or an unknown airport, is NoGates`() {
        val none = """{"airport": {"icao": "KXYZ", "name": "Somewhere"}, "gate": null}"""
        assertEquals(GateAssignment.NoGates, FdvApi.interpretGateAssign(200, none))
        assertEquals(GateAssignment.NoGates, FdvApi.interpretGateAssign(404, """{"message": "Not found."}"""))
    }

    @Test
    fun `validation error shows FDV's message`() {
        val e = assertThrows(FdvApiException::class.java) {
            FdvApi.interpretGateAssign(422, """{"message": "The selected aircraft is invalid."}""")
        }
        assertEquals("The selected aircraft is invalid.", e.message)
    }

    @Test
    fun `path falls back to Delta and drops a non-type aircraft`() {
        assertEquals(
            "/airports/KATL/gates/assign?airline=DAL&aircraft=A321",
            FdvApi.gateAssignPath("katl", "dal", "a321")
        )
        assertEquals(
            "/airports/KJFK/gates/assign?airline=DAL",
            FdvApi.gateAssignPath("KJFK", "Delta Air Lines", "Airbus A321neo")
        )
    }
}