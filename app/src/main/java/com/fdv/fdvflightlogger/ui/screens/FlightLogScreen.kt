@file:Suppress("unused", "ASSIGNED_VALUE_IS_NEVER_READ")

package com.fdv.fdvflightlogger.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fdv.fdvflightlogger.data.db.FlightType
import com.fdv.fdvflightlogger.data.prefs.QnhUnit
import com.fdv.fdvflightlogger.data.prefs.TempUnit
import com.fdv.fdvflightlogger.ui.AppViewModel
import com.fdv.fdvflightlogger.ui.mappers.toDraft
import com.fdv.fdvflightlogger.ui.theme.DeltaBlue
import kotlinx.coroutines.delay

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun rememberWindowWidthClass(): WindowWidthSizeClass {
    val activity = LocalContext.current.findActivity()
        ?: error("WindowSizeClass requires an Activity context")

    // calculateWindowSizeClass is @Composable, so call it directly here (NOT inside remember {})
    return calculateWindowSizeClass(activity).widthSizeClass
}
private val FlightDraftSaver: Saver<FlightDraft, Any> = listSaver(
    save = { d ->
        listOf(
            d.id?.toString() ?: "",
            d.dep,
            d.arr,
            d.flightType.name,
            d.depRwy ?: "",
            d.depGate ?: "",
            d.sid ?: "",
            d.cruiseFl ?: "",
            d.depFlaps ?: "",
            d.v2 ?: "",
            d.route ?: "",
            d.depQnh ?: "",

            d.arrRwy ?: "",
            d.arrGate ?: "",
            d.star ?: "",
            d.altn ?: "",
            d.qnh ?: "",
            d.vref ?: "",
            d.arrFlaps ?: "",

            d.flightNumber ?: "",
            d.aircraft ?: "",
            d.fuel ?: "",
            d.pax ?: "",
            d.payload ?: "",
            d.airTime ?: "",
            d.blockTime ?: "",
            d.costIndex ?: "",
            d.reserveFuel ?: "",
            d.zfw ?: "",
            d.crzWind ?: "",
            d.crzOat ?: "",
            d.info ?: "",
            d.initAlt ?: "",
            d.squawk ?: "",
            d.scratchpad ?: ""
        )
    },
    restore = { raw ->
        val v = raw as List<*>

        FlightDraft(
            id = (v[0] as? String)?.takeIf { it.isNotBlank() }?.toLongOrNull(),
            dep = v[1] as String,
            arr = v[2] as String,
            flightType = (v[3] as? String)?.let {
                runCatching { FlightType.valueOf(it) }.getOrDefault(FlightType.ONLINE)
            } ?: FlightType.ONLINE,
            depRwy = (v[3] as? String)?.takeIf { it.isNotBlank() },
            depGate = (v[4] as? String)?.takeIf { it.isNotBlank() },
            sid = (v[5] as? String)?.takeIf { it.isNotBlank() },
            cruiseFl = (v[6] as? String)?.takeIf { it.isNotBlank() },
            depFlaps = (v[7] as? String)?.takeIf { it.isNotBlank() },
            v2 = (v[8] as? String)?.takeIf { it.isNotBlank() },
            route = (v[9] as? String)?.takeIf { it.isNotBlank() },
            depQnh = (v[10] as? String)?.takeIf { it.isNotBlank() },

            arrRwy = (v[11] as? String)?.takeIf { it.isNotBlank() },
            arrGate = (v[12] as? String)?.takeIf { it.isNotBlank() },
            star = (v[13] as? String)?.takeIf { it.isNotBlank() },
            altn = (v[14] as? String)?.takeIf { it.isNotBlank() },
            qnh = (v[15] as? String)?.takeIf { it.isNotBlank() },
            vref = (v[16] as? String)?.takeIf { it.isNotBlank() },
            arrFlaps = (v[17] as? String)?.takeIf { it.isNotBlank() },

            flightNumber = (v[18] as? String)?.takeIf { it.isNotBlank() },
            aircraft = (v[19] as? String)?.takeIf { it.isNotBlank() },
            fuel = (v[20] as? String)?.takeIf { it.isNotBlank() },
            pax = (v[21] as? String)?.takeIf { it.isNotBlank() },
            payload = (v[22] as? String)?.takeIf { it.isNotBlank() },
            airTime = (v[23] as? String)?.takeIf { it.isNotBlank() },
            blockTime = (v[24] as? String)?.takeIf { it.isNotBlank() },
            costIndex = (v[25] as? String)?.takeIf { it.isNotBlank() },
            reserveFuel = (v[26] as? String)?.takeIf { it.isNotBlank() },
            zfw = (v[27] as? String)?.takeIf { it.isNotBlank() },
            crzWind = (v[28] as? String)?.takeIf { it.isNotBlank() },
            crzOat = (v[29] as? String)?.takeIf { it.isNotBlank() },

            info = (v[30] as? String)?.takeIf { it.isNotBlank() },
            initAlt = (v[31] as? String)?.takeIf { it.isNotBlank() },
            squawk = (v[32] as? String)?.takeIf { it.isNotBlank() },

            scratchpad = (v[33] as? String)?.takeIf { it.isNotBlank() }
        )
    }
)

