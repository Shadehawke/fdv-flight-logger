@file:Suppress("unused", "ASSIGNED_VALUE_IS_NEVER_READ", "SameParameterValue")

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fdv.fdvflightlogger.data.Airlines
import com.fdv.fdvflightlogger.data.aircraft.AircraftTypes
import com.fdv.fdvflightlogger.data.airports.AirportRepository
import com.fdv.fdvflightlogger.data.db.FlightType
import com.fdv.fdvflightlogger.data.prefs.QnhUnit
import com.fdv.fdvflightlogger.data.prefs.TempUnit
import com.fdv.fdvflightlogger.ui.AppViewModel
import com.fdv.fdvflightlogger.ui.mappers.toDraft
import com.fdv.fdvflightlogger.ui.theme.DeltaBlue
import com.fdv.fdvflightlogger.ui.theme.FieldShape
import com.fdv.fdvflightlogger.ui.theme.GradientButton
import com.fdv.fdvflightlogger.ui.theme.SectionChip
import com.fdv.fdvflightlogger.ui.theme.fdvFieldColors
import com.fdv.fdvflightlogger.ui.theme.inset
import com.fdv.fdvflightlogger.ui.theme.raised
import com.fdv.fdvflightlogger.ui.theme.raisedColors
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

// Save and restore order MUST match index-for-index, or fields shift on rotation/process death.
private val FlightDraftSaver: Saver<FlightDraft, Any> = listSaver(
    save = { d ->
        listOf(
            d.id?.toString() ?: "",   // 0
            d.dep,                    // 1
            d.arr,                    // 2
            d.flightType.name,        // 3
            d.depRwy ?: "",           // 4
            d.depGate ?: "",          // 5
            d.sid ?: "",              // 6
            d.cruiseFl ?: "",         // 7
            d.depFlaps ?: "",         // 8
            d.v2 ?: "",               // 9
            d.route ?: "",            // 10
            d.depQnh ?: "",           // 11

            d.arrRwy ?: "",           // 12
            d.arrGate ?: "",          // 13
            d.star ?: "",             // 14
            d.altn ?: "",             // 15
            d.qnh ?: "",              // 16
            d.vref ?: "",             // 17
            d.arrFlaps ?: "",         // 18

            d.flightNumber ?: "",     // 19
            d.airline ?: "",          // 20
            d.aircraft ?: "",         // 21
            d.fuel ?: "",             // 22
            d.pax ?: "",              // 23
            d.payload ?: "",          // 24
            d.airTime ?: "",          // 25
            d.blockTime ?: "",        // 26
            d.costIndex ?: "",        // 27
            d.reserveFuel ?: "",      // 28
            d.zfw ?: "",              // 29
            d.crzWind ?: "",          // 30
            d.crzOat ?: "",           // 31
            d.info ?: "",             // 32
            d.initAlt ?: "",          // 33
            d.squawk ?: "",           // 34
            d.scratchpad ?: ""        // 35
        )
    },
    restore = { raw ->
        val v = raw as List<*>
        fun s(i: Int): String? = (v[i] as? String)?.takeIf { it.isNotBlank() }

        FlightDraft(
            id = s(0)?.toLongOrNull(),
            dep = v[1] as String,
            arr = v[2] as String,
            flightType = s(3)?.let {
                runCatching { FlightType.valueOf(it) }.getOrDefault(FlightType.ONLINE)
            } ?: FlightType.ONLINE,
            depRwy = s(4),
            depGate = s(5),
            sid = s(6),
            cruiseFl = s(7),
            depFlaps = s(8),
            v2 = s(9),
            route = s(10),
            depQnh = s(11),

            arrRwy = s(12),
            arrGate = s(13),
            star = s(14),
            altn = s(15),
            qnh = s(16),
            vref = s(17),
            arrFlaps = s(18),

            flightNumber = s(19),
            airline = s(20),
            aircraft = s(21),
            fuel = s(22),
            pax = s(23),
            payload = s(24),
            airTime = s(25),
            blockTime = s(26),
            costIndex = s(27),
            reserveFuel = s(28),
            zfw = s(29),
            crzWind = s(30),
            crzOat = s(31),

            info = s(32),
            initAlt = s(33),
            squawk = s(34),

            scratchpad = s(35)
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

                // New flight: adopt the generated ID so later saves update instead of insert
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
                lastLanded = lastLandedDisplay,
                currentFlight = draft
            )

            SectionJumpChips(
                activeSection = activeSection,
                showAtc = draft.flightType != FlightType.OFFLINE_NO_ATC,
                onSectionClick = { section -> activeSection = section }
            )

            GradientButton(
                text = if (editFlightId != null) "Update Flight" else "Save Flight",
                enabled = draft.isValid(),
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
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
            )

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
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RouteHeader(draft, onDraftChange)

            ActiveSectionCard(draft, onDraftChange, qnhUnit, tempUnit, activeSection)

            Spacer(Modifier.height(8.dp))
        }

        ScratchpadCard(draft, onDraftChange)
    }
}

