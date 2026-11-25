package com.fdv.fdvflightlogger.data.feedback

import java.net.URLEncoder

/**
 * The feedback Google Form. Values come from the form's "Get pre-filled link":
 * FORM_URL is everything before "?", the entries are the entry.N numbers.
 */
object FeedbackForm {

    const val FORM_URL = "https://docs.google.com/forms/d/e/1FAIpQLSc0RcrlTTVrXdhHY2NwlWxeXCL-T7yXnWZe51KrRQDxj4SOGA/viewform"
    // Multiple choice "What's this about?"; its options must read exactly as Kind's formValues
    const val KIND_ENTRY = "entry.613143809"
    // Short answer "App and device", filled in for the pilot
    const val DETAILS_ENTRY = "entry.341113248"

    enum class Kind(val formValue: String) {
        PROBLEM("Bug Report"),
        SUGGESTION("Feature Request")
    }

    private val ENTRY = Regex("""entry\.\d+""")

    /** False until the real form's link is filled in above; the buttons stay hidden until then. */
    val isConfigured: Boolean
        get() = FORM_URL.startsWith("https://docs.google.com/forms/") && "FORM_ID" !in FORM_URL &&
                ENTRY.matches(KIND_ENTRY) && ENTRY.matches(DETAILS_ENTRY)

    /** The form, with the kind chosen and "FDV Flight Logger 1.5.0 · Android 16 · samsung SM-F966U" filled in. */
    fun url(kind: Kind, appVersion: String, androidVersion: String, device: String): String {
        val details = "FDV Flight Logger $appVersion · Android $androidVersion · $device"
        return "$FORM_URL?usp=pp_url&$KIND_ENTRY=${encode(kind.formValue)}&$DETAILS_ENTRY=${encode(details)}"
    }

    // %20 rather than + for spaces: both work in Google Forms, %20 is right everywhere
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}