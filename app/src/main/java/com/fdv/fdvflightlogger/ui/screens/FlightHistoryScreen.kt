package com.fdv.fdvflightlogger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.ui.AppViewModel
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightHistoryScreen(
    appViewModel: AppViewModel,
    navController: NavController
) {
    val flightsFlow: Flow<List<FlightLogEntity>> = appViewModel.observeFlights()
    val flights by flightsFlow.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flight History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${f.dep} → ${f.arr}", style = MaterialTheme.typography.titleMedium)
                        if (f.flightNumber.isNotBlank()) Text(f.flightNumber, style = MaterialTheme.typography.bodySmall)
                        if (f.aircraft.isNotBlank()) Text(f.aircraft, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
