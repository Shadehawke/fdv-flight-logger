package com.fdv.fdvflightlogger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailScreen(
    appViewModel: AppViewModel,
    navController: NavController,
    flightId: Long
) {
    var flight by remember { mutableStateOf<FlightLogEntity?>(null) }
    var loading by remember { mutableStateOf(true) }
    var notFound by remember { mutableStateOf(false) }

    val confirmDelete = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(flightId) {
        loading = true
        notFound = false
        flight = appViewModel.getFlightById(flightId)
        loading = false
        notFound = (flight == null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flight Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        enabled = !loading && flight != null,
                        onClick = { navController.navigate("log/$flightId") }
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }

                    IconButton(
                        enabled = !loading && flight != null,
                        onClick = { confirmDelete.value = true }
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            notFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text("Flight not found.") }
            }

            else -> {
                FlightDetailContent(
                    flight = requireNotNull(flight),
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }

    // Dialog as sibling to avoid any "assigned but never read" false positives.
    if (confirmDelete.value) {
        AlertDialog(
            onDismissRequest = { confirmDelete.value = false },
            title = { Text("Delete flight?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete.value = false
                        flight?.let { appViewModel.deleteFlight(it) }
                        navController.popBackStack()
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                Button(onClick = { confirmDelete.value = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FlightDetailContent(
    flight: FlightLogEntity,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("${flight.dep} → ${flight.arr}", style = MaterialTheme.typography.titleLarge)

                val meta = listOfNotNull(
                    flight.flightNumber?.takeIf { it.isNotBlank() },  // ← Added ?.
                    flight.aircraft?.takeIf { it.isNotBlank() }       // ← Added ?.
                ).joinToString(" • ")

                if (meta.isNotBlank()) {
                    Text(meta, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        SectionCard("Departure + Enroute") {
            ReadRow("RWY", flight.depRwy.orEmpty(), "Gate", flight.depGate.orEmpty(), "SID", flight.sid.orEmpty())
            ReadRow("Cruise (FL)", flight.cruiseFl.orEmpty(), "Flaps", flight.depFlaps.orEmpty(), "V2", flight.v2.orEmpty())
            ReadBlock("Route", flight.route.orEmpty())
            ReadRow("Dep QNH", flight.depQnh.orEmpty(), "", "", "", "")
        }

        SectionCard("Arrival") {
            ReadRow("RWY", flight.arrRwy.orEmpty(), "Gate", flight.arrGate.orEmpty(), "STAR", flight.star.orEmpty())
            ReadRow("ALTN", flight.altn.orEmpty(), "QNH", flight.qnh.orEmpty(), "Arr Flaps", flight.arrFlaps.orEmpty())
            ReadRow("Vref", flight.vref.orEmpty(), "", "", "", "")
        }

        SectionCard("Aircraft + Performance") {
            ReadRow("Fuel", flight.fuel.orEmpty(), "PAX", flight.pax.orEmpty(), "Payload", flight.payload.orEmpty())
            ReadRow("B. Time", flight.blockTime.orEmpty(), "A. Time", flight.airTime.orEmpty(), "CI", flight.costIndex.orEmpty())
            ReadRow("R. Fuel", flight.reserveFuel.orEmpty(), "ZFW", flight.zfw.orEmpty(), "", "")
            ReadRow("Crz. Wind", flight.crzWind.orEmpty(), "Crz. OAT", flight.crzOat.orEmpty(), "", "")
        }

        SectionCard("ATC") {
            ReadRow("Info", flight.info.orEmpty(), "Init. Alt.", flight.initAlt.orEmpty(), "Sqwk", flight.squawk.orEmpty())
        }

        SectionCard("Notes") {
            ReadBlock("Scratchpad", flight.scratchpad.orEmpty())
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ReadRow(
    l1: String, v1: String,
    l2: String, v2: String,
    l3: String, v3: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ReadField(l1, v1, Modifier.weight(1f))
        ReadField(l2, v2, Modifier.weight(1f))
        if (l3.isNotBlank()) {
            ReadField(l3, v3, Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReadField(label: String, value: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun ReadBlock(label: String, value: String) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
}
