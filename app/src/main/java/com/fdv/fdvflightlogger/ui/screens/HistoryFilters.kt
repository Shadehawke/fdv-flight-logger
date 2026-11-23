package com.fdv.fdvflightlogger.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fdv.fdvflightlogger.data.Airlines
import com.fdv.fdvflightlogger.data.aircraft.AircraftTypes
import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.data.db.FlightType
import com.fdv.fdvflightlogger.data.history.DatePreset
import com.fdv.fdvflightlogger.data.history.FdvStanding
import com.fdv.fdvflightlogger.data.history.HistoryFilter
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private enum class FilterSheet { DATE, AIRLINE, AIRCRAFT, TYPE, STANDING }

/** One row in a filter sheet. */
private data class Choice(val key: String, val label: String, val count: Int)

private val ShortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
private val LongDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

/** Saved as JSON text, so the filter survives rotation and the app being closed in the background. */
internal val HistoryFilterSaver = Saver<HistoryFilter, String>(
    save = { Json.encodeToString(HistoryFilter.serializer(), it) },
    // A filter saved by an older version that no longer reads simply starts fresh
    restore = { saved -> runCatching { Json.decodeFromString(HistoryFilter.serializer(), saved) }.getOrNull() }
)

/** The chip row under History's search, and the sheet each chip opens. Choices come from [flights]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryFilterBar(
    filter: HistoryFilter,
    flights: List<FlightLogEntity>,
    onChange: (HistoryFilter) -> Unit
) {
    var open by rememberSaveable { mutableStateOf<FilterSheet?>(null) }
    val dropIcon: @Composable () -> Unit = {
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Five chips don't fit a phone, so the row scrolls sideways instead of wrapping
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = filter.datePreset != DatePreset.ANY,
            onClick = { open = FilterSheet.DATE },
            label = { Text(dateLabel(filter)) },
            trailingIcon = dropIcon
        )
        FilterChip(
            selected = filter.airlines.isNotEmpty(),
            onClick = { open = FilterSheet.AIRLINE },
            label = { Text(chipLabel("Airline", filter.airlines) { it }) },
            trailingIcon = dropIcon
        )
        FilterChip(
            selected = filter.aircraft.isNotEmpty(),
            onClick = { open = FilterSheet.AIRCRAFT },
            label = { Text(chipLabel("Aircraft", filter.aircraft) { it }) },
            trailingIcon = dropIcon
        )
        FilterChip(
            selected = filter.flightTypes.isNotEmpty(),
            onClick = { open = FilterSheet.TYPE },
            label = { Text(chipLabel("Type", filter.flightTypes) { FlightType.valueOf(it).displayName() }) },
            trailingIcon = dropIcon
        )
        FilterChip(
            selected = filter.standings.isNotEmpty(),
            onClick = { open = FilterSheet.STANDING },
            label = { Text(chipLabel("FDV status", filter.standings.map { it.name }.toSet()) { FdvStanding.valueOf(it).label }) },
            trailingIcon = dropIcon
        )
        if (filter.isActive) {
            AssistChip(
                onClick = { onChange(HistoryFilter()) },
                label = { Text("Clear") },
                leadingIcon = {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                }
            )
        }
    }

    val close = { open = null }
    when (open) {
        null -> Unit
        FilterSheet.DATE -> DateSheet(filter = filter, onChange = onChange, onDismiss = close)
        FilterSheet.AIRLINE -> ChoiceSheet(
            title = "Airline",
            choices = remember(flights) {
                HistoryFilter.choices(flights.map { it.airline }).map { (code, count) ->
                    Choice(code, Airlines.getByIcao(code)?.let { "$code · ${it.name}" } ?: code, count)
                }
            },
            selected = filter.airlines,
            onSelectedChange = { onChange(filter.copy(airlines = it)) },
            onDismiss = close
        )
        FilterSheet.AIRCRAFT -> ChoiceSheet(
            title = "Aircraft",
            choices = remember(flights) {
                HistoryFilter.choices(flights.map { it.aircraft }).map { (code, count) ->
                    Choice(code, AircraftTypes.getByIcao(code)?.let { "$code · ${it.name}" } ?: code, count)
                }
            },
            selected = filter.aircraft,
            onSelectedChange = { onChange(filter.copy(aircraft = it)) },
            onDismiss = close
        )
        FilterSheet.TYPE -> ChoiceSheet(
            title = "Flight type",
            choices = remember(flights) {
                FlightType.entries
                    .map { type -> Choice(type.name, type.displayName(), flights.count { it.flightType == type.name }) }
                    .filter { it.count > 0 }
            },
            selected = filter.flightTypes,
            onSelectedChange = { onChange(filter.copy(flightTypes = it)) },
            onDismiss = close
        )
        FilterSheet.STANDING -> ChoiceSheet(
            title = "FDV status",
            choices = remember(flights) {
                FdvStanding.entries
                    .map { standing -> Choice(standing.name, standing.label, flights.count { FdvStanding.of(it) == standing }) }
                    .filter { it.count > 0 }
            },
            selected = filter.standings.map { it.name }.toSet(),
            onSelectedChange = { names -> onChange(filter.copy(standings = names.map(FdvStanding::valueOf).toSet())) },
            onDismiss = close
        )
    }
}

/** A sheet of ticks: any number can be chosen; none means "any". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceSheet(
    title: String,
    choices: List<Choice>,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetHeader(title = title, showClear = selected.isNotEmpty(), onClear = { onSelectedChange(emptySet()) })
        LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(choices, key = { it.key }) { choice ->
                val checked = choice.key in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // The whole row is the tick box, and TalkBack reads it as one
                        .toggleable(
                            value = checked,
                            role = Role.Checkbox,
                            onValueChange = { onSelectedChange(if (checked) selected - choice.key else selected + choice.key) }
                        )
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    Text(
                        text = choice.label,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    Text(
                        text = "%,d".format(choice.count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Presets apply and close; Custom range opens the calendar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSheet(filter: HistoryFilter, onChange: (HistoryFilter) -> Unit, onDismiss: () -> Unit) {
    var pickRange by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetHeader(
            title = "Date",
            showClear = filter.datePreset != DatePreset.ANY,
            onClear = { onChange(filter.copy(datePreset = DatePreset.ANY, fromDay = null, toDay = null)) }
        )
        DatePreset.entries.forEach { preset ->
            val chosen = filter.datePreset == preset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = chosen,
                        role = Role.RadioButton,
                        onClick = {
                            if (preset == DatePreset.CUSTOM) {
                                pickRange = true
                            } else {
                                onChange(filter.copy(datePreset = preset, fromDay = null, toDay = null))
                                onDismiss()
                            }
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = chosen, onClick = null)
                Text(
                    text = if (preset == DatePreset.CUSTOM && chosen) customLabel(filter) else preset.label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (pickRange) {
        RangePickerDialog(
            filter = filter,
            onPicked = { from, to ->
                onChange(filter.copy(datePreset = DatePreset.CUSTOM, fromDay = from, toDay = to))
                pickRange = false
                onDismiss()
            },
            onDismiss = { pickRange = false }
        )
    }
}

/** Material's date-range calendar. It works in UTC-midnight millis, converted here to calendar days. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePickerDialog(filter: HistoryFilter, onPicked: (Long, Long) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = filter.fromDay?.let { LocalDate.ofEpochDay(it).toUtcMillis() },
        initialSelectedEndDateMillis = filter.toDay?.let { LocalDate.ofEpochDay(it).toUtcMillis() }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedStartDateMillis != null,
                onClick = {
                    val from = state.selectedStartDateMillis?.utcMillisToEpochDay() ?: return@TextButton
                    // Only a start picked means that one day
                    val to = state.selectedEndDateMillis?.utcMillisToEpochDay() ?: from
                    onPicked(from, to)
                }
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DateRangePicker(state = state, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SheetHeader(title: String, showClear: Boolean, onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (showClear) TextButton(onClick = onClear) { Text("Clear") }
    }
}

// "Airline", "Airline: DAL", "Airline: 3"
private fun chipLabel(base: String, selected: Set<String>, nameOf: (String) -> String): String = when (selected.size) {
    0 -> base
    1 -> "$base: ${nameOf(selected.single())}"
    else -> "$base: ${selected.size}"
}

private fun dateLabel(filter: HistoryFilter): String = when (filter.datePreset) {
    DatePreset.ANY -> "Date"
    DatePreset.CUSTOM -> customLabel(filter)
    else -> filter.datePreset.label
}

// "Nov 1 – Nov 15", "From Nov 1", "Until Jan 1, 2026"
private fun customLabel(filter: HistoryFilter): String {
    val thisYear = LocalDate.now().year
    fun show(day: Long): String {
        val date = LocalDate.ofEpochDay(day)
        return date.format(if (date.year == thisYear) ShortDate else LongDate)
    }
    val from = filter.fromDay
    val to = filter.toDay
    return when {
        from != null && to != null && from == to -> show(from)
        from != null && to != null -> "${show(from)} – ${show(to)}"
        from != null -> "From ${show(from)}"
        to != null -> "Until ${show(to)}"
        else -> DatePreset.CUSTOM.label
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.utcMillisToEpochDay(): Long = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()