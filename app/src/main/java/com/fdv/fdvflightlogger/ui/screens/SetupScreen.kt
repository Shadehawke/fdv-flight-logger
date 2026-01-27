package com.fdv.fdvflightlogger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.fdv.fdvflightlogger.data.prefs.*
import com.fdv.fdvflightlogger.ui.AppViewModel

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.ExposedDropdownMenuAnchorType


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    appViewModel: AppViewModel,
    onSetupComplete: () -> Unit
) {
    var pilotId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var hub by remember { mutableStateOf("") }

    var themeMode by remember { mutableStateOf(ThemeMode.FDV) }
    var weightUnit by remember { mutableStateOf(WeightUnit.LB) }
    var fuelUnit by remember { mutableStateOf(WeightUnit.LB) }
    var qnhUnit by remember { mutableStateOf(QnhUnit.INHG) }
    var tempUnit by remember { mutableStateOf(TempUnit.F) }

    val canContinue = pilotId.isNotBlank() && name.isNotBlank() && hub.isNotBlank()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Initial Setup") }) }
    ) { padding ->

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),

        ) {
            Text("Pilot Profile", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = pilotId,
                onValueChange = { pilotId = it },
                label = { Text("Pilot ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = hub,
                onValueChange = { hub = it },
                label = { Text("Hub (ICAO/IATA)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            Text("Preferences", style = MaterialTheme.typography.titleMedium)

            EnumDropdown("Theme", themeMode, ThemeMode.entries.toList()) { themeMode = it }
            EnumDropdown("Weight Units (Payload/ZFW)", weightUnit, WeightUnit.entries.toList()) { weightUnit = it }
            EnumDropdown("Fuel Units (Fuel/Reserve)", fuelUnit, WeightUnit.entries.toList()) { fuelUnit = it }
            EnumDropdown("QNH Units", qnhUnit, QnhUnit.entries.toList()) { qnhUnit = it }
            EnumDropdown("Cruise OAT Units", tempUnit, TempUnit.entries.toList()) { tempUnit = it }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    appViewModel.saveSetup(
                        profile = PilotProfile(pilotId = pilotId, name = name, hub = hub),
                        settings = AppSettings(
                            themeMode = themeMode,
                            weightUnit = weightUnit,
                            fuelUnit = fuelUnit,
                            qnhUnit = qnhUnit,
                            tempUnit = tempUnit
                        )
                    )
                    onSetupComplete()
                },
                enabled = canContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Finish Setup")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

