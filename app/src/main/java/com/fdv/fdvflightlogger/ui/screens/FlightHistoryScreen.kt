package com.fdv.fdvflightlogger.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.ui.AppViewModel
import com.fdv.fdvflightlogger.ui.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightHistoryScreen(
    appViewModel: AppViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val flights by appViewModel.observeFlights().collectAsStateWithLifecycle(initialValue = emptyList())

    val menuOpen = remember { mutableStateOf(false) }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            if (uri != null) {
                appViewModel.exportAllFlightsToCsv(context, uri)
            }
        }
    )

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null) {
                appViewModel.exportAllFlightsToPdf(context, uri)
            }
        }
    )

    val snackbarHostState = remember { SnackbarHostState() }

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
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Flight History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(flights) { f ->
                FlightHistoryCard(
                    f = f,
                    onClick = { navController.navigate("detail/${f.id}") }
                )
            }
        }
    }
}

@Composable
private fun FlightHistoryCard(
    f: FlightLogEntity,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${f.dep} → ${f.arr}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Open",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (f.flightNumber.isNotBlank()) {
                Text(
                    text = f.flightNumber,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (f.aircraft.isNotBlank()) {
                Text(
                    text = f.aircraft,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val summary = buildString {
                if (f.zfw.isNotBlank()) append("ZFW: ${f.zfw}  ")
                if (f.fuel.isNotBlank()) append("Fuel: ${f.fuel}  ")
                if (f.pax.isNotBlank()) append("PAX: ${f.pax}")
            }.trim()

            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