@Composable
private fun MediumTwoColumnLayout(
    draft: FlightDraft,
    onDraftChange: (FlightDraft) -> Unit,
    qnhUnit: QnhUnit,
    tempUnit: TempUnit,
    activeSection: FlightSection
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RouteHeader(draft, onDraftChange)
            ActiveSectionCard(draft, onDraftChange, qnhUnit, tempUnit, activeSection)
        }

        ScratchpadCard(draft, onDraftChange)
    }
}

@Composable
private fun ExpandedWhiteboardLayout(
    draft: FlightDraft,
    onDraftChange: (FlightDraft) -> Unit,
    qnhUnit: QnhUnit,
    tempUnit: TempUnit,
    activeSection: FlightSection
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RouteHeader(draft, onDraftChange)
            ActiveSectionCard(draft, onDraftChange, qnhUnit, tempUnit, activeSection)
        }

        ScratchpadCard(draft, onDraftChange)
    }
}

// Shared by all three layouts so a section change only has to be made once.
@Composable
private fun ActiveSectionCard(
    draft: FlightDraft,
    onDraftChange: (FlightDraft) -> Unit,
    qnhUnit: QnhUnit,
    tempUnit: TempUnit,
    activeSection: FlightSection
) {
    when (activeSection) {
        FlightSection.DEPARTURE -> SectionCard(title = "Departure + Enroute") {
            DepartureEnrouteFields(draft, onDraftChange, qnhUnit)
        }

        FlightSection.ARRIVAL -> SectionCard(title = "Arrival") {
            ArrivalFields(draft, onDraftChange, qnhUnit)
        }

        FlightSection.AIRCRAFT -> SectionCard(title = "Aircraft + Performance") {
            AircraftPerfFields(draft, onDraftChange, tempUnit)
        }

        FlightSection.ATC -> SectionCard(title = "ATC") {
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

@Composable
private fun ScratchpadCard(
    draft: FlightDraft,
    onDraftChange: (FlightDraft) -> Unit
) {
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

/* ----------------------------- Sections ----------------------------- */

@Composable
private fun RouteHeader(
    draft: FlightDraft,
    onDraftChange: (FlightDraft) -> Unit
) {
    SectionCard(title = "Route") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AirportField(
                label = "DEP",
                value = draft.dep,
                onValueChange = { onDraftChange(draft.copy(dep = it)) },
                modifier = Modifier.weight(1f)
            )
            Text("→", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp))
            AirportField(
                label = "ARR",
                value = draft.arr,
                onValueChange = { onDraftChange(draft.copy(arr = it)) },
                modifier = Modifier.weight(1f)
            )
        }

        val info = Airlines.formatFlightInfo(draft.airline, draft.flightNumber, draft.aircraft)
        if (info.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = info,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    FlightTypeDropdown(
        flightType = draft.flightType,
        onFlightTypeChange = { onChange(draft.copy(flightType = it)) },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        RunwayField(
            label = "RWY",
            value = draft.depRwy.orEmpty(),
            airportIcao = draft.dep,
            onValueChange = { onChange(draft.copy(depRwy = it)) },
            modifier = Modifier.weight(1f)
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
        FlapField(
            label = "Flaps",
            value = draft.depFlaps.orEmpty(),
            options = remember(draft.aircraft) { AircraftTypes.takeoffFlapsFor(draft.aircraft) },
            onValueChange = { onChange(draft.copy(depFlaps = it)) },
            modifier = Modifier.weight(1f)
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

    QnhField(
        label = "QNH",
        value = draft.depQnh.orEmpty(),
        qnhUnit = qnhUnit,
        onChange = { onChange(draft.copy(depQnh = it.takeIf { s -> s.isNotBlank() })) },
        modifier = Modifier.fillMaxWidth(0.33f)
    )

    RouteTextField(
        value = draft.route.orEmpty(),
        onChange = { onChange(draft.copy(route = it.uppercase().takeIf { s -> s.isNotBlank() })) }
    )
}

@Composable
private fun ArrivalFields(d: FlightDraft, onChange: (FlightDraft) -> Unit, qnhUnit: QnhUnit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        RunwayField(
            label = "RWY",
            value = d.arrRwy.orEmpty(),
            airportIcao = d.arr,
            onValueChange = { onChange(d.copy(arrRwy = it)) },
            modifier = Modifier.weight(1f)
        )
        TextFieldSmall(
            "Gate",
            d.arrGate.orEmpty(),
            { onChange(d.copy(arrGate = it.uppercase().takeIf { s -> s.isNotBlank() })) },
            Modifier.weight(1f),
            capitalization = KeyboardCapitalization.Characters
        )
        TextFieldSmall(
            "STAR",
            d.star.orEmpty(),
            { onChange(d.copy(star = it.uppercase().takeIf { s -> s.isNotBlank() })) },
            Modifier.weight(1f),
            capitalization = KeyboardCapitalization.Characters
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
        QnhField(
            label = "QNH",
            value = d.qnh.orEmpty(),
            qnhUnit = qnhUnit,
            onChange = { onChange(d.copy(qnh = it.takeIf { s -> s.isNotBlank() })) },
            modifier = Modifier.weight(1f)
        )
        FlapField(
            label = "Arr Flaps",
            value = d.arrFlaps.orEmpty(),
            options = remember(d.aircraft) { AircraftTypes.landingFlapsFor(d.aircraft) },
            onValueChange = { onChange(d.copy(arrFlaps = it)) },
            modifier = Modifier.weight(1f)
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
        AirlineDropdown(
            airline = d.airline,
            onAirlineChange = { onChange(d.copy(airline = it)) },
            modifier = Modifier.weight(1f)
        )
        TextFieldSmall(
            "Flight #",
            d.flightNumber.orEmpty(),
            { onChange(d.copy(flightNumber = it.uppercase().takeIf { s -> s.isNotBlank() })) },
            Modifier.weight(1f),
            capitalization = KeyboardCapitalization.Characters
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        AircraftField(
            value = d.aircraft.orEmpty(),
            onValueChange = { onChange(d.copy(aircraft = it)) },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.weight(1f))
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ----------------------------- Components ----------------------------- */

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
                    else -> offset - 1
                }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

class CrzWindVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitsOnly = text.text.filter { it.isDigit() }.take(6) // XXX/XXX

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
                return when {
                    offset <= 3 -> offset
                    // Leading zeros in speed are trimmed, so clamp to what's actually shown
                    else -> (offset + 1).coerceAtMost(formatted.length)
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 3 -> offset
                    offset == 4 -> 3 // Slash position maps back to position 3
                    else -> (offset - 1).coerceAtMost(digitsOnly.length)
                }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

class QnhInHgVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitsOnly = text.text.filter { it.isDigit() }.take(4)

        val formatted = when {
            digitsOnly.isEmpty() -> ""
            digitsOnly.length <= 2 -> digitsOnly
            else -> "${digitsOnly.take(2)}.${digitsOnly.drop(2)}"
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset <= 2 -> offset
                    else -> (offset + 1).coerceAtMost(formatted.length)
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 2 -> offset
                    offset == 3 -> 2 // Decimal point maps back to position 2
                    else -> (offset - 1).coerceAtMost(digitsOnly.length)
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
    TextField(
        value = value,
        onValueChange = { input ->
            // Store plain digits; the VisualTransformation handles the colon
            onChange(input.filter { it.isDigit() }.take(4))
        },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = TimeVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = FieldShape,
        colors = fdvFieldColors(),
        modifier = modifier.inset(raisedColors())
    )
}

@Composable
private fun CrzWindField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = { input ->
            onChange(input.filter { it.isDigit() }.take(6))
        },
        label = { Text("Crz. Wind") },
        singleLine = true,
        visualTransformation = CrzWindVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = FieldShape,
        colors = fdvFieldColors(),
        modifier = modifier.inset(raisedColors())
    )
}

@Composable
private fun QnhField(
    label: String,
    value: String,
    qnhUnit: QnhUnit,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = { input ->
            onChange(input.filter { it.isDigit() }.take(4))
        },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = when (qnhUnit) {
            QnhUnit.INHG -> QnhInHgVisualTransformation() // XX.XX
            QnhUnit.HPA -> VisualTransformation.None      // XXXX
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = FieldShape,
        colors = fdvFieldColors(),
        modifier = modifier.inset(raisedColors())
    )
}

@Composable
private fun IdentityStrip(
    pilotId: String,
    pilotName: String,
    hub: String,
    lastLanded: String,
    currentFlight: FlightDraft? = null
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .raised(raisedColors())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Pilot: $pilotId • $pilotName",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Hub: $hub • Last landed: $lastLanded",
            style = MaterialTheme.typography.bodyMedium
        )

        if (currentFlight != null) {
            val info = Airlines.formatFlightInfo(
                currentFlight.airline, currentFlight.flightNumber, currentFlight.aircraft
            )
            if (info.isNotEmpty()) {
                Text(
                    text = "Current Flight: $info",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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
            // Extra vertical room so the raised shadows/glow aren't clipped by the scroll row
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionChip(
            label = "Departure",
            selected = activeSection == FlightSection.DEPARTURE,
            onClick = { onSectionClick(FlightSection.DEPARTURE) }
        )
        SectionChip(
            label = "Arrival",
            selected = activeSection == FlightSection.ARRIVAL,
            onClick = { onSectionClick(FlightSection.ARRIVAL) }
        )
        SectionChip(
            label = "Aircraft",
            selected = activeSection == FlightSection.AIRCRAFT,
            onClick = { onSectionClick(FlightSection.AIRCRAFT) }
        )
        if (showAtc) {
            SectionChip(
                label = "ATC",
                selected = activeSection == FlightSection.ATC,
                onClick = { onSectionClick(FlightSection.ATC) }
            )
        }
    }
}

@Composable
private fun RightEdgeFadeWithChevron(modifier: Modifier = Modifier) {
    val bg = MaterialTheme.colorScheme.background
    val fadeWidth = 28.dp

    Box(
        modifier = modifier.graphicsLayer { }
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
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
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .raised(raisedColors())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
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
    TextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization
        ),
        shape = FieldShape,
        colors = fdvFieldColors(),
        modifier = modifier.inset(raisedColors())
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
        TextField(
            value = flightType.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Flight Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = FieldShape,
            colors = fdvFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .inset(raisedColors())
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

/**
 * Editable text field with a suggestion dropdown.
 * Free text is always allowed; suggestions only help fill it in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> AutocompleteField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<T>,
    onSuggestionSelected: (T) -> Unit,
    itemContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && suggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        TextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true // Reopen suggestions on every edit
            },
            label = { Text(label) },
            singleLine = true,
            supportingText = supportingText?.let {
                { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = KeyboardCapitalization.Characters
            ),
            shape = FieldShape,
            colors = fdvFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                // PrimaryEditable keeps the keyboard open while the menu shows
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .inset(raisedColors())
        )

        ExposedDropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            suggestions.forEach { item ->
                DropdownMenuItem(
                    text = { itemContent(item) },
                    onClick = {
                        onSuggestionSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AirportField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Recomputed only when the text changes, not on every recomposition
    val suggestions = remember(value) { AirportRepository.search(value) }
    val matched = remember(value) { AirportRepository.getByIcao(value) }

    AutocompleteField(
        label = label,
        value = value,
        onValueChange = { onValueChange(it.uppercase()) },
        suggestions = suggestions,
        onSuggestionSelected = { onValueChange(it.icao) },
        itemContent = { airport ->
            Column {
                Text(airport.icao, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = listOf(airport.name, airport.city)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        supportingText = matched?.name,
        modifier = modifier
    )
}

@Composable
private fun RunwayField(
    label: String,
    value: String,
    airportIcao: String,
    onValueChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val runways = remember(airportIcao) { AirportRepository.runwaysFor(airportIcao) }
    // Empty field shows every runway; typing narrows it ("2" → 26L, 26R, 27L...)
    val suggestions = remember(value, runways) {
        if (value.isBlank()) runways else runways.filter { it.startsWith(value.uppercase()) }
    }

    AutocompleteField(
        label = label,
        value = value,
        onValueChange = { onValueChange(it.uppercase().takeIf { s -> s.isNotBlank() }) },
        suggestions = suggestions,
        onSuggestionSelected = { onValueChange(it) },
        itemContent = { Text(it) },
        modifier = modifier
    )
}

@Composable
private fun AircraftField(
    value: String,
    onValueChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = remember(value) { AircraftTypes.search(value) }
    val matched = remember(value) { AircraftTypes.getByIcao(value) }

    AutocompleteField(
        label = "Aircraft",
        value = value,
        onValueChange = { onValueChange(it.uppercase().takeIf { s -> s.isNotBlank() }) },
        suggestions = suggestions,
        onSuggestionSelected = { onValueChange(it.icao) },
        itemContent = { type ->
            Column {
                Text(type.icao, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = type.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        supportingText = matched?.name,
        modifier = modifier
    )
}

@Composable
private fun FlapField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Empty field shows every setting; typing narrows it ("1" → 1+F)
    val suggestions = remember(value, options) {
        if (value.isBlank()) options else options.filter { it.startsWith(value.uppercase()) }
    }

    AutocompleteField(
        label = label,
        value = value,
        onValueChange = { onValueChange(it.uppercase().trim().takeIf { s -> s.isNotBlank() }) },
        suggestions = suggestions,
        onSuggestionSelected = { onValueChange(it) },
        itemContent = { Text(it) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AirlineDropdown(
    airline: String?,
    onAirlineChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        TextField(
            value = airline ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Airline") },
            placeholder = { Text("Select") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = FieldShape,
            colors = fdvFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .inset(raisedColors())
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onAirlineChange(null)
                    expanded = false
                }
            )

            Airlines.ALL.forEach { airlineItem ->
                DropdownMenuItem(
                    text = { Text("${airlineItem.icao} - ${airlineItem.name}") },
                    onClick = {
                        onAirlineChange(airlineItem.icao)
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
    TextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Route") },
        minLines = 2,
        shape = FieldShape,
        colors = fdvFieldColors(),
        modifier = modifier
            .fillMaxWidth()
            .inset(raisedColors())
    )
}

@Composable
private fun NotesField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Notes / Taxi / Clearance") },
        minLines = 4,
        shape = FieldShape,
        colors = fdvFieldColors(),
        modifier = modifier
            .fillMaxWidth()
            .inset(raisedColors())
    )
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
 */
private fun formatTime(input: String): String {
    val digitsOnly = input.filter { it.isDigit() }

    return when {
        digitsOnly.isEmpty() -> ""
        digitsOnly.length <= 2 -> digitsOnly
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
        QnhUnit.HPA -> digitsOnly.filter { it.isDigit() }.take(4)
    }
}

/**
 * Formats temperature as integer with optional minus sign.
 * Truncates decimals toward zero (e.g., -15.7 → -15, 15.3 → 15).
 */
private fun formatTemperature(input: String): String {
    val cleaned = input.filter { it.isDigit() || it == '-' || it == '.' }

    if (cleaned.isEmpty() || cleaned == "-") return cleaned

    return try {
        cleaned.toDouble().toInt().toString()
    } catch (_: NumberFormatException) {
        cleaned.filter { it.isDigit() || it == '-' }.take(4)
    }
}