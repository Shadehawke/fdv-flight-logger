package com.fdv.fdvflightlogger.data.whatsnew

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WhatsNewTest {

    private val notes = WhatsNew("1.5.1", "### Fixed\n- Something")

    @Test
    fun `parse reads the version and the body`() {
        val parsed = WhatsNew.parse("# 1.5.1\n\n### Fixed\n- Air times\n")
        assertEquals("1.5.1", parsed?.version)
        assertEquals("### Fixed\n- Air times", parsed?.body)
    }

    @Test
    fun `parse accepts a v before the version`() {
        assertEquals("1.5.1", WhatsNew.parse("# v1.5.1\n- Item")?.version)
    }

    @Test
    fun `parse rejects a file without a version title or without notes`() {
        assertNull(WhatsNew.parse("### Fixed\n- Item"))
        assertNull(WhatsNew.parse("# 1.5.1\n\n"))
        assertNull(WhatsNew.parse(""))
    }

    @Test
    fun `a fresh install shows nothing`() {
        assertFalse(WhatsNew.shouldShow(notes, current = "1.5.1", lastSeen = null, updatedInPlace = false))
    }

    @Test
    fun `the first update from 1_5_0 shows the notes`() {
        assertTrue(WhatsNew.shouldShow(notes, current = "1.5.1", lastSeen = null, updatedInPlace = true))
    }

    @Test
    fun `a later update shows the notes`() {
        val next = WhatsNew("1.5.2", "- Item")
        assertTrue(WhatsNew.shouldShow(next, current = "1.5.2", lastSeen = "1.5.1", updatedInPlace = true))
    }

    @Test
    fun `notes already seen are not shown again`() {
        assertFalse(WhatsNew.shouldShow(notes, current = "1.5.1", lastSeen = "1.5.1", updatedInPlace = true))
    }

    @Test
    fun `notes left over from an older release are not shown`() {
        assertFalse(WhatsNew.shouldShow(notes, current = "1.5.2", lastSeen = "1.5.1", updatedInPlace = true))
        assertFalse(WhatsNew.shouldShow(null, current = "1.5.2", lastSeen = "1.5.1", updatedInPlace = true))
    }

    @Test
    fun `the bundled notes file parses`() {
        // Unit tests run from the app module folder, so the asset can be read straight from source
        val text = File("src/main/assets/${WhatsNew.ASSET_NAME}").readText()
        assertNotNull("whats-new.md needs a '# <version>' first line and some notes", WhatsNew.parse(text))
    }
}