package com.fdv.fdvflightlogger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fdv.fdvflightlogger.data.prefs.QnhUnit
import com.fdv.fdvflightlogger.data.prefs.TempUnit
import com.fdv.fdvflightlogger.data.prefs.ThemeMode
import com.fdv.fdvflightlogger.data.prefs.WeightUnit
import com.fdv.fdvflightlogger.ui.AppViewModel
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import com.fdv.fdvflightlogger.ui.components.FdvAccountCard
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.fdv.fdvflightlogger.data.prefs.FdvImportMode
import com.fdv.fdvflightlogger.data.sync.ImportPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appViewModel: AppViewModel,
    navController: NavController
) {
    val state by appViewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val context = LocalContext.current
            val fdvSession by appViewModel.fdvSession.collectAsStateWithLifecycle()
            val fdvPilot by appViewModel.fdvPilot.collectAsStateWithLifecycle()
            val fdvRefreshing by appViewModel.fdvRefreshing.collectAsStateWithLifecycle()
            val fdvSync by appViewModel.fdvSync.collectAsStateWithLifecycle()
            val historyProgress by appViewModel.historyProgress.collectAsStateWithLifecycle()
            val airTimeFill by appViewModel.airTimeFill.collectAsStateWithLifecycle()

            // The previewed import waiting for the pilot's choice; null hides the dialog
            var importPlan by remember { mutableStateOf<ImportPlan?>(null) }
            val confirmRemoveImported = rememberSaveable { mutableStateOf(false) }

            // After an app restart the session is stored but the profile isn't; fetch it once here
            LaunchedEffect(fdvSession) {
                if (fdvSession != null && fdvPilot == null) appViewModel.refreshFdvProfile(quiet = true)
            }

            FdvAccountCard(
                airTimeFill = airTimeFill,
                session = fdvSession,
                pilot = fdvPilot,
                refreshing = fdvRefreshing,
                syncState = fdvSync,
                historyProgress = historyProgress,
                onSignIn = { appViewModel.startFdvSignIn(context) },
                onRefresh = { appViewModel.refreshFdvProfile() },
                onSignOut = { appViewModel.signOutOfFdv() },
                onImportHistory = { appViewModel.previewHistoryImport { importPlan = it } },
                onSyncNew = { appViewModel.syncNewPireps() },
                onRemoveImported = { confirmRemoveImported.value = true },
                onFillAirTimes = { appViewModel.startAirTimeFill() },
                onCancelAirTimes = { appViewModel.cancelAirTimeFill() }
            )

            importPlan?.let { plan ->
                AlertDialog(
                    onDismissRequest = { importPlan = null },
                    title = { Text("Import your FDV history?") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${plan.total} PIREPs found on FDV.")
                            Text("${plan.matched.size} match flights in your logbook and will get their actuals.")
                            if (plan.alreadyLinked > 0) Text("${plan.alreadyLinked} are already synced.")
                            Text("${plan.unmatched.size} have no matching flight.")
                            Text(
                                "Import all adds those as new flights. Only my flights leaves them out. " +
                                        "Either way, new PIREPs follow the same choice when you sync.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            importPlan = null
                            appViewModel.applyHistoryImport(plan, FdvImportMode.ALL)
                        }) { Text("Import all") }
                    },
                    dismissButton = {
                        Row {
                            TextButton(onClick = { importPlan = null }) { Text("Cancel") }
                            TextButton(onClick = {
                                importPlan = null
                                appViewModel.applyHistoryImport(plan, FdvImportMode.MATCH_ONLY)
                            }) { Text("Only my flights") }
                        }
                    }
                )
            }

            if (confirmRemoveImported.value) {
                AlertDialog(
                    onDismissRequest = { confirmRemoveImported.value = false },
                    title = { Text("Remove imported flights?") },
                    text = {
                        Text(
                            "This deletes every flight the FDV import created. Flights you logged yourself stay, " +
                                    "including actuals synced onto them. You can import again afterwards."
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmRemoveImported.value = false
                            appViewModel.removeImportedFlights()
                        }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmRemoveImported.value = false }) { Text("Keep") }
                    }
                )
            }

            // Local text wins once the user starts typing, so DataStore's async
            // round-trip can't overwrite characters mid-edit
            var aliasText by rememberSaveable { mutableStateOf<String?>(null) }

            OutlinedTextField(
                value = aliasText ?: settings.simbriefAlias,
                onValueChange = { input ->
                    aliasText = input
                    appViewModel.updateSettings { it.copy(simbriefAlias = input) }
                },
                label = { Text("SimBrief / Navigraph Alias") },
                supportingText = {
                    Text("Found in SimBrief → Account Settings. Only used when you tap Import.")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Keep Screen On",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Prevents screen from sleeping while logging flights",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.keepScreenOn,
                    onCheckedChange = { enabled ->
                        appViewModel.updateSettings { it.copy(keepScreenOn = enabled) }
                    }
                )
            }

            EnumDropdown(
                label = "Theme",
                value = settings.themeMode,
                values = ThemeMode.entries.toList(),
                onChange = { mode ->
                    appViewModel.updateSettings { it.copy(themeMode = mode) }
                }
            )

            EnumDropdown(
                label = "Weight Units (Payload/ZFW)",
                value = settings.weightUnit,
                values = WeightUnit.entries.toList(),
                onChange = { unit ->
                    appViewModel.updateSettings { it.copy(weightUnit = unit) }
                }
            )

            EnumDropdown(
                label = "Fuel Units (Fuel/Reserve)",
                value = settings.fuelUnit,
                values = WeightUnit.entries.toList(),
                onChange = { unit ->
                    appViewModel.updateSettings { it.copy(fuelUnit = unit) }
                }
            )

            EnumDropdown(
                label = "QNH Units",
                value = settings.qnhUnit,
                values = QnhUnit.entries.toList(),
                onChange = { unit ->
                    appViewModel.updateSettings { it.copy(qnhUnit = unit) }
                }
            )

            EnumDropdown(
                label = "Cruise OAT Units",
                value = settings.tempUnit,
                values = TempUnit.entries.toList(),
                onChange = { unit ->
                    appViewModel.updateSettings { it.copy(tempUnit = unit) }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
@Composable
private fun <T : Enum<T>> EnumDropdown(
    label: String,
    value: T,
    values: List<T>,
    onChange: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            values.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        onChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
