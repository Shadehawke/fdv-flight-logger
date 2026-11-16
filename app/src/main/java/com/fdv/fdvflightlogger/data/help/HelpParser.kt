package com.fdv.fdvflightlogger.data.help

/**
 * Reads the help files in assets/help. Each file is one section:
 *
 *     # Section title
 *     Intro paragraph.
 *
 *     ## Topic title
 *     <!-- keywords: extra, search, words -->
 *     Topic text…
 *
 * Inside a topic: ### headings, paragraphs, "- " bullets, "1. " steps, "> " tips, **bold** and `code`.
 * The same files read normally on GitHub, where the keywords comments are invisible.
 */
object HelpParser {

    /** Only numbered files are help sections; the number sets the order. */
    val FILE_NAME = Regex("""^\d+-.+\.md$""")

    private val KEYWORDS = Regex("""<!--\s*keywords:(.*?)-->""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val LEADING_NUMBER = Regex("""^\d+[-_]""")
    private val STEP = Regex("""^(\d+)\.\s+(.*)$""")
    private val NOT_SLUG = Regex("[^a-z0-9]+")

    /** One file into a section, or null if it has no "# " title. */
    fun parseSection(fileName: String, text: String): HelpSection? {
        val lines = text.replace("\r\n", "\n").lines()
        val title = lines.firstOrNull { it.startsWith("# ") }?.removePrefix("# ")?.trim() ?: return null

        val intro = StringBuilder()
        val topics = mutableListOf<HelpTopic>()
        var topicTitle: String? = null
        val body = StringBuilder()

        // Closes the topic being read, if there is one
        fun finishTopic() {
            topicTitle?.let { topics += topic(it, body.toString()) }
            body.clear()
        }

        for (line in lines) {
            when {
                line.startsWith("# ") -> Unit
                line.startsWith("## ") -> {
                    finishTopic()
                    topicTitle = line.removePrefix("## ").trim()
                }
                topicTitle == null -> intro.appendLine(line)
                else -> body.appendLine(line)
            }
        }
        finishTopic()

        return HelpSection(
            id = slug(fileName.substringBeforeLast('.').replace(LEADING_NUMBER, "")),
            title = title,
            intro = plainText(intro.toString()),
            topics = topics
        )
    }

    /** A topic's Markdown into blocks for the screen. */
    fun blocks(markdown: String): List<HelpBlock> {
        val blocks = mutableListOf<HelpBlock>()
        val paragraph = StringBuilder()

        fun finishParagraph() {
            if (paragraph.isNotBlank()) blocks += HelpBlock.Paragraph(paragraph.toString().trim())
            paragraph.clear()
        }

        for (raw in markdown.lines()) {
            val line = raw.trim()
            val step = STEP.matchEntire(line)
            when {
                line.isEmpty() -> finishParagraph()
                line.startsWith("### ") -> {
                    finishParagraph()
                    blocks += HelpBlock.Heading(line.removePrefix("### ").trim())
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    finishParagraph()
                    blocks += HelpBlock.Bullet(line.drop(2).trim())
                }
                step != null -> {
                    finishParagraph()
                    blocks += HelpBlock.Step(step.groupValues[1].toInt(), step.groupValues[2].trim())
                }
                line.startsWith(">") -> {
                    finishParagraph()
                    val text = line.removePrefix(">").trim()
                    // Consecutive "> " lines are one tip
                    val last = blocks.lastOrNull()
                    if (last is HelpBlock.Tip) blocks[blocks.lastIndex] = HelpBlock.Tip("${last.text} $text")
                    else blocks += HelpBlock.Tip(text)
                }
                // Lines of one paragraph are joined, as Markdown does
                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(line)
                }
            }
        }
        finishParagraph()
        return blocks
    }

    /** The words a reader sees, with Markdown marks removed: for search and snippets. */
    fun plainText(markdown: String): String =
        blocks(markdown).joinToString(" ") { stripMarks(it.text) }

    fun stripMarks(text: String): String = text.replace("**", "").replace("`", "")

    fun slug(text: String): String = text.lowercase().replace(NOT_SLUG, "-").trim('-')

    private fun topic(title: String, raw: String): HelpTopic {
        val keywords = KEYWORDS.findAll(raw)
            .flatMap { it.groupValues[1].split(',') }
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toList()
        return HelpTopic(
            id = slug(title),
            title = title,
            keywords = keywords,
            body = raw.replace(KEYWORDS, "").trim()
        )
    }
}