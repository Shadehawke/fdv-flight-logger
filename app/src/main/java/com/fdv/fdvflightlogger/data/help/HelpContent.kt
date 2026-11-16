package com.fdv.fdvflightlogger.data.help

/** One help file: "Logging a flight", with its topics. */
data class HelpSection(
    val id: String,              // from the file name: 03-logging.md → "logging"
    val title: String,           // the file's "# " heading
    val intro: String,           // text between the title and the first topic
    val topics: List<HelpTopic>
)

/** One "## " heading in a help file, and everything under it. */
data class HelpTopic(
    val id: String,              // from the heading: "Getting a gate from FDV" → "getting-a-gate-from-fdv"
    val title: String,
    val keywords: List<String>,  // from <!-- keywords: … -->, lowercase; found by search, never shown
    val body: String             // the topic's Markdown
)

/** The pieces of Markdown the help uses. Anything else is shown as a plain paragraph. */
sealed interface HelpBlock {
    val text: String

    data class Heading(override val text: String) : HelpBlock                 // ### Heading
    data class Paragraph(override val text: String) : HelpBlock               // lines of text
    data class Bullet(override val text: String) : HelpBlock                  // - item
    data class Step(val number: Int, override val text: String) : HelpBlock   // 1. item
    data class Tip(override val text: String) : HelpBlock                     // > tip
}