package com.fdv.fdvflightlogger.data.help

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HelpTest {

    private val sample = """
        # Logging a flight
        Everything about a flight.

        ## Getting a gate from FDV
        <!-- keywords: parking, stand -->
        For a flight without a booking, FDV can pick a **gate**.

        1. Enter the airport.
        2. Tap **Get FDV gate**.

        > The chip only appears
        > while the Gate field is empty.

        ## Saving
        - Tap `Save Flight`
        - Auto-save runs every 60 seconds
    """.trimIndent()

    private val section = HelpParser.parseSection("03-logging.md", sample)!!

    @Test
    fun `a file becomes a section with topics`() {
        assertEquals("logging", section.id)
        assertEquals("Logging a flight", section.title)
        assertEquals("Everything about a flight.", section.intro)
        assertEquals(listOf("getting-a-gate-from-fdv", "saving"), section.topics.map { it.id })
        assertEquals(listOf("parking", "stand"), section.topics[0].keywords)
        assertFalse(section.topics[0].body.contains("<!--"))
    }

    @Test
    fun `topic markdown becomes blocks`() {
        assertEquals(
            listOf(
                HelpBlock.Paragraph("For a flight without a booking, FDV can pick a **gate**."),
                HelpBlock.Step(1, "Enter the airport."),
                HelpBlock.Step(2, "Tap **Get FDV gate**."),
                HelpBlock.Tip("The chip only appears while the Gate field is empty.")
            ),
            HelpParser.blocks(section.topics[0].body)
        )
        assertEquals(HelpBlock.Bullet("Tap `Save Flight`"), HelpParser.blocks(section.topics[1].body).first())
    }

    @Test
    fun `search needs every word, and keywords count`() {
        assertEquals("getting-a-gate-from-fdv", HelpSearch.search(listOf(section), "gate").first().topic.id)
        assertTrue(HelpSearch.search(listOf(section), "gate xyzzy").isEmpty())
        assertEquals("getting-a-gate-from-fdv", HelpSearch.search(listOf(section), "parking").single().topic.id)
    }

    @Test
    fun `snippet highlights the search words in the visible text`() {
        val result = HelpSearch.search(listOf(section), "auto-save").single()
        assertTrue(result.snippet.contains("Auto-save runs every 60 seconds"))
        assertEquals("auto-save", result.snippet.substring(result.highlights.single()).lowercase())
    }

    @Test
    fun `the bundled help files are all readable`() {
        // Unit tests run from the app module folder, so the real help files are on disk here
        val files = File("src/main/assets/help").listFiles { f -> HelpParser.FILE_NAME.matches(f.name) }.orEmpty()
        assertTrue("No help files found", files.isNotEmpty())
        files.forEach { file ->
            val parsed = HelpParser.parseSection(file.name, file.readText())
            assertNotNull("${file.name} has no # title", parsed)
            parsed!!
            assertTrue("${file.name} has no topics", parsed.topics.isNotEmpty())
            assertEquals("${file.name} repeats a topic title", parsed.topics.size, parsed.topics.map { it.id }.toSet().size)
        }
    }
}