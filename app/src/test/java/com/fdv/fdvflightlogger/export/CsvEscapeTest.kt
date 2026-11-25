package com.fdv.fdvflightlogger.export

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvEscapeTest {

    @Test
    fun `cells that would run as formulas become text`() {
        assertEquals("'=1+1", ExportCsv.csvEscape("=1+1"))
        assertEquals("'@SUM(A1:A9)", ExportCsv.csvEscape("@SUM(A1:A9)"))
        assertEquals("'+cmd", ExportCsv.csvEscape("+cmd"))
        assertEquals("'-2+3", ExportCsv.csvEscape("-2+3"))
        assertEquals("'\tsneaky", ExportCsv.csvEscape("\tsneaky"))
    }

    @Test
    fun `numbers stay numbers`() {
        assertEquals("-142", ExportCsv.csvEscape("-142"))      // a landing rate
        assertEquals("-12.5", ExportCsv.csvEscape("-12.5"))
        assertEquals("+3", ExportCsv.csvEscape("+3"))
    }

    @Test
    fun `quoting still works, including on neutralised cells`() {
        assertEquals("\"Hello, world\"", ExportCsv.csvEscape("Hello, world"))
        assertEquals("\"Say \"\"hi\"\"\"", ExportCsv.csvEscape("Say \"hi\""))
        assertEquals("\"'=A1,B1\"", ExportCsv.csvEscape("=A1,B1"))
        assertEquals("", ExportCsv.csvEscape(null))
    }
}