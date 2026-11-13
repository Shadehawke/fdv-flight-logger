package com.fdv.fdvflightlogger.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UpdateTest {

    @Test
    fun `versions compare as numbers, not text`() {
        assertTrue(AppVersion.isNewer("1.10.0", "1.9.0"))
        assertTrue(AppVersion.isNewer("v1.2.0", "1.1.0"))
        assertTrue(AppVersion.isNewer("1.2.1", "1.2"))
        assertFalse(AppVersion.isNewer("1.2", "1.2.0"))   // same version
        assertFalse(AppVersion.isNewer("1.1.0", "1.2.0"))
    }

    @Test
    fun `older tag styles still parse`() {
        assertEquals(listOf(1, 1), AppVersion.parse("v1.1_Full"))
        assertEquals(listOf(3), AppVersion.parse("v3"))
        assertNull(AppVersion.parse("latest"))
        assertFalse(AppVersion.isNewer("latest", "1.0.0"))   // unreadable is never newer
    }

    @Test
    fun `release maps the APK and its checksum`() {
        val body = """
            {"tag_name": "v1.2.0", "name": "FDV Flight Logger 1.2.0", "body": "- In-app updates\n- Air times",
             "html_url": "https://github.com/Shadehawke/fdv-flight-logger/releases/tag/v1.2.0",
             "draft": false, "prerelease": false,
             "assets": [
               {"name": "checksums.txt", "browser_download_url": "https://example.com/checksums.txt", "size": 120},
               {"name": "fdv-flight-logger-1.2.0.apk",
                "browser_download_url": "https://github.com/Shadehawke/fdv-flight-logger/releases/download/v1.2.0/fdv-flight-logger-1.2.0.apk",
                "size": 18234567, "digest": "sha256:abc123"}
             ]}
        """.trimIndent()

        val release = ReleaseChecker.interpret(200, body)!!

        assertEquals("1.2.0", release.version)
        assertEquals("FDV Flight Logger 1.2.0", release.title)
        assertEquals("- In-app updates\n- Air times", release.notes)
        assertTrue(release.apkUrl!!.endsWith("fdv-flight-logger-1.2.0.apk"))   // not the checksums file
        assertEquals(18234567L, release.apkSize)
        assertEquals("abc123", release.apkSha256)
    }

    @Test
    fun `no releases yet is not an error`() {
        assertNull(ReleaseChecker.interpret(404, """{"message": "Not Found"}"""))
    }

    @Test
    fun `pre-releases are ignored`() {
        assertNull(ReleaseChecker.interpret(200, """{"tag_name": "v2.0.0", "prerelease": true}"""))
    }

    @Test
    fun `rate limit has its own message`() {
        val e = assertThrows(UpdateException::class.java) {
            ReleaseChecker.interpret(403, """{"message": "API rate limit exceeded"}""")
        }
        assertEquals("GitHub is limiting update checks right now. Try again in a while.", e.message)
    }

    @Test
    fun `sha256 matches the standard test vector`() {
        // FIPS 180-2's example: SHA-256 of "abc"
        val file = File.createTempFile("sha", ".txt").apply {
            writeText("abc")
            deleteOnExit()
        }
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            UpdateDownloader.sha256(file)
        )
    }
}