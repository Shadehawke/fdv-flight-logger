package com.fdv.fdvflightlogger.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.fdv.fdvflightlogger.data.prefs.AppSettings
import com.fdv.fdvflightlogger.data.prefs.PilotProfile
import com.fdv.fdvflightlogger.data.prefs.QnhUnit
import com.fdv.fdvflightlogger.data.prefs.TempUnit
import com.fdv.fdvflightlogger.data.prefs.ThemeMode
import com.fdv.fdvflightlogger.data.prefs.WeightUnit
import com.fdv.fdvflightlogger.ui.AppViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdv.fdvflightlogger.ui.theme.GradientButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect


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

    val context = LocalContext.current
    val fdvSession by appViewModel.fdvSession.collectAsStateWithLifecycle()
    val fdvPilot by appViewModel.fdvPilot.collectAsStateWithLifecycle()

    // Signing in fills the profile fields from FDV; the pilot can still edit them before finishing
    LaunchedEffect(fdvPilot) {
        fdvPilot?.let { pilot ->
            pilotId = pilot.callsign
            if (pilot.name.isNotBlank()) name = pilot.name
            pilot.hubIcao?.let { hub = it }
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("FlyDeltaVirtual", style = MaterialTheme.typography.titleMedium)

            val session = fdvSession
            if (session == null) {
                Text(
                    "Sign in to fill in your profile automatically, or enter it yourself below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GradientButton(
                    text = "Sign in with FDV",
                    enabled = true,
                    onClick = { appViewModel.startFdvSignIn(context) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = fdvPilot?.let { "Signed in as ${it.callsign}. Your profile is filled in below." }
                        ?: "Signed in as ${session.callsign ?: "FDV pilot"}. Loading your profile…",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider()

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

