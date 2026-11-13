package com.fdv.fdvflightlogger.data.fdv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FdvAwardsTest {

    private val body = """
        {"data": [
          {"type": "trophy", "id": 1, "name": "New Airport",
           "description": "Awarded for landing at an airport that the pilot has never landed at before.",
           "category": "Uncommon", "requirement": null, "image_url": null,
           "earned": true, "earned_at": "2026-08-06T18:25:18+00:00", "times_earned": 18},
          {"type": "award", "id": 152, "name": "100K Passengers",
           "description": "Awarded to pilots who safely transport 100K Passengers!",
           "category": "passengers", "requirement": "100000 passengers",
           "image_url": "https://flydeltavirtual.com/private/L9PtTJ5TUklqbtUSDepCDd3kLgDuUM6r6D1RXH6k.png",
           "earned": true, "earned_at": "2026-04-01T02:20:05+00:00", "times_earned": 1},
          {"type": "award", "id": 24, "name": "Piano Landing", "description": "Landing Rate of -88fpm",
           "category": "landing", "requirement": "between -88 fpm and -88 fpm",
           "image_url": "https://flydeltavirtual.com/private/CvdfDwVrSGPQM0sqWjakdu1XijF1fJIKTQofGNpk.jpg",
           "earned": false, "earned_at": null, "times_earned": 0},
          {"type": "badge", "id": 9, "name": "Something new"}
        ]}
    """.trimIndent()

    @Test
    fun `awards and trophies map from the live shape`() {
        val awards = FdvApi.interpretAwards(200, body)

        assertEquals(3, awards.size)    // the unknown "badge" type is skipped

        val trophy = awards[0]
        assertEquals(AwardKind.TROPHY, trophy.kind)
        assertEquals("Uncommon", trophy.category)
        assertEquals(18, trophy.timesEarned)
        assertNull(trophy.imageUrl)
        assertEquals(Instant.parse("2026-08-06T18:25:18Z").toEpochMilli(), trophy.earnedAtMillis)

        val award = awards[1]
        assertEquals(AwardKind.AWARD, award.kind)
        assertEquals("100000 passengers", award.requirement)
        assertTrue(award.imageUrl!!.endsWith(".png"))
        assertTrue(award.earned)
    }

    @Test
    fun `locked award has no earned date`() {
        val locked = FdvApi.interpretAwards(200, body)[2]
        assertFalse(locked.earned)
        assertNull(locked.earnedAtMillis)
        assertEquals(0, locked.timesEarned)
    }
}