enum class FlightSection {
    DEPARTURE,
    ARRIVAL,
    AIRCRAFT,
    ATC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightLogScreen(
    appViewModel: AppViewModel,
    navController: NavController,
    editFlightId: Long? = null
) {
    val state by appViewModel.state.collectAsStateWithLifecycle()
    val lastLandedDisplay = state.lastLanded.ifBlank { "--" }

    var draft by rememberSaveable(stateSaver = FlightDraftSaver) {
        mutableStateOf(FlightDraft())
    }

    var initialDraft by rememberSaveable(stateSaver = FlightDraftSaver) {
        mutableStateOf(FlightDraft())
    }

    val confirmDiscard = rememberSaveable { mutableStateOf(false) }

    val isDirty = draft.normalizedForDirtyCheck() != initialDraft.normalizedForDirtyCheck()

    val widthClass = rememberWindowWidthClass()

    var activeSection by rememberSaveable { mutableStateOf(FlightSection.DEPARTURE) }

    LaunchedEffect(editFlightId) {
        if (editFlightId != null) {
            val e = appViewModel.getFlightById(editFlightId)
            if (e != null) {
                val loaded = e.toDraft()
                draft = loaded
                initialDraft = loaded
            }
        } else {
            // Creating a new flight: baseline is blank
            val blank = FlightDraft()
            draft = blank
            initialDraft = blank
        }
    }

    BackHandler(enabled = isDirty) {
        confirmDiscard.value = true
    }

    // Auto-save draft every 60 seconds if dirty
    LaunchedEffect(isDirty) {
        while (isDirty) {
            delay(60000)
            if (draft.dep.isNotBlank() && draft.arr.isNotBlank()) {
                appViewModel.saveFlight(draft)

                // If this was a new flight (no ID), get the auto-generated ID
                if (draft.id == null) {
                    val saved = appViewModel.getLatestFlight()
                    if (saved != null) {
                        draft = draft.copy(id = saved.id)
                    }
                }

                initialDraft = draft
            }
        }
    }

    // Save when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            if (isDirty && draft.dep.isNotBlank() && draft.arr.isNotBlank()) {
                appViewModel.saveFlight(draft)
            }
        }
    }

    if (confirmDiscard.value) {
        AlertDialog(
            onDismissRequest = { confirmDiscard.value = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Discard them and leave this screen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDiscard.value = false
                        draft = initialDraft
                        navController.popBackStack()
                    }
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard.value = false }) { Text("Keep editing") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FDV Flight Logger") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeltaBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }

                    IconButton(onClick = { navController.navigate("history") }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Flight History"
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
            IdentityStrip(
                pilotId = state.profile.pilotId,
                pilotName = state.profile.name,
                hub = state.profile.hub,
                lastLanded = lastLandedDisplay
            )

            SectionJumpChips(
                activeSection = activeSection,
                showAtc = draft.flightType != FlightType.OFFLINE_NO_ATC,
                onSectionClick = { section -> activeSection = section }
            )

            Button(
                onClick = {
                    appViewModel.saveFlight(draft)

                    if (editFlightId != null) {
                        initialDraft = draft
                    } else {
                        val blank = FlightDraft()
                        draft = blank
                        initialDraft = blank
                    }
                },
                enabled = draft.isValid(),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Text(if (editFlightId != null) "Update Flight" else "Save Flight")
            }

            when (widthClass) {
                WindowWidthSizeClass.Expanded -> ExpandedWhiteboardLayout(
                    draft = draft,
                    onDraftChange = { draft = it },
                    qnhUnit = state.settings.qnhUnit,
                    tempUnit = state.settings.tempUnit,
                    activeSection = activeSection
                )

                WindowWidthSizeClass.Medium -> MediumTwoColumnLayout(
                    draft = draft,
                    onDraftChange = { draft = it },
                    qnhUnit = state.settings.qnhUnit,
                    tempUnit = state.settings.tempUnit,
                    activeSection = activeSection
                )

                else -> CompactSingleColumnLayout(
                    draft = draft,
                    onDraftChange = { draft = it },
                    qnhUnit = state.settings.qnhUnit,
                    tempUnit = state.settings.tempUnit,
                    activeSection = activeSection
                )
            }
        }
    }
}

