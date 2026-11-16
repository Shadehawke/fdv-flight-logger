package com.fdv.fdvflightlogger.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fdv.fdvflightlogger.data.help.HelpParser
import com.fdv.fdvflightlogger.data.help.HelpRepository
import com.fdv.fdvflightlogger.data.help.HelpResult
import com.fdv.fdvflightlogger.data.help.HelpSearch
import com.fdv.fdvflightlogger.data.help.HelpSection
import com.fdv.fdvflightlogger.data.help.HelpTopic
import com.fdv.fdvflightlogger.ui.components.FdvTopBar
import com.fdv.fdvflightlogger.ui.components.HelpBody
import com.fdv.fdvflightlogger.ui.components.highlighted
import com.fdv.fdvflightlogger.ui.nav.Routes
import com.fdv.fdvflightlogger.ui.theme.raised
import com.fdv.fdvflightlogger.ui.theme.raisedColors

// Help reads best as a column; on a tablet it stays this wide, centred
private val MaxWidth = 720.dp

/** The help, loaded once from the APK. Null for the moment it takes to read. */
@Composable
private fun rememberHelpSections(): List<HelpSection>? {
    val context = LocalContext.current
    val sections by produceState<List<HelpSection>?>(initialValue = null) {
        value = HelpRepository.sections(context)
    }
    return sections
}

@Composable
private fun HelpScaffold(title: String, navController: NavController, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            FdvTopBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = content
    )
}

// ---------------- Home: sections, or search results ----------------

@Composable
fun HelpScreen(navController: NavController) {
    val sections = rememberHelpSections()
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(sections, query) { sections?.let { HelpSearch.search(it, query) }.orEmpty() }

    HelpScaffold("Help", navController) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "search") { HelpSearchField(query = query, onQueryChange = { query = it }) }

            when {
                sections == null -> item(key = "loading") {
                    CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                }
                query.isBlank() -> items(sections, key = { it.id }) { section ->
                    SectionCard(section, onClick = { navController.navigate(Routes.helpSection(section.id)) })
                }
                results.isEmpty() -> item(key = "none") {
                    Text(
                        text = "Nothing in the help matches “${query.trim()}”. Try fewer or different words.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.widthIn(max = MaxWidth).fillMaxWidth().padding(top = 16.dp)
                    )
                }
                else -> {
                    item(key = "count") {
                        Text(
                            text = if (results.size == 1) "1 topic" else "${results.size} topics",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.widthIn(max = MaxWidth).fillMaxWidth()
                        )
                    }
                    items(results, key = { "${it.section.id}/${it.topic.id}" }) { result ->
                        ResultCard(result, onClick = {
                            navController.navigate(Routes.helpTopic(result.section.id, result.topic.id, query))
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpSearchField(query: String, onQueryChange: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search the help") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        // Results update as you type; the keyboard's search key just puts the keyboard away
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        modifier = Modifier.widthIn(max = MaxWidth).fillMaxWidth()
    )
}

@Composable
private fun SectionCard(section: HelpSection, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .widthIn(max = MaxWidth)
            .fillMaxWidth()
            .raised(raisedColors())
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(sectionIcon(section.id), contentDescription = null, tint = cs.primary, modifier = Modifier.size(28.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(section.title, style = MaterialTheme.typography.titleMedium)
            if (section.intro.isNotEmpty()) {
                Text(
                    text = section.intro,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text("${section.topics.size}", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = cs.onSurfaceVariant)
    }
}

@Composable
private fun ResultCard(result: HelpResult, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .widthIn(max = MaxWidth)
            .fillMaxWidth()
            .raised(raisedColors())
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(result.section.title, style = MaterialTheme.typography.labelMedium, color = cs.secondary)
        Text(result.topic.title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = highlighted(result.snippet, result.highlights),
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// A help file added later without an icon here gets the general help icon
private fun sectionIcon(id: String): ImageVector = when (id) {
    "getting-started" -> Icons.Filled.PlayCircle
    "dashboard" -> Icons.Filled.Dashboard
    "logging" -> Icons.Filled.EditNote
    "fdv-sync" -> Icons.Filled.CloudSync
    "history" -> Icons.Filled.History
    "backup-export" -> Icons.Filled.Backup
    "trophies-events" -> Icons.Filled.EmojiEvents
    "settings" -> Icons.Filled.Settings
    "troubleshooting" -> Icons.Filled.Build
    else -> Icons.AutoMirrored.Filled.Help
}

// ---------------- One section: its topics ----------------

@Composable
fun HelpSectionScreen(navController: NavController, sectionId: String) {
    val sections = rememberHelpSections()
    val section = sections?.firstOrNull { it.id == sectionId }

    HelpScaffold(section?.title ?: "Help", navController) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                sections == null -> item(key = "loading") { CircularProgressIndicator(modifier = Modifier.padding(32.dp)) }
                section == null -> item(key = "missing") { Text("This help section isn't available.") }
                else -> {
                    if (section.intro.isNotEmpty()) {
                        item(key = "intro") {
                            Text(
                                text = section.intro,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.widthIn(max = MaxWidth).fillMaxWidth()
                            )
                        }
                    }
                    items(section.topics, key = { it.id }) { topic ->
                        TopicRow(topic, onClick = { navController.navigate(Routes.helpTopic(section.id, topic.id)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicRow(topic: HelpTopic, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val preview = remember(topic) { HelpParser.plainText(topic.body) }
    Row(
        modifier = Modifier
            .widthIn(max = MaxWidth)
            .fillMaxWidth()
            .raised(raisedColors())
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(topic.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = cs.onSurfaceVariant)
    }
}

// ---------------- One topic ----------------

@Composable
fun HelpTopicScreen(navController: NavController, sectionId: String, topicId: String, query: String) {
    val sections = rememberHelpSections()
    // Every topic in reading order, for Previous and Next
    val all = remember(sections) { sections.orEmpty().flatMap { s -> s.topics.map { t -> s to t } } }
    val index = all.indexOfFirst { (s, t) -> s.id == sectionId && t.id == topicId }
    val current = all.getOrNull(index)
    val terms = remember(query) { HelpSearch.terms(query) }

    HelpScaffold(current?.first?.title ?: "Help", navController) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = MaxWidth).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    sections == null -> CircularProgressIndicator()
                    current == null -> Text("This help topic isn't available.")
                    else -> {
                        val topic = current.second
                        Text(topic.title, style = MaterialTheme.typography.headlineSmall)
                        HelpBody(markdown = topic.body, terms = terms)

                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth()) {
                            all.getOrNull(index - 1)?.let { (s, t) ->
                                TextButton(
                                    onClick = { openTopic(navController, s.id, t.id) },
                                    modifier = Modifier.weight(1f, fill = false)
                                ) { Text("‹ ${t.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                            Row(modifier = Modifier.weight(1f)) {}    // pushes Next to the right edge
                            all.getOrNull(index + 1)?.let { (s, t) ->
                                TextButton(
                                    onClick = { openTopic(navController, s.id, t.id) },
                                    modifier = Modifier.weight(1f, fill = false)
                                ) { Text("${t.title} ›", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Swaps one topic for another, so Back returns to the list or the search rather than through every topic read
private fun openTopic(navController: NavController, sectionId: String, topicId: String) {
    navController.navigate(Routes.helpTopic(sectionId, topicId)) {
        popUpTo(Routes.HELP_TOPIC) { inclusive = true }
    }
}