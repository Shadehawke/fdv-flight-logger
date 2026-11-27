package com.fdv.fdvflightlogger.data.feedback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackFormTest {

    @Test
    fun `the real form link is filled in`() {
        // Fails until FORM_URL and the two entry IDs come from the form's "Get pre-filled link",
        // so a release can't ship with the buttons hidden by mistake
        assertTrue("Fill in FeedbackForm from the form's pre-filled link", FeedbackForm.isConfigured)
    }

    @Test
    fun `answers are encoded into the link`() {
        val url = FeedbackForm.url(FeedbackForm.Kind.PROBLEM, "1.5.0", "16", "samsung SM-F966U")
        assertTrue(url.startsWith("${FeedbackForm.FORM_URL}?usp=pp_url&"))
        assertTrue(url.contains("${FeedbackForm.KIND_ENTRY}=Bug%20Report"))
        assertTrue(url.contains("Android%2016"))
        assertFalse(url.contains(" "))
    }

    @Test
    fun `a suggestion picks the form's Feature Request option`() {
        val url = FeedbackForm.url(FeedbackForm.Kind.SUGGESTION, "1.5.0", "16", "samsung SM-F966U")
        assertTrue(url.contains("${FeedbackForm.KIND_ENTRY}=Feature%20Request"))
    }
}