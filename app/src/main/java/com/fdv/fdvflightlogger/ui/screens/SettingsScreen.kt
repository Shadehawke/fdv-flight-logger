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