/* ------------------------------- Layouts ------------------------------- */

@Composable
private fun CompactSingleColumnLayout(
    draft: FlightDraft,
    onDraftChange: (FlightDraft) -> Unit,
    qnhUnit: QnhUnit,
    tempUnit: TempUnit,
    activeSection: FlightSection
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Scrollable section content
        Column(
            modifier = Modifier
                .weight(1f)  // ← Takes remaining space, allows scroll
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        ) {
            // Always show route header (DEP → ARR)
            RouteHeader(draft, onDraftChange)

            // Show active section only
            when (activeSection) {
                FlightSection.DEPARTURE -> {
                    SectionCard(title = "Departure + Enroute") {
                        DepartureEnrouteFields(draft, onDraftChange, qnhUnit)
                    }
                }

                FlightSection.ARRIVAL -> {
                    SectionCard(title = "Arrival") {
                        ArrivalFields(draft, onDraftChange, qnhUnit)
                    }
                }

                FlightSection.AIRCRAFT -> {
                    SectionCard(title = "Aircraft + Performance") {
                        AircraftPerfFields(draft, onDraftChange, tempUnit)
                    }
                }

                FlightSection.ATC -> {
                    SectionCard(title = "ATC") {
                        AtcFields(
                            info = draft.info,
                            initAlt = draft.initAlt,
                            squawk = draft.squawk,
                            onInfoChange = { onDraftChange(draft.copy(info = it)) },
                            onInitAltChange = { onDraftChange(draft.copy(initAlt = it)) },
                            onSquawkChange = { onDraftChange(draft.copy(squawk = it)) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        SectionCard(
            title = "Scratchpad",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            NotesField(
                value = draft.scratchpad.orEmpty(),
                onChange = { onDraftChange(draft.copy(scratchpad = it.takeIf { s -> s.isNotBlank() })) }
            )
        }
    }
}

@Composable
private fun MediumTwoColumnLayout(
    draft: FlightDraft,
    onDraftChange: (FlightDraft) -> Unit,
    qnhUnit: QnhUnit,
    tempUnit: TempUnit,
    activeSection: FlightSection  // ← ADD PARAMETER
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RouteHeader(draft, onDraftChange)

            // Show active section only
            when (activeSection) {
                FlightSection.DEPARTURE -> {
                    SectionCard(title = "Departure + Enroute") {
                        DepartureEnrouteFields(draft, onDraftChange, qnhUnit)
                    }
                }

                FlightSection.ARRIVAL -> {
                    SectionCard(title = "Arrival") {
                        ArrivalFields(draft, onDraftChange, qnhUnit)
                    }
                }

                FlightSection.AIRCRAFT -> {
                    SectionCard(title = "Aircraft + Performance") {
                        AircraftPerfFields(draft, onDraftChange, tempUnit)
                    }
                }

                FlightSection.ATC -> {
                    SectionCard(title = "ATC") {
                        AtcFields(
                            info = draft.info,
                            initAlt = draft.initAlt,
                            squawk = draft.squawk,
                            onInfoChange = { onDraftChange(draft.copy(info = it)) },
                            onInitAltChange = { onDraftChange(draft.copy(initAlt = it)) },
                            onSquawkChange = { onDraftChange(draft.copy(squawk = it)) }
                        )
                    }
                }
            }
        }

        // Fixed scratchpad at bottom
        SectionCard(
            title = "Scratchpad",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            NotesField(
                value = draft.scratchpad.orEmpty(),
                onChange = { onDraftChange(draft.copy(scratchpad = it.takeIf { s -> s.isNotBlank() })) }
            )
        }
    }
}

@Composable
private fun ExpandedWhiteboardLayout(
    draft: FlightDraft,
    onDraftChange: (FlightDraft) -> Unit,
    qnhUnit: QnhUnit,
    tempUnit: TempUnit,
    activeSection: FlightSection  // ← ADD PARAMETER
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RouteHeader(draft, onDraftChange)

            // Show active section only
            when (activeSection) {
                FlightSection.DEPARTURE -> {
                    SectionCard(title = "Departure + Enroute") {
                        DepartureEnrouteFields(draft, onDraftChange, qnhUnit)
                    }
                }

                FlightSection.ARRIVAL -> {
                    SectionCard(title = "Arrival") {
                        ArrivalFields(draft, onDraftChange, qnhUnit)
                    }
                }

                FlightSection.AIRCRAFT -> {
                    SectionCard(title = "Aircraft + Performance") {
                        AircraftPerfFields(draft, onDraftChange, tempUnit)
                    }
                }

                FlightSection.ATC -> {
                    SectionCard(title = "ATC") {
                        AtcFields(
                            info = draft.info,
                            initAlt = draft.initAlt,
                            squawk = draft.squawk,
                            onInfoChange = { onDraftChange(draft.copy(info = it)) },
                            onInitAltChange = { onDraftChange(draft.copy(initAlt = it)) },
                            onSquawkChange = { onDraftChange(draft.copy(squawk = it)) }
                        )
                    }
                }
            }
        }

        // Fixed scratchpad at bottom
        SectionCard(
            title = "Scratchpad",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            NotesField(
                value = draft.scratchpad.orEmpty(),
                onChange = { onDraftChange(draft.copy(scratchpad = it.takeIf { s -> s.isNotBlank() })) }
            )
        }
    }
}

/* ----------------------------- Sections ----------------------------- */

@Composable
private fun RouteHeader(
    draft: FlightDraft,
    onDraftChange: (FlightDraft) -> Unit
) {
    SectionCard(title = "Route") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            TextFieldSmall(
                label = "DEP",
                value = draft.dep,
                onChange = { onDraftChange(draft.copy(dep = it.uppercase())) },
                modifier = Modifier.weight(1f),
                capitalization = KeyboardCapitalization.Characters
            )
            Text("→", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp))
            TextFieldSmall(
                label = "ARR",
                value = draft.arr,
                onChange = { onDraftChange(draft.copy(arr = it.uppercase())) },
                modifier = Modifier.weight(1f),
                capitalization = KeyboardCapitalization.Characters
            )
        }
    }
}

