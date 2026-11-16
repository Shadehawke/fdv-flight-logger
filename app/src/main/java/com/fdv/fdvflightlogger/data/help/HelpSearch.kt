package com.fdv.fdvflightlogger.data.help

/** A topic that matched a search, with a short extract and where the search words fall in it. */
data class HelpResult(
    val section: HelpSection,
    val topic: HelpTopic,
    val snippet: String,
    val highlights: List<IntRange>
)

object HelpSearch {

    private const val SNIPPET_BEFORE = 60
    private const val SNIPPET_AFTER = 120

    /** "Gate  FDV" → ["gate", "fdv"] */
    fun terms(query: String): List<String> =
        query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }.distinct()

    /**
     * Topics containing every search word (in the title, text, keywords or section title),
     * best first: title matches count most, then keywords, then how often the words appear.
     */
    fun search(sections: List<HelpSection>, query: String, limit: Int = 30): List<HelpResult> {
        val terms = terms(query)
        if (terms.isEmpty()) return emptyList()

        val scored = sections.flatMapIndexed { sectionIndex, section ->
            val sectionTitle = section.title.lowercase()
            section.topics.mapIndexedNotNull { topicIndex, topic ->
                val title = topic.title.lowercase()
                val plain = HelpParser.plainText(topic.body)
                val text = plain.lowercase()

                fun inKeywords(term: String) = topic.keywords.any { it.contains(term) }

                val allFound = terms.all { t -> t in title || t in text || inKeywords(t) || t in sectionTitle }
                if (!allFound) return@mapIndexedNotNull null

                val score = terms.sumOf { t ->
                    (if (t in title) 10 else 0) +
                            (if (inKeywords(t)) 6 else 0) +
                            (if (t in sectionTitle) 2 else 0) +
                            occurrences(text, t).coerceAtMost(5)
                }
                val snippet = snippet(plain, terms)
                Scored(score, sectionIndex, topicIndex, HelpResult(section, topic, snippet, highlights(snippet, terms)))
            }
        }

        return scored
            // Ties keep the order the help is written in
            .sortedWith(compareByDescending<Scored> { it.score }.thenBy { it.sectionIndex }.thenBy { it.topicIndex })
            .take(limit)
            .map { it.result }
    }

    /** About 180 characters around the first search word, cut at spaces, with … where text was cut. */
    internal fun snippet(plain: String, terms: List<String>): String {
        val lower = plain.lowercase()
        val hit = terms.mapNotNull { t -> lower.indexOf(t).takeIf { it >= 0 } }.minOrNull() ?: 0

        var start = (hit - SNIPPET_BEFORE).coerceAtLeast(0)
        var end = (hit + SNIPPET_AFTER).coerceAtMost(plain.length)
        // Start and end on whole words
        if (start > 0) plain.indexOf(' ', start).takeIf { it in start until hit }?.let { start = it + 1 }
        if (end < plain.length) plain.lastIndexOf(' ', end).takeIf { it > hit }?.let { end = it }

        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < plain.length) "…" else ""
        return prefix + plain.substring(start, end).trim() + suffix
    }

    /** Every place a search word appears in [text], in order. */
    fun highlights(text: String, terms: List<String>): List<IntRange> {
        val lower = text.lowercase()
        return terms
            .flatMap { term ->
                generateSequence(lower.indexOf(term).takeIf { it >= 0 }) { previous ->
                    lower.indexOf(term, previous + term.length).takeIf { it >= 0 }
                }.map { it until it + term.length }.toList()
            }
            .sortedBy { it.first }
    }

    private fun occurrences(text: String, term: String): Int =
        generateSequence(text.indexOf(term).takeIf { it >= 0 }) { previous ->
            text.indexOf(term, previous + term.length).takeIf { it >= 0 }
        }.count()

    private data class Scored(val score: Int, val sectionIndex: Int, val topicIndex: Int, val result: HelpResult)
}