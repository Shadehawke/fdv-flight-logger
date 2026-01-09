package com.fdv.fdvflightlogger.ui.screens

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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fdv.fdvflightlogger.ui.AppViewModel


private val FlightDraftSaver: Saver<FlightDraft, Any> = listSaver(
    save = { d ->
        listOf(
            d.dep, d.arr,
            d.depRwy, d.depGate, d.sid, d.cruiseFl, d.depFlaps, d.v2, d.route,
            d.arrRwy, d.arrGate, d.star, d.altn, d.qnh, d.vref,
            d.flightNumber, d.aircraft, d.fuel, d.pax, d.payload,
            d.airTime, d.blockTime, d.costIndex, d.reserveFuel, d.zfw,
            d.crzWind, d.crzOat,
            d.info, d.initAlt, d.squawk,
            d.scratchpad
        )
    },
    restore = { list ->
        val v = list.map { it as String }
        FlightDraft(
            dep = v[0], arr = v[1],
            depRwy = v[2], depGate = v[3], sid = v[4], cruiseFl = v[5],
            depFlaps = v[6], v2 = v[7], route = v[8],
            arrRwy = v[9], arrGate = v[10], star = v[11], altn = v[12],
            qnh = v[13], vref = v[14],
            flightNumber = v[15], aircraft = v[16], fuel = v[17], pax = v[18],
            payload = v[19], airTime = v[20], blockTime = v[21], costIndex = v[22],
            reserveFuel = v[23], zfw = v[24],
            crzWind = v[25], crzOat = v[26],
            info = v[27], initAlt = v[28], squawk = v[29],
            scratchpad = v[30]
        )
    }
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightLogScreen(appViewModel: AppViewModel, navController: NavController) {
    val state by appViewModel.state.collectAsStateWithLifecycle()
    val lastLandedDisplay = state.lastLanded.ifBlank { "--" }

    var draft by rememberSaveable(stateSaver = FlightDraftSaver) {
        mutableStateOf(FlightDraft())
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val widthClass = when {
        screenWidthDp >= 840 -> WindowWidthSizeClass.Expanded
        screenWidthDp >= 600 -> WindowWidthSizeClass.Medium
        else -> WindowWidthSizeClass.Compact
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FDV Flight Logger") },
                actions = {
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
                onJump = { /* MVP: chips are purely visual until we add scroll anchors */ }
            )

            Button(
                onClick = {
                    appViewModel.saveFlight(draft)
                    draft = FlightDraft()
                },
                enabled = draft.dep.isNotBlank() && draft.arr.isNotBlank(),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Text("Save Flight")
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
                value = draft.scratchpad,
                onChange = { onDraftChange(draft.copy(scratchpad = it)) }
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
                        value = draft.scratchpad,
                        onChange = { onDraftChange(draft.copy(scratchpad = it)) }
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
            // Left column: Departure + Enroute
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionCard(title = "Departure + Enroute") {
                    DepartureEnrouteFields(draft, onDraftChange)
                }
            }

            // Middle column: Arrival
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionCard(title = "Arrival") {
                    ArrivalFields(draft, onDraftChange)
                }
                SectionCard(title = "Scratchpad") {
                    NotesField(
                        value = draft.scratchpad,
                        onChange = { onDraftChange(draft.copy(scratchpad = it)) }
                    )
                }
            }

            // Right column: Aircraft/Perf
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
private fun DepartureEnrouteFields(d: FlightDraft, onChange: (FlightDraft) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("RWY", d.depRwy, { onChange(d.copy(depRwy = it)) }, Modifier.weight(1f))
        TextFieldSmall("Gate", d.depGate, { onChange(d.copy(depGate = it)) }, Modifier.weight(1f))
        TextFieldSmall("SID", d.sid, { onChange(d.copy(sid = it)) }, Modifier.weight(1f))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("Cruise (FL)", d.cruiseFl, { onChange(d.copy(cruiseFl = it)) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
        TextFieldSmall("Flaps", d.depFlaps, { onChange(d.copy(depFlaps = it)) }, Modifier.weight(1f))
        TextFieldSmall("V2", d.v2, { onChange(d.copy(v2 = it)) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
    }

    TextFieldLarge(
        label = "Route",
        value = d.route,
        onChange = { onChange(d.copy(route = it)) }
    )
}

@Composable
private fun ArrivalFields(d: FlightDraft, onChange: (FlightDraft) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("RWY", d.arrRwy, { onChange(d.copy(arrRwy = it)) }, Modifier.weight(1f))
        TextFieldSmall("Gate", d.arrGate, { onChange(d.copy(arrGate = it)) }, Modifier.weight(1f))
        TextFieldSmall("STAR", d.star, { onChange(d.copy(star = it)) }, Modifier.weight(1f))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("ALTN", d.altn, { onChange(d.copy(altn = it.uppercase())) }, Modifier.weight(1f), capitalization = KeyboardCapitalization.Characters)
        TextFieldSmall("QNH", d.qnh, { onChange(d.copy(qnh = it)) }, Modifier.weight(1f))
        TextFieldSmall("Vref", d.vref, { onChange(d.copy(vref = it)) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
    }
}

@Composable
private fun AircraftPerfFields(d: FlightDraft, onChange: (FlightDraft) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("Flight #", d.flightNumber, { onChange(d.copy(flightNumber = it.uppercase())) }, Modifier.weight(1f), capitalization = KeyboardCapitalization.Characters)
        TextFieldSmall("Aircraft", d.aircraft, { onChange(d.copy(aircraft = it.uppercase())) }, Modifier.weight(1f), capitalization = KeyboardCapitalization.Characters)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("Fuel", d.fuel, { onChange(d.copy(fuel = it)) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
        TextFieldSmall("PAX", d.pax, { onChange(d.copy(pax = it)) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
        TextFieldSmall("Payload", d.payload, { onChange(d.copy(payload = it)) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("A. Time", d.airTime, { onChange(d.copy(airTime = it)) }, Modifier.weight(1f))
        TextFieldSmall("B. Time", d.blockTime, { onChange(d.copy(blockTime = it)) }, Modifier.weight(1f))
        TextFieldSmall("CI", d.costIndex, { onChange(d.copy(costIndex = it)) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("R. Fuel", d.reserveFuel, { onChange(d.copy(reserveFuel = it)) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
        TextFieldSmall("ZFW", d.zfw, { onChange(d.copy(zfw = it)) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TextFieldSmall("Crz. Wind", d.crzWind, { onChange(d.copy(crzWind = it)) }, Modifier.weight(1f))
        TextFieldSmall("Crz. OAT", d.crzOat, { onChange(d.copy(crzOat = it)) }, Modifier.weight(1f))
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
    ElevatedCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
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
private fun SectionJumpChips(onJump: (String) -> Unit) {
    val scroll = rememberScrollState()

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scroll),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(onClick = { onJump("dep") }, label = { Text("Departure") })
            AssistChip(onClick = { onJump("arr") }, label = { Text("Arrival") })
            AssistChip(onClick = { onJump("ac") }, label = { Text("Aircraft") })
            AssistChip(onClick = { onJump("atc") }, label = { Text("ATC") })
            AssistChip(onClick = { onJump("notes") }, label = { Text("Notes") })
        }

        // Right-edge affordance: fade + chevron (only shows when there is more to scroll)
        val canScrollMore = scroll.value < scroll.maxValue
        if (canScrollMore) {
            RightEdgeFadeWithChevron(
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun RightEdgeFadeWithChevron(modifier: Modifier = Modifier) {
    // Use surface/background tones so it blends with themes
    val bg = MaterialTheme.colorScheme.background
    val fadeWidth = 28.dp

    Box(
        modifier = modifier
            .graphicsLayer { } // forces a draw layer
            .padding(end = 0.dp)
    ) {
        // Fade overlay
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

        // Chevron overlay
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
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                Text(title, style = MaterialTheme.typography.titleMedium)
                content()
            }
        )
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
        modifier = modifier
    )
}

@Composable
private fun TextFieldLarge(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun NotesField(
    value: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Notes / Taxi / Clearance") },
        minLines = 4,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AtcStrip(
    info: String,
    initAlt: String,
    squawk: String,
    onInfoChange: (String) -> Unit,
    onInitAltChange: (String) -> Unit,
    onSquawkChange: (String) -> Unit
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
                value = info,
                onChange = onInfoChange,
                modifier = Modifier.weight(1f),
                capitalization = KeyboardCapitalization.Characters
            )
            TextFieldSmall(
                label = "Init. Alt.",
                value = initAlt,
                onChange = onInitAltChange,
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
            TextFieldSmall(
                label = "Sqwk",
                value = squawk,
                onChange = onSquawkChange,
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
        }
    }
}