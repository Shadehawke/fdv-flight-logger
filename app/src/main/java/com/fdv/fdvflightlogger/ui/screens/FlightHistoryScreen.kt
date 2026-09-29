package com.fdv.fdvflightlogger.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fdv.fdvflightlogger.data.Airlines
import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.ui.AppViewModel
import com.fdv.fdvflightlogger.ui.UiEvent
import com.fdv.fdvflightlogger.ui.theme.DeltaBlue
import com.fdv.fdvflightlogger.ui.theme.FieldShape
import com.fdv.fdvflightlogger.ui.theme.fdvFieldColors
import com.fdv.fdvflightlogger.ui.theme.inset
import com.fdv.fdvflightlogger.ui.theme.raised
import com.fdv.fdvflightlogger.ui.theme.raisedColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class SortMode { NEWEST_FIRST, OLDEST_FIRST }

private val CardShapeRadius = 16.dp
private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightHistoryScreen(
    appViewModel: AppViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val flights by appViewModel.observeFlights().collectAsStateWithLifecycle(initialValue = emptyList())

    val menuOpen = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Search + sort are UI state, and should survive rotation/process recreation when possible
    var query by rememberSaveable { mutableStateOf("") }
    val sortMode = rememberSaveable { mutableStateOf(SortMode.NEWEST_FIRST) }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            if (uri != null) appViewModel.exportAllFlightsToCsv(context, uri)
        }
    )

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null) appViewModel.exportAllFlightsToPdf(context, uri)
        }
    )

    LaunchedEffect(Unit) {
        appViewModel.events.collect { event ->
            when (event) {
                is UiEvent.ExportSuccess -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Exported ${event.fileName}",
                        actionLabel = "Share",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = event.mimeType
                            putExtra(Intent.EXTRA_STREAM, event.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(share, "Share ${event.fileName}")
                        )
                    }
                }
                is UiEvent.ExportError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }
                is UiEvent.Message -> {
                    snackbarHostState.showSnackbar(event.text, duration = SnackbarDuration.Short)
                }
            }
        }
    }

    // Filter + sort (kept simple and fast)
    val normalizedQuery = query.trim().lowercase()

    val filteredFlights = flights
        .asSequence()
        .filter { f ->
            if (normalizedQuery.isBlank()) true else f.matches(normalizedQuery)
        }
        .toList()
        .let { list ->
            when (sortMode.value) {
                SortMode.NEWEST_FIRST -> list.sortedByDescending { it.createdAtEpochMs }
                SortMode.OLDEST_FIRST -> list.sortedBy { it.createdAtEpochMs }
            }
        }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Flight History") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeltaBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        sortMode.value = when (sortMode.value) {
                            SortMode.NEWEST_FIRST -> SortMode.OLDEST_FIRST
                            SortMode.OLDEST_FIRST -> SortMode.NEWEST_FIRST
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Toggle sort"
                        )
                    }

                    IconButton(onClick = { menuOpen.value = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }

                    DropdownMenu(
                        expanded = menuOpen.value,
                        onDismissRequest = { menuOpen.value = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export CSV") },
                            onClick = {
                                menuOpen.value = false
                                createCsvLauncher.launch("fdv_flights.csv")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export PDF") },
                            onClick = {
                                menuOpen.value = false
                                createPdfLauncher.launch("fdv_flights.pdf")
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = {
                    Text(
                        "Search DEP, ARR, flight #, aircraft, route, notes",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                shape = FieldShape,
                colors = fdvFieldColors(),
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .inset(raisedColors())
            )

            val sortLabel = when (sortMode.value) {
                SortMode.NEWEST_FIRST -> "Newest first"
                SortMode.OLDEST_FIRST -> "Oldest first"
            }

            Text(
                text = "${filteredFlights.size} flights • $sortLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Generous padding so the raised shadows aren't clipped at the list edges
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (filteredFlights.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (query.isNotBlank()) {
                                    "No flights match \"$query\""
                                } else {
                                    "No flights logged yet.\nCreate your first flight!"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(filteredFlights, key = { it.id }) { f ->
                        FlightHistoryCard(
                            f = f,
                            onClick = { navController.navigate("detail/${f.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlightHistoryCard(
    f: FlightLogEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .raised(raisedColors(), cornerRadius = CardShapeRadius)
            // Clip after raised so the ripple is rounded but the shadow isn't cut off
            .clip(RoundedCornerShape(CardShapeRadius))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${f.dep} → ${f.arr}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatDate(f.createdAtEpochMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Same formatter as log/detail/PDF, so Delta Connection shows the DAL number
            val info = Airlines.formatFlightInfo(f.airline, f.flightNumber, f.aircraft)
            if (info.isNotEmpty()) {
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val summary = buildList {
                if (!f.zfw.isNullOrBlank()) add("ZFW ${f.zfw}")
                if (!f.fuel.isNullOrBlank()) add("Fuel ${f.fuel}")
                if (!f.pax.isNullOrBlank()) add("PAX ${f.pax}")
                if (!f.blockTime.isNullOrBlank()) add("Block ${formatStoredTime(f.blockTime)}")
            }.joinToString(" • ")

            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * Centralized matching logic so it's easy to evolve later (chips, advanced filters, etc.)
 */
private fun FlightLogEntity.matches(q: String): Boolean {
    fun String?.m(): Boolean = !this.isNullOrBlank() && this.lowercase().contains(q)

    return dep.m() ||
            arr.m() ||
            airline.m() ||
            flightNumber.m() ||
            // Matches "DAL1234" even when the flight was logged under a Connection carrier
            Airlines.formatFlightInfo(airline, flightNumber, aircraft).m() ||
            aircraft.m() ||
            route.m() ||
            scratchpad.m() ||
            sid.m() ||
            star.m() ||
            altn.m()
}

private fun formatDate(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(DateFormatter)

/**
 * Formats stored time digits (e.g., "0345") as HH:MM (e.g., "03:45")
 */
private fun formatStoredTime(digits: String?): String {
    if (digits.isNullOrBlank()) return ""
    val d = digits.filter { it.isDigit() }.take(4)
    return when {
        d.length <= 2 -> d
        d.length == 3 -> "${d.take(2)}:${d.drop(2)}"
        else -> "${d.take(2)}:${d.drop(2).take(2)}"
    }
}