@Composable
private fun DepartureEnrouteFields(
    draft: FlightDraft,
    onChange: (FlightDraft) -> Unit,
    qnhUnit: QnhUnit
) {
    // Flight Type dropdown (full width at top)
    FlightTypeDropdown(
        flightType = draft.flightType,
        onFlightTypeChange = { onChange(draft.copy(flightType = it)) },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall(
            "RWY",
            draft.depRwy.orEmpty(),
            { onChange(draft.copy(depRwy = it.uppercase().takeIf { s -> s.isNotBlank() })) },
            Modifier.weight(1f),
            capitalization = KeyboardCapitalization.Characters
        )
        TextFieldSmall(
            "Gate",
            draft.depGate.orEmpty(),
            { onChange(draft.copy(depGate = it.uppercase().takeIf { s -> s.isNotBlank() })) },
            Modifier.weight(1f),
            capitalization = KeyboardCapitalization.Characters
        )
        TextFieldSmall(
            "SID",
            draft.sid.orEmpty(),
            { onChange(draft.copy(sid = it.uppercase().takeIf { s -> s.isNotBlank() })) },
            Modifier.weight(1f),
            capitalization = KeyboardCapitalization.Characters
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall(
            "Cruise (FL)",
            draft.cruiseFl.orEmpty(),
            {
                val validated = validateNumeric(it, allowDecimal = false)
                onChange(draft.copy(cruiseFl = validated.takeIf { s -> s.isNotBlank() }))
            },
            Modifier.weight(1f),
            keyboardType = KeyboardType.Number
        )
        TextFieldSmall(
            "Flaps",
            draft.depFlaps.orEmpty(),
            {
                val validated = validateNumeric(it, allowDecimal = false)
                onChange(draft.copy(depFlaps = validated.takeIf { s -> s.isNotBlank() }))
            },
            Modifier.weight(1f),
            keyboardType = KeyboardType.Number
        )
        TextFieldSmall(
            "V2",
            draft.v2.orEmpty(),
            {
                val validated = validateNumeric(it, allowDecimal = false)
                onChange(draft.copy(v2 = validated.takeIf { s -> s.isNotBlank() }))
            },
            Modifier.weight(1f),
            keyboardType = KeyboardType.Number
        )
    }

    RouteTextField(
        value = draft.route.orEmpty(),
        onChange = { onChange(draft.copy(route = it.uppercase().takeIf { s -> s.isNotBlank() })) }
    )

    TextFieldSmall(
        "Dep QNH",
        draft.depQnh.orEmpty(),
        {
            val formatted = formatQnh(it, qnhUnit)
            onChange(draft.copy(depQnh = formatted.takeIf { s -> s.isNotBlank() }))
        },
        Modifier.fillMaxWidth(),
        keyboardType = KeyboardType.Decimal
    )
}

@Composable
private fun ArrivalFields(d: FlightDraft, onChange: (FlightDraft) -> Unit, qnhUnit: QnhUnit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall(
            "RWY",
            d.arrRwy.orEmpty(),
            { onChange(d.copy(arrRwy = it.uppercase().takeIf { s -> s.isNotBlank() })) },  // ← Add .uppercase()
            Modifier.weight(1f),
            capitalization = KeyboardCapitalization.Characters  // ← ADD
        )
        TextFieldSmall(
            "Gate",
            d.arrGate.orEmpty(),
            { onChange(d.copy(arrGate = it.uppercase().takeIf { s -> s.isNotBlank() })) },  // ← Add .uppercase()
            Modifier.weight(1f),
            capitalization = KeyboardCapitalization.Characters  // ← ADD
        )
        TextFieldSmall(
            "STAR",
            d.star.orEmpty(),
            { onChange(d.copy(star = it.uppercase().takeIf { s -> s.isNotBlank() })) },  // ← Add .uppercase()
            Modifier.weight(1f),
            capitalization = KeyboardCapitalization.Characters  // ← ADD
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall(
            "ALTN",
            d.altn.orEmpty(),
            { onChange(d.copy(altn = it.uppercase().takeIf { s -> s.isNotBlank() })) },
            Modifier.weight(1f),
            capitalization = KeyboardCapitalization.Characters
        )
        TextFieldSmall(
            "QNH",
            d.qnh.orEmpty(),
            {
                val formatted = formatQnh(it, qnhUnit)
                onChange(d.copy(qnh = formatted.takeIf { s -> s.isNotBlank() }))
            },
            Modifier.weight(1f),
            keyboardType = KeyboardType.Decimal
        )
        TextFieldSmall(
            "Arr Flaps",
            d.arrFlaps.orEmpty(),
            {
                val validated = validateNumeric(it, allowDecimal = false)
                onChange(d.copy(arrFlaps = validated.takeIf { s -> s.isNotBlank() }))
            },
            Modifier.weight(1f),
            keyboardType = KeyboardType.Number
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall(
            "Vref",
            d.vref.orEmpty(),
            {
                val validated = validateNumeric(it, allowDecimal = false)
                onChange(d.copy(vref = validated.takeIf { s -> s.isNotBlank() }))
            },
            Modifier.weight(1f),
            keyboardType = KeyboardType.Number
        )
        Spacer(Modifier.weight(2f))
    }
}

@Composable
private fun AircraftPerfFields(d: FlightDraft, onChange: (FlightDraft) -> Unit, tempUnit: TempUnit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall(
            "Flight #",
            d.flightNumber.orEmpty(),
            { onChange(d.copy(flightNumber = it.uppercase().takeIf { s -> s.isNotBlank() })) },
            Modifier.weight(1f),
            capitalization = KeyboardCapitalization.Characters
        )
        TextFieldSmall(
            "Aircraft",
            d.aircraft.orEmpty(),
            { onChange(d.copy(aircraft = it.uppercase().takeIf { s -> s.isNotBlank() })) },
            Modifier.weight(1f),
            capitalization = KeyboardCapitalization.Characters
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall(
            "Fuel",
            d.fuel.orEmpty(),
            {
                val validated = validateNumeric(it, allowDecimal = true)
                onChange(d.copy(fuel = validated.takeIf { s -> s.isNotBlank() }))
            },
            Modifier.weight(1f),
            keyboardType = KeyboardType.Decimal
        )
        TextFieldSmall(
            "PAX",
            d.pax.orEmpty(),
            {
                val validated = validateNumeric(it, allowDecimal = false)
                onChange(d.copy(pax = validated.takeIf { s -> s.isNotBlank() }))
            },
            Modifier.weight(1f),
            keyboardType = KeyboardType.Number
        )
        TextFieldSmall(
            "Payload",
            d.payload.orEmpty(),
            {
                val validated = validateNumeric(it, allowDecimal = true)
                onChange(d.copy(payload = validated.takeIf { s -> s.isNotBlank() }))
            },
            Modifier.weight(1f),
            keyboardType = KeyboardType.Decimal
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TimeField(
            "A. Time",
            d.airTime.orEmpty(),
            { onChange(d.copy(airTime = it.takeIf { s -> s.isNotBlank() })) },
            Modifier.weight(1f)
        )
        TimeField(
            "B. Time",
            d.blockTime.orEmpty(),
            { onChange(d.copy(blockTime = it.takeIf { s -> s.isNotBlank() })) },
            Modifier.weight(1f)
        )
        TextFieldSmall(
            "CI",
            d.costIndex.orEmpty(),
            {
                val validated = validateNumeric(it, allowDecimal = false)
                onChange(d.copy(costIndex = validated.takeIf { s -> s.isNotBlank() }))
            },
            Modifier.weight(1f),
            keyboardType = KeyboardType.Number
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall(
            "R. Fuel",
            d.reserveFuel.orEmpty(),
            {
                val validated = validateNumeric(it, allowDecimal = true)
                onChange(d.copy(reserveFuel = validated.takeIf { s -> s.isNotBlank() }))
            },
            Modifier.weight(1f),
            keyboardType = KeyboardType.Decimal
        )
        TextFieldSmall(
            "ZFW",
            d.zfw.orEmpty(),
            {
                val validated = validateNumeric(it, allowDecimal = true)
                onChange(d.copy(zfw = validated.takeIf { s -> s.isNotBlank() }))
            },
            Modifier.weight(1f),
            keyboardType = KeyboardType.Decimal
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        CrzWindField(
            value = d.crzWind.orEmpty(),
            onChange = { onChange(d.copy(crzWind = it.takeIf { s -> s.isNotBlank() })) },
            modifier = Modifier.weight(1f)
        )

        // OAT field with unit label
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextFieldSmall(
                "Crz. OAT",
                d.crzOat.orEmpty(),
                {
                    val formatted = formatTemperature(it)
                    onChange(d.copy(crzOat = formatted.takeIf { s -> s.isNotBlank() }))
                },
                Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
            Text(
                text = when (tempUnit) {
                    TempUnit.F -> "°F"
                    TempUnit.C -> "°C"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)  // Align with text field content
            )
        }
    }
}

/* ----------------------------- Components ----------------------------- */

/**
 * Visual transformation that displays digits as HH:MM without modifying the actual value
 */
class TimeVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitsOnly = text.text.filter { it.isDigit() }.take(4)

        val formatted = when {
            digitsOnly.isEmpty() -> ""
            digitsOnly.length <= 2 -> digitsOnly
            digitsOnly.length == 3 -> "${digitsOnly.take(2)}:${digitsOnly.drop(2)}"
            else -> "${digitsOnly.take(2)}:${digitsOnly.drop(2).take(2)}"
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset <= 2 -> offset
                    else -> offset + 1 // Account for the colon
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 2 -> offset
                    offset == 3 -> 2 // Colon position maps back to position 2
                    else -> offset - 1 // After colon, subtract 1
                }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

/**
 * Visual transformation that displays wind as XXX/XX without modifying the actual value
 * Direction: 3 digits with leading zeros (e.g., 096)
 * Speed: 2-3 digits without leading zeros (e.g., 25, not 025)
 */
class CrzWindVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitsOnly = text.text.filter { it.isDigit() }.take(6) // Max 6 digits (XXX/XXX)

        val formatted = when {
            digitsOnly.isEmpty() -> ""
            digitsOnly.length <= 3 -> digitsOnly
            else -> {
                val direction = digitsOnly.take(3).padStart(3, '0')
                val speed = digitsOnly.drop(3).take(3).trimStart('0').ifEmpty { "0" }
                "$direction/$speed"
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                // Original is just digits, transformed has a slash after position 3
                return when {
                    offset <= 3 -> offset
                    else -> {
                        // After position 3, add 1 for the slash
                        val adjustedOffset = offset + 1
                        // Don't exceed the formatted text length
                        adjustedOffset.coerceAtMost(formatted.length)
                    }
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                // Transformed has a slash at position 3
                return when {
                    offset <= 3 -> offset
                    offset == 4 -> 3 // Slash position maps back to position 3
                    else -> {
                        // After slash, subtract 1
                        val adjustedOffset = offset - 1
                        // Don't exceed the original text length
                        adjustedOffset.coerceAtMost(digitsOnly.length)
                    }
                }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // Only allow digits, max 4 characters
            val digitsOnly = input.filter { it.isDigit() }.take(4)
            onChange(digitsOnly)  // ← Store plain digits, NOT formatted
        },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = TimeVisualTransformation(),  // ← ADD THIS LINE
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.secondary,
            cursorColor = MaterialTheme.colorScheme.secondary,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}

@Composable
private fun CrzWindField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // Only allow digits, max 6 characters (XXX/XXX)
            val digitsOnly = input.filter { it.isDigit() }.take(6)
            onChange(digitsOnly)
        },
        label = { Text("Crz. Wind") },
        singleLine = true,
        visualTransformation = CrzWindVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.secondary,
            cursorColor = MaterialTheme.colorScheme.secondary,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}

@Composable
private fun IdentityStrip(
    pilotId: String,
    pilotName: String,
    hub: String,
    lastLanded: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Pilot: $pilotId • $pilotName",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Hub: $hub • Last landed: $lastLanded",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SectionJumpChips(
    activeSection: FlightSection,
    showAtc: Boolean,
    onSectionClick: (FlightSection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = activeSection == FlightSection.DEPARTURE,
            onClick = { onSectionClick(FlightSection.DEPARTURE) },
            label = { Text("Departure") }
        )

        FilterChip(
            selected = activeSection == FlightSection.ARRIVAL,
            onClick = { onSectionClick(FlightSection.ARRIVAL) },
            label = { Text("Arrival") }
        )

        FilterChip(
            selected = activeSection == FlightSection.AIRCRAFT,
            onClick = { onSectionClick(FlightSection.AIRCRAFT) },
            label = { Text("Aircraft") }
        )

        // Only show ATC chip if flight type requires it
        if (showAtc) {
            FilterChip(
                selected = activeSection == FlightSection.ATC,
                onClick = { onSectionClick(FlightSection.ATC) },
                label = { Text("ATC") }
            )
        }
    }
}


@Composable
private fun RightEdgeFadeWithChevron(modifier: Modifier = Modifier) {
    val bg = MaterialTheme.colorScheme.background
    val fadeWidth = 28.dp

    Box(
        modifier = modifier
            .graphicsLayer { }
            .padding(end = 0.dp)
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .padding(end = 0.dp)
                .align(Alignment.CenterEnd)
                .width(fadeWidth)
                .height(36.dp)
        ) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, bg)
                )
            )
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Scroll for more",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .alpha(0.55f)
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,  // ← ADD THIS PARAMETER
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier  // ← USE THE PARAMETER HERE
            .fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun TextFieldSmall(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,   // red accent
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.secondary,
            cursorColor = MaterialTheme.colorScheme.secondary,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlightTypeDropdown(
    flightType: FlightType,
    onFlightTypeChange: (FlightType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = flightType.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Flight Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            FlightType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName()) },
                    onClick = {
                        onFlightTypeChange(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun RouteTextField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Route") },
        minLines = 2,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.secondary,
            cursorColor = MaterialTheme.colorScheme.secondary,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun NotesField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Notes / Taxi / Clearance") },
        minLines = 4,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.secondary,
            cursorColor = MaterialTheme.colorScheme.secondary,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun AtcStrip(
    info: String?,
    initAlt: String?,
    squawk: String?,
    onInfoChange: (String?) -> Unit,
    onInitAltChange: (String?) -> Unit,
    onSquawkChange: (String?) -> Unit
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextFieldSmall(
                label = "Info",
                value = info.orEmpty(),
                onChange = { onInfoChange(it.takeIf { s -> s.isNotBlank() }) },
                modifier = Modifier.weight(1f),
                capitalization = KeyboardCapitalization.Characters
            )
            TextFieldSmall(
                label = "Init. Alt.",
                value = initAlt.orEmpty(),
                onChange = { onInitAltChange(it.takeIf { s -> s.isNotBlank() }) },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
            TextFieldSmall(
                label = "Sqwk",
                value = squawk.orEmpty(),
                onChange = { onSquawkChange(it.takeIf { s -> s.isNotBlank() }) },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
        }
    }
}

@Composable
private fun AtcFields(
    info: String?,
    initAlt: String?,
    squawk: String?,
    onInfoChange: (String?) -> Unit,
    onInitAltChange: (String?) -> Unit,
    onSquawkChange: (String?) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        TextFieldSmall(
            label = "ATIS Info",
            value = info.orEmpty(),
            onChange = { onInfoChange(it.uppercase().takeIf { s -> s.isNotBlank() }) },
            modifier = Modifier.fillMaxWidth(),
            capitalization = KeyboardCapitalization.Characters
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TextFieldSmall(
                label = "Initial Alt",
                value = initAlt.orEmpty(),
                onChange = {
                    val validated = validateNumeric(it, allowDecimal = false)
                    onInitAltChange(validated.takeIf { s -> s.isNotBlank() })
                },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )

            TextFieldSmall(
                label = "Squawk",
                value = squawk.orEmpty(),
                onChange = {
                    val validated = validateNumeric(it, allowDecimal = false)
                    onSquawkChange(validated.takeIf { s -> s.isNotBlank() })
                },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
        }
    }
}

/* ----------------------------- Helpers ----------------------------- */

private fun FlightDraft.isValid(): Boolean {
    return dep.isNotBlank() &&
            dep.length <= 4 &&
            arr.isNotBlank() &&
            arr.length <= 4
}

private fun FlightDraft.normalizedForDirtyCheck(): FlightDraft = copy(
    route = route?.trimEnd(),
    scratchpad = scratchpad?.trimEnd()
)

/**
 * Validates numeric input (allows digits and optional decimal point)
 */
private fun validateNumeric(input: String, allowDecimal: Boolean = false): String {
    return if (allowDecimal) {
        input.filter { it.isDigit() || it == '.' }
            .let {
                // Ensure only one decimal point
                val parts = it.split('.')
                if (parts.size > 2) parts[0] + "." + parts.drop(1).joinToString("")
                else it
            }
    } else {
        input.filter { it.isDigit() }
    }
}

/**
 * Formats time input as HH:MM
 * Handles cursor position to prevent digit reversal
 */
private fun formatTime(input: String): String {
    // Remove everything except digits
    val digitsOnly = input.filter { it.isDigit() }

    return when {
        digitsOnly.isEmpty() -> ""
        digitsOnly.length == 1 -> digitsOnly
        digitsOnly.length == 2 -> digitsOnly
        digitsOnly.length == 3 -> "${digitsOnly.take(2)}:${digitsOnly.drop(2)}"
        else -> "${digitsOnly.take(2)}:${digitsOnly.drop(2).take(2)}"
    }
}

/**
 * Formats QNH based on unit setting
 * inHg: xx.xx (e.g., 29.92)
 * hPa: xxxx (e.g., 1013)
 */
private fun formatQnh(input: String, unit: QnhUnit): String {
    val digitsOnly = input.filter { it.isDigit() || it == '.' }

    return when (unit) {
        QnhUnit.INHG -> {
            // Format as xx.xx (max 5 chars: 29.92)
            val parts = digitsOnly.split('.')
            val whole = parts[0].take(2)
            val decimal = parts.getOrNull(1)?.take(2) ?: ""

            if (decimal.isEmpty() && whole.isNotEmpty() && digitsOnly.contains('.')) {
                "$whole."
            } else if (decimal.isNotEmpty()) {
                "$whole.$decimal"
            } else {
                whole
            }
        }
        QnhUnit.HPA -> {
            // Format as xxxx (integers only, max 4 digits: 1013)
            digitsOnly.filter { it.isDigit() }.take(4)
        }
    }
}

/**
 * Formats temperature as integer with optional minus sign
 * Rounds decimals (e.g., -15.7 → -16, 15.3 → 15)
 * Examples: -15, 0, 23
 */
private fun formatTemperature(input: String): String {
    // Allow digits, minus sign, and decimal point (for intermediate input)
    val cleaned = input.filter { it.isDigit() || it == '-' || it == '.' }

    // If empty or just a minus sign, return as-is
    if (cleaned.isEmpty() || cleaned == "-") return cleaned

    // Try to parse and round
    return try {
        val value = cleaned.toDouble()
        value.toInt().toString()  // Rounds toward zero by default
    } catch (_: NumberFormatException) {
        cleaned.filter { it.isDigit() || it == '-' }.take(4)
    }
}



