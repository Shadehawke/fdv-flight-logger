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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fdv.fdvflightlogger.ui.AppViewModel
import com.fdv.fdvflightlogger.ui.mappers.toDraft
import com.fdv.fdvflightlogger.ui.theme.*

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
            d.depRwy ?: "",
            d.depGate ?: "",
            d.sid ?: "",
            d.cruiseFl ?: "",
            d.depFlaps ?: "",
            d.v2 ?: "",
            d.route ?: "",
            d.arrRwy ?: "",
            d.arrGate ?: "",
            d.star ?: "",
            d.altn ?: "",
            d.qnh ?: "",
            d.vref ?: "",
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
            depRwy = (v[3] as? String)?.takeIf { it.isNotBlank() },
            depGate = (v[4] as? String)?.takeIf { it.isNotBlank() },
            sid = (v[5] as? String)?.takeIf { it.isNotBlank() },
            cruiseFl = (v[6] as? String)?.takeIf { it.isNotBlank() },
            depFlaps = (v[7] as? String)?.takeIf { it.isNotBlank() },
            v2 = (v[8] as? String)?.takeIf { it.isNotBlank() },
            route = (v[9] as? String)?.takeIf { it.isNotBlank() },
            arrRwy = (v[10] as? String)?.takeIf { it.isNotBlank() },
            arrGate = (v[11] as? String)?.takeIf { it.isNotBlank() },
            star = (v[12] as? String)?.takeIf { it.isNotBlank() },
            altn = (v[13] as? String)?.takeIf { it.isNotBlank() },
            qnh = (v[14] as? String)?.takeIf { it.isNotBlank() },
            vref = (v[15] as? String)?.takeIf { it.isNotBlank() },
            flightNumber = (v[16] as? String)?.takeIf { it.isNotBlank() },
            aircraft = (v[17] as? String)?.takeIf { it.isNotBlank() },
            fuel = (v[18] as? String)?.takeIf { it.isNotBlank() },
            pax = (v[19] as? String)?.takeIf { it.isNotBlank() },
            payload = (v[20] as? String)?.takeIf { it.isNotBlank() },
            airTime = (v[21] as? String)?.takeIf { it.isNotBlank() },
            blockTime = (v[22] as? String)?.takeIf { it.isNotBlank() },
            costIndex = (v[23] as? String)?.takeIf { it.isNotBlank() },
            reserveFuel = (v[24] as? String)?.takeIf { it.isNotBlank() },
            zfw = (v[25] as? String)?.takeIf { it.isNotBlank() },
            crzWind = (v[26] as? String)?.takeIf { it.isNotBlank() },
            crzOat = (v[27] as? String)?.takeIf { it.isNotBlank() },
            info = (v[28] as? String)?.takeIf { it.isNotBlank() },
            initAlt = (v[29] as? String)?.takeIf { it.isNotBlank() },
            squawk = (v[30] as? String)?.takeIf { it.isNotBlank() },
            scratchpad = (v[31] as? String)?.takeIf { it.isNotBlank() }
        )
    }
)

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

    if (confirmDiscard.value) {
        AlertDialog(
            onDismissRequest = { confirmDiscard.value = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Discard them and leave this screen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDiscard.value = false
                        // Revert to baseline so you don't leak edits if you stay
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
                jumpToSection = { /* still visual-only until anchors */ }
            )

            Button(
                onClick = {
                    appViewModel.saveFlight(draft)

                    if (editFlightId != null) {
                        // Edit mode: keep current values, but mark them as “saved”
                        initialDraft = draft
                    } else {
                        // Create mode: clear the form and baseline
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
                    onDraftChange = { draft = it }
                )

                WindowWidthSizeClass.Medium -> MediumTwoColumnLayout(
                    draft = draft,
                    onDraftChange = { draft = it }
                )

                else -> CompactSingleColumnLayout(
                    draft = draft,
                    onDraftChange = { draft = it }
                )
            }

            AtcStrip(
                info = draft.info,
                initAlt = draft.initAlt,
                squawk = draft.squawk,
                onInfoChange = { draft = draft.copy(info = it) },
                onInitAltChange = { draft = draft.copy(initAlt = it) },
                onSquawkChange = { draft = draft.copy(squawk = it) }
            )
        }
    }
}

/* ------------------------------- Layouts ------------------------------- */

