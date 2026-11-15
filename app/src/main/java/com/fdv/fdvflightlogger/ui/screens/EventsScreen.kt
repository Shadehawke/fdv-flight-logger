package com.fdv.fdvflightlogger.ui.screens

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fdv.fdvflightlogger.data.fdv.FdvEvent
import com.fdv.fdvflightlogger.ui.AppViewModel
import com.fdv.fdvflightlogger.ui.components.EventBanner
import com.fdv.fdvflightlogger.ui.components.FdvTopBar
import com.fdv.fdvflightlogger.ui.components.LivePill
import com.fdv.fdvflightlogger.ui.components.eventRoute
import com.fdv.fdvflightlogger.ui.components.eventTiming
import com.fdv.fdvflightlogger.ui.components.eventWindow
import com.fdv.fdvflightlogger.ui.components.rememberNow
import com.fdv.fdvflightlogger.ui.theme.raised
import com.fdv.fdvflightlogger.ui.theme.raisedColors
import com.fdv.fdvflightlogger.data.fdv.EventBonuses

// Cards stop growing past this on a tablet, so banners and lines of text stay a readable size
private val MaxCardWidth = 720.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(appViewModel: AppViewModel, navController: NavController) {
    val events by appViewModel.fdvEvents.collectAsStateWithLifecycle()
    val loading by appViewModel.eventsLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val now = rememberNow()

    // Uses the dashboard's copy if it's under 10 minutes old
    LaunchedEffect(Unit) { appViewModel.loadEvents() }

    Scaffold(
        topBar = {
            FdvTopBar(
                title = { Text("Events") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(22.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { appViewModel.loadEvents(force = true) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            val all = events
            val visible = all.orEmpty().filterNot { it.hasEndedAt(now) }
            val live = visible.filter { it.isLiveAt(now) }
            val upcoming = visible.filterNot { it.isLiveAt(now) }

            when {
                all == null && loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                all == null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Events couldn't be loaded.", style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { appViewModel.loadEvents(force = true) }) { Text("Try again") }
                }
                visible.isEmpty() -> Text(
                    text = "No events on the calendar right now.\nCheck back soon.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (live.isNotEmpty()) {
                        item(key = "header-live") { SectionLabel("Happening now") }
                        items(live, key = { it.id }) { event ->
                            EventCard(event, now, onOpenPage = { openPage(context, it) })
                        }
                    }
                    if (upcoming.isNotEmpty()) {
                        item(key = "header-upcoming") { SectionLabel("Upcoming") }
                        items(upcoming, key = { it.id }) { event ->
                            EventCard(event, now, onOpenPage = { openPage(context, it) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.widthIn(max = MaxCardWidth).fillMaxWidth().padding(top = 4.dp)
    )
}

@Composable
private fun EventCard(event: FdvEvent, now: Long, onOpenPage: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val live = event.isLiveAt(now)
    // Saveable and keyed by id: an expanded description stays open across rotation and scrolling
    var expanded by rememberSaveable(event.id) { mutableStateOf(false) }
    // Only offer "Show more" when the text really is cut off
    var overflows by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .widthIn(max = MaxCardWidth)
            .fillMaxWidth()
            .raised(raisedColors())
            .clip(RoundedCornerShape(18.dp))
    ) {
        Box {
            EventBanner(imageUrl = event.imageUrl, modifier = Modifier.fillMaxWidth().aspectRatio(3f))
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (live) LivePill(onImage = true)
                event.category?.let { CategoryTag(it) }
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(event.name, style = MaterialTheme.typography.titleLarge)
            eventRoute(event)?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = cs.secondary) }
            if (EventBonuses.qualifies(event)) {
                Text(
                    text = "CAT Free · double hours",
                    style = MaterialTheme.typography.labelLarge,
                    color = cs.primary
                )
            }
            Text(eventTiming(event, now), style = MaterialTheme.typography.bodyMedium)
            Text(eventWindow(event), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            PilotsLine(event)

            if (event.description.isNotEmpty()) {
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result -> if (!expanded) overflows = result.hasVisualOverflow },
                    modifier = Modifier.animateContentSize()
                )
                if (overflows || expanded) {
                    Text(
                        text = if (expanded) "Show less" else "Show more",
                        style = MaterialTheme.typography.labelLarge,
                        color = cs.secondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { expanded = !expanded }
                            .padding(vertical = 2.dp)
                    )
                }
            }

            event.url?.let { url ->
                OutlinedButton(onClick = { onOpenPage(url) }, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        // Signing up happens on the website; the app's API is read-only
                        text = if (event.registered) "  View on FDV" else "  Register on FDV"
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTag(category: String) {
    Text(
        text = category,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
private fun PilotsLine(event: FdvEvent) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Filled.Groups, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(
            text = event.maxPilots?.let { "${event.registeredPilots} of $it pilots" }
                ?: "${event.registeredPilots} pilots registered",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant
        )
        if (event.registered) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = cs.primary, modifier = Modifier.size(16.dp))
            Text("You're registered", style = MaterialTheme.typography.bodySmall, color = cs.primary)
        }
    }
}

private fun openPage(context: Context, url: String) {
    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
}