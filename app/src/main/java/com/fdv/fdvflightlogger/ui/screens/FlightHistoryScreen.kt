package com.fdv.fdvflightlogger.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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

    Scaffold(
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
                FlightHistoryCard(f)
            }
        }
    }
}

@Composable
private fun FlightHistoryCard(f: FlightLogEntity) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${f.dep} → ${f.arr}",
                style = MaterialTheme.typography.titleMedium
            )

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