@Composable
private fun CompactSingleColumnLayout(
    draft: FlightDraft,
    onDraftChange: (FlightDraft) -> Unit
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
    ) {
        RouteHeader(draft, onDraftChange)

        SectionCard(title = "Departure + Enroute") {
            DepartureEnrouteFields(draft, onDraftChange)
        }

        SectionCard(title = "Arrival") {
            ArrivalFields(draft, onDraftChange)
        }

        SectionCard(title = "Aircraft + Performance") {
            AircraftPerfFields(draft, onDraftChange)
        }

        SectionCard(title = "Scratchpad") {
            NotesField(
                value = draft.scratchpad.orEmpty(),
                onChange = { onDraftChange(draft.copy(scratchpad = it.takeIf { s -> s.isNotBlank() })) }
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MediumTwoColumnLayout(
    draft: FlightDraft,
    onDraftChange: (FlightDraft) -> Unit
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RouteHeader(draft, onDraftChange)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionCard(title = "Departure + Enroute") {
                    DepartureEnrouteFields(draft, onDraftChange)
                }
                SectionCard(title = "Arrival") {
                    ArrivalFields(draft, onDraftChange)
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionCard(title = "Aircraft + Performance") {
                    AircraftPerfFields(draft, onDraftChange)
                }
                SectionCard(title = "Scratchpad") {
                    NotesField(
                        value = draft.scratchpad.orEmpty(),
                        onChange = { onDraftChange(draft.copy(scratchpad = it.takeIf { s -> s.isNotBlank() })) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedWhiteboardLayout(
    draft: FlightDraft,
    onDraftChange: (FlightDraft) -> Unit
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RouteHeader(draft, onDraftChange)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionCard(title = "Departure + Enroute") {
                    DepartureEnrouteFields(draft, onDraftChange)
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionCard(title = "Arrival") {
                    ArrivalFields(draft, onDraftChange)
                }
                SectionCard(title = "Scratchpad") {
                    NotesField(
                        value = draft.scratchpad.orEmpty(),
                        onChange = { onDraftChange(draft.copy(scratchpad = it.takeIf { s -> s.isNotBlank() })) }
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionCard(title = "Aircraft + Performance") {
                    AircraftPerfFields(draft, onDraftChange)
                }
            }
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
private fun DepartureEnrouteFields(draft: FlightDraft, onChange: (FlightDraft) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("RWY", draft.depRwy.orEmpty(), { onChange(draft.copy(depRwy = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f))
        TextFieldSmall("Gate", draft.depGate.orEmpty(), { onChange(draft.copy(depGate = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f))
        TextFieldSmall("SID", draft.sid.orEmpty(), { onChange(draft.copy(sid = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("Cruise (FL)", draft.cruiseFl.orEmpty(), { onChange(draft.copy(cruiseFl = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
        TextFieldSmall("Flaps", draft.depFlaps.orEmpty(), { onChange(draft.copy(depFlaps = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f))
        TextFieldSmall("V2", draft.v2.orEmpty(), { onChange(draft.copy(v2 = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
    }

    RouteTextField(
        value = draft.route.orEmpty(),
        onChange = { onChange(draft.copy(route = it.takeIf { s -> s.isNotBlank() })) }
    )
}

@Composable
private fun ArrivalFields(d: FlightDraft, onChange: (FlightDraft) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("RWY", d.arrRwy.orEmpty(), { onChange(d.copy(arrRwy = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f))
        TextFieldSmall("Gate", d.arrGate.orEmpty(), { onChange(d.copy(arrGate = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f))
        TextFieldSmall("STAR", d.star.orEmpty(), { onChange(d.copy(star = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("ALTN", d.altn.orEmpty(), { onChange(d.copy(altn = it.uppercase().takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f), capitalization = KeyboardCapitalization.Characters)
        TextFieldSmall("QNH", d.qnh.orEmpty(), { onChange(d.copy(qnh = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f))
        TextFieldSmall("Vref", d.vref.orEmpty(), { onChange(d.copy(vref = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
    }
}

@Composable
private fun AircraftPerfFields(d: FlightDraft, onChange: (FlightDraft) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("Flight #", d.flightNumber.orEmpty(), { onChange(d.copy(flightNumber = it.uppercase().takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f), capitalization = KeyboardCapitalization.Characters)
        TextFieldSmall("Aircraft", d.aircraft.orEmpty(), { onChange(d.copy(aircraft = it.uppercase().takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f), capitalization = KeyboardCapitalization.Characters)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("Fuel", d.fuel.orEmpty(), { onChange(d.copy(fuel = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
        TextFieldSmall("PAX", d.pax.orEmpty(), { onChange(d.copy(pax = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
        TextFieldSmall("Payload", d.payload.orEmpty(), { onChange(d.copy(payload = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("A. Time", d.airTime.orEmpty(), { onChange(d.copy(airTime = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f))
        TextFieldSmall("B. Time", d.blockTime.orEmpty(), { onChange(d.copy(blockTime = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f))
        TextFieldSmall("CI", d.costIndex.orEmpty(), { onChange(d.copy(costIndex = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("R. Fuel", d.reserveFuel.orEmpty(), { onChange(d.copy(reserveFuel = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
        TextFieldSmall("ZFW", d.zfw.orEmpty(), { onChange(d.copy(zfw = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("Crz. Wind", d.crzWind.orEmpty(), { onChange(d.copy(crzWind = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f))
        TextFieldSmall("Crz. OAT", d.crzOat.orEmpty(), { onChange(d.copy(crzOat = it.takeIf { s -> s.isNotBlank() })) }, Modifier.weight(1f))
    }
}

/* ----------------------------- Components ----------------------------- */

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
private fun SectionJumpChips(jumpToSection: (String) -> Unit) {
    val scroll = rememberScrollState()

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(onClick = { jumpToSection("dep") }, label = { Text("Departure") })
            AssistChip(onClick = { jumpToSection("arr") }, label = { Text("Arrival") })
            AssistChip(onClick = { jumpToSection("ac") }, label = { Text("Aircraft") })
            AssistChip(onClick = { jumpToSection("atc") }, label = { Text("ATC") })
            AssistChip(onClick = { jumpToSection("notes") }, label = { Text("Notes") })
        }

        val canScrollMore = remember {
            derivedStateOf { scroll.value < scroll.maxValue }
        }.value
        if (canScrollMore) {
            RightEdgeFadeWithChevron(modifier = Modifier.align(Alignment.CenterEnd))
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
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
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
    info: String?,  // ← Change parameters to nullable
    initAlt: String?,
    squawk: String?,
    onInfoChange: (String?) -> Unit,  // ← Change callbacks to accept nullable
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


