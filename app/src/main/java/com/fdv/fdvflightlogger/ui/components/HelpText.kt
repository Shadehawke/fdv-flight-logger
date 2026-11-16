package com.fdv.fdvflightlogger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.fdv.fdvflightlogger.data.help.HelpBlock
import com.fdv.fdvflightlogger.data.help.HelpParser
import com.fdv.fdvflightlogger.data.help.HelpSearch

// A highlighter-pen yellow, see-through so it works on light and dark backgrounds
private val Highlight = SpanStyle(background = Color(0xFFFFD54F).copy(alpha = 0.45f))

/** A help topic's Markdown, laid out: headings, paragraphs, bullets, numbered steps and tips. */
@Composable
fun HelpBody(markdown: String, terms: List<String>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val blocks = remember(markdown) { HelpParser.blocks(markdown) }
    val code = SpanStyle(fontFamily = FontFamily.Monospace, background = cs.surfaceVariant)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            val text = remember(block, terms, code) { styled(block.text, terms, code) }
            when (block) {
                is HelpBlock.Heading -> Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
                is HelpBlock.Paragraph -> Text(text, style = MaterialTheme.typography.bodyLarge)
                is HelpBlock.Bullet -> Row {
                    Text("•", style = MaterialTheme.typography.bodyLarge, color = cs.primary, modifier = Modifier.width(22.dp))
                    Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
                is HelpBlock.Step -> Row {
                    Text(
                        text = "${block.number}.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.primary,
                        modifier = Modifier.width(28.dp)
                    )
                    Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
                is HelpBlock.Tip -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cs.secondaryContainer.copy(alpha = 0.6f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = "Tip", tint = cs.secondary, modifier = Modifier.size(20.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Plain text (a search snippet) with the given ranges highlighted. */
@Composable
fun highlighted(text: String, ranges: List<IntRange>): AnnotatedString = remember(text, ranges) {
    buildAnnotatedString {
        append(text)
        ranges.forEach { addStyle(Highlight, it.first, it.last + 1) }
    }
}

/** **bold** and `code` styled with their marks removed, then search words highlighted over the result. */
private fun styled(text: String, terms: List<String>, code: SpanStyle): AnnotatedString {
    val marked = buildAnnotatedString {
        var bold = false
        var i = 0
        val run = StringBuilder()

        // Writes the text collected so far, bold or not, in one piece
        fun flush() {
            if (run.isEmpty()) return
            if (bold) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(run.toString()) }
            else append(run.toString())
            run.clear()
        }

        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    flush()
                    bold = !bold
                    i += 2
                }
                text[i] == '`' && text.indexOf('`', i + 1) > i -> {
                    flush()
                    val end = text.indexOf('`', i + 1)
                    withStyle(code) { append(text.substring(i + 1, end)) }
                    i = end + 1
                }
                else -> {
                    run.append(text[i])
                    i++
                }
            }
        }
        flush()
    }
    if (terms.isEmpty()) return marked

    // Highlights are matched against the visible text, after the ** and ` marks are gone
    return buildAnnotatedString {
        append(marked)
        HelpSearch.highlights(marked.text, terms).forEach { addStyle(Highlight, it.first, it.last + 1) }
    }
}