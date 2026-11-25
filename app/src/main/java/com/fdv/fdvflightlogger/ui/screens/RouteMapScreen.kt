package com.fdv.fdvflightlogger.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fdv.fdvflightlogger.data.airports.AirportRepository
import com.fdv.fdvflightlogger.data.history.HistoryFilter
import com.fdv.fdvflightlogger.data.map.GreatCircle
import com.fdv.fdvflightlogger.data.map.LatLon
import com.fdv.fdvflightlogger.data.map.MapAirport
import com.fdv.fdvflightlogger.data.map.MapFit
import com.fdv.fdvflightlogger.data.map.MapRoute
import com.fdv.fdvflightlogger.data.map.MapView
import com.fdv.fdvflightlogger.data.map.RouteNetwork
import com.fdv.fdvflightlogger.data.map.WorldMap
import com.fdv.fdvflightlogger.data.map.WorldShapes
import com.fdv.fdvflightlogger.ui.AppViewModel
import com.fdv.fdvflightlogger.ui.components.FdvTopBar
import com.fdv.fdvflightlogger.ui.theme.DeltaRed
import com.fdv.fdvflightlogger.ui.theme.raised
import com.fdv.fdvflightlogger.ui.theme.raisedColors
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val MAX_ZOOM = 40f          // relative to the whole-world view
private const val LABELS_ALWAYS = 12      // the busiest airports are always labelled
private const val LABEL_ALL_ZOOM = 6f     // zoomed in this far, every airport is labelled

@Composable
fun RouteMapScreen(appViewModel: AppViewModel, navController: NavController, initialFilterJson: String?) {
    val context = LocalContext.current
    val flights by appViewModel.observeFlights().collectAsStateWithLifecycle(initialValue = emptyList())
    val appState by appViewModel.state.collectAsStateWithLifecycle()

    // Starts with History's filters when opened from there
    var filter by rememberSaveable(stateSaver = HistoryFilterSaver) {
        mutableStateOf(
            initialFilterJson
                ?.let { json -> runCatching { Json.decodeFromString(HistoryFilter.serializer(), json) }.getOrNull() }
                ?: HistoryFilter()
        )
    }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    val world by produceState<WorldShapes?>(initialValue = null) { value = WorldMap.load(context) }
    val airportsReady by produceState(initialValue = AirportRepository.isLoaded) {
        AirportRepository.ensureLoaded(context)
        value = true
    }

    val today = LocalDate.now()
    val network = remember(flights, filter, airportsReady, today) {
        val zone = ZoneId.systemDefault()
        RouteNetwork.build(flights.filter { filter.matches(it, today, zone) }, ::positionOf)
    }
    // Each route's arc, worked out once per network
    val arcs = remember(network) {
        val byIcao = network.airports.associateBy { it.icao }
        network.routes.map { route ->
            route to GreatCircle.points(byIcao.getValue(route.from).position, byIcao.getValue(route.to).position)
        }
    }
    // Fit to the arcs as well as the airports: long routes bulge toward the pole, past both ends
    val view = remember(network, arcs) {
        MapFit.fit(
            network.airports.map { it.position } +
                    arcs.flatMap { (_, points) -> (points.indices step 2).map { LatLon(points[it + 1].toDouble(), points[it].toDouble()) } }
        )
    }
    val hub = appState.profile.hub.trim().uppercase()

    Scaffold(
        topBar = {
            FdvTopBar(
                title = { Text("Route map") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Box(modifier = Modifier.padding(top = 12.dp)) {
                HistoryFilterBar(filter = filter, flights = flights, onChange = {
                    filter = it
                    selected = null
                })
            }
            Text(
                text = summary(network),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val shapes = world
                if (shapes == null || !airportsReady) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    RouteMapCanvas(
                        world = shapes,
                        network = network,
                        arcs = arcs,
                        view = view,
                        hub = hub,
                        selected = selected,
                        onSelect = { selected = it }
                    )
                    if (network.airports.isEmpty()) {
                        Text(
                            text = "No flights to map.\nChange the filters, or log a flight.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(32.dp)
                        )
                    }
                }

                selected?.let { icao -> network.airports.firstOrNull { it.icao == icao } }?.let { airport ->
                    AirportCard(
                        airport = airport,
                        network = network,
                        isHub = airport.icao == hub,
                        onClose = { selected = null },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    )
                }
            }
        }
    }
}

private fun positionOf(icao: String): LatLon? {
    val airport = AirportRepository.getByIcao(icao) ?: return null
    val lat = airport.lat ?: return null
    val lon = airport.lon ?: return null
    return LatLon(lat, lon)
}

// "128 routes · 74 airports · 795 flights · 3 not shown (unknown airport)"
private fun summary(network: RouteNetwork): String = buildList {
    add(plural(network.routes.size, "route"))
    add(plural(network.airports.size, "airport"))
    add(plural(network.flightsShown, "flight"))
    if (network.flightsWithoutPosition > 0) add("${network.flightsWithoutPosition} not shown (unknown airport)")
}.joinToString(" · ")

private fun plural(count: Int, word: String) = "%,d %s".format(count, if (count == 1) word else "${word}s")

// ---------------- The map ----------------

/** Where the map is looking: the centre in degrees, and how many pixels one degree is. */
@Stable
private class MapCamera {
    var centerLon by mutableDoubleStateOf(0.0)
    var centerLat by mutableDoubleStateOf(20.0)
    var scale by mutableFloatStateOf(0f)              // 0 until the map knows its size
    var viewport by mutableStateOf(IntSize.Zero)

    // The whole world's height, or its width, fills the screen: no zooming out past one world
    private val minScale: Float
        get() = if (viewport == IntSize.Zero) 1f else max(viewport.width / 360f, viewport.height / 180f)
    private val maxScale: Float get() = minScale * MAX_ZOOM

    val zoom: Float get() = scale / minScale

    fun fit(view: MapView?) {
        if (viewport == IntSize.Zero) return
        if (view == null) {
            centerLon = 0.0
            centerLat = 20.0
            scale = minScale
        } else {
            centerLon = view.centerLon
            centerLat = view.centerLat
            scale = min(viewport.width / view.spanLon.toFloat(), viewport.height / view.spanLat.toFloat())
        }
        clamp()
    }

    /** Zooms by [factor] keeping the map point at [anchor] (a screen position) under the fingers, then moves by [pan]. */
    fun transform(anchor: Offset, pan: Offset, factor: Float) {
        if (scale == 0f) return
        val newScale = (scale * factor).coerceIn(minScale, maxScale)
        val dx = anchor.x - viewport.width / 2f
        val dy = anchor.y - viewport.height / 2f
        val anchorLon = centerLon + dx / scale
        val anchorLat = centerLat - dy / scale
        centerLon = anchorLon - (dx + pan.x) / newScale
        centerLat = anchorLat + (dy + pan.y) / newScale
        scale = newScale
        clamp()
    }

    fun toScreen(lon: Double, lat: Double): Offset = Offset(
        ((lon - centerLon) * scale + viewport.width / 2f).toFloat(),
        ((centerLat - lat) * scale + viewport.height / 2f).toFloat()
    )

    private fun clamp() {
        scale = scale.coerceIn(minScale, maxScale)
        centerLon = MapFit.normalise(centerLon)           // sideways, the world repeats
        val halfHeight = viewport.height / 2.0 / scale    // up and down, it stops at the poles
        centerLat = if (halfHeight >= 90) 0.0 else centerLat.coerceIn(-90 + halfHeight, 90 - halfHeight)
    }
}

/** The world outline as two Paths in map units (x = longitude, y = minus latitude), built once. */
private class WorldPaths(val land: Path, val borders: Path) {
    companion object {
        fun of(world: WorldShapes) = WorldPaths(
            land = Path().apply { world.land.forEach { addShape(it, close = true) } },
            borders = Path().apply { world.borders.forEach { addShape(it, close = false) } }
        )
    }
}

private fun Path.addShape(points: FloatArray, close: Boolean) {
    if (points.size < 4) return
    moveTo(points[0], -points[1])
    var i = 2
    while (i < points.size) {
        lineTo(points[i], -points[i + 1])
        i += 2
    }
    if (close) close()
}

@Composable
private fun RouteMapCanvas(
    world: WorldShapes,
    network: RouteNetwork,
    arcs: List<Pair<MapRoute, FloatArray>>,
    view: MapView?,
    hub: String,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val camera = remember { MapCamera() }
    val worldPaths = remember(world) { WorldPaths.of(world) }
    val routePaths = remember(arcs) { arcs.map { (route, points) -> route to Path().apply { addShape(points, close = false) } } }

    // Colours from the theme, so the map suits FDV, Light and Dark
    val ocean = lerp(cs.background, cs.primary, 0.10f)
    val land = lerp(cs.surfaceVariant, cs.background, 0.25f)
    val coast = cs.outline.copy(alpha = 0.55f)
    val border = cs.outlineVariant.copy(alpha = 0.6f)
    val airportColor = cs.onSurface
    val hubColor = cs.primary

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = cs.onSurface, fontSize = 11.sp, shadow = Shadow(ocean, blurRadius = 6f))
    // Each label measured once, not every frame
    val labels = remember(network, labelStyle) { network.airports.associate { it.icao to textMeasurer.measure(it.icao, labelStyle) } }

    val touchRadius = with(LocalDensity.current) { 24.dp.toPx() }
    val maxVisits = max(1, network.maxVisits).toFloat()
    val maxFlights = max(1, network.maxFlights).toFloat()

    // Fit again whenever what's shown changes, or the space for the map does (rotating, unfolding)
    LaunchedEffect(view, camera.viewport) { camera.fit(view) }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .onSizeChanged { camera.viewport = it }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        // The point that was under the fingers stays under them
                        camera.transform(anchor = centroid - pan, pan = pan, factor = zoom)
                    }
                }
                .pointerInput(network) {
                    detectTapGestures(
                        onDoubleTap = { camera.transform(anchor = it, pan = Offset.Zero, factor = 2f) },
                        onTap = { tap -> onSelect(airportAt(network, camera, tap, touchRadius)?.icao) }
                    )
                }
        ) {
            drawRect(ocean)
            val s = camera.scale
            if (s == 0f) return@Canvas

            // The map repeats every 360°: copy k sits 360·k degrees to the east
            fun origin(k: Int) = Offset(
                ((360.0 * k - camera.centerLon) * s + size.width / 2).toFloat(),
                (camera.centerLat * s + size.height / 2).toFloat()
            )
            val halfWidthDegrees = size.width / 2f / s
            val copies = (-1..1).filter { k ->
                360.0 * k + 180 > camera.centerLon - halfWidthDegrees - 180 &&
                        360.0 * k - 180 < camera.centerLon + halfWidthDegrees + 180
            }

            // Land for every copy first, so one copy's land never covers another copy's routes
            for (k in copies) {
                val o = origin(k)
                withTransform({
                    translate(o.x, o.y)
                    scale(s, s, pivot = Offset.Zero)
                }) {
                    drawPath(worldPaths.land, land)
                    drawPath(worldPaths.land, coast, style = Stroke(width = 0.8.dp.toPx() / s))
                    drawPath(worldPaths.borders, border, style = Stroke(width = 0.6.dp.toPx() / s))
                }
            }

            for (k in copies) {
                val o = origin(k)
                withTransform({
                    translate(o.x, o.y)
                    scale(s, s, pivot = Offset.Zero)
                }) {
                    // Line widths are divided by the scale, so they stay the same on screen at any zoom
                    routePaths.forEach { (route, path) ->
                        val weight = sqrt(route.flights / maxFlights)
                        drawPath(
                            path = path,
                            color = DeltaRed.copy(alpha = 0.35f + 0.6f * weight),
                            style = Stroke(width = (1.2.dp.toPx() + 2.8.dp.toPx() * weight) / s, cap = StrokeCap.Round)
                        )
                    }
                    network.airports.forEach { airport ->
                        val weight = sqrt(airport.visits / maxVisits)
                        val radius = (2.5.dp.toPx() + 4.5.dp.toPx() * weight) / s
                        val centre = Offset(airport.position.lon.toFloat(), -airport.position.lat.toFloat())
                        if (airport.icao == hub) drawCircle(hubColor, radius * 1.9f, centre)
                        drawCircle(airportColor, radius, centre)
                        if (airport.icao == selected) {
                            drawCircle(hubColor, radius * 2.4f, centre, style = Stroke(width = 2.dp.toPx() / s))
                        }
                    }
                }
            }

            // Labels in screen space, so text stays the same size at any zoom
            val labelEverything = camera.zoom >= LABEL_ALL_ZOOM
            network.airports.forEachIndexed { index, airport ->
                if (!labelEverything && index >= LABELS_ALWAYS && airport.icao != selected) return@forEachIndexed
                val label = labels[airport.icao] ?: return@forEachIndexed
                for (k in copies) {
                    val p = camera.toScreen(airport.position.lon + 360.0 * k, airport.position.lat)
                    if (p.x < -60 || p.x > size.width + 60 || p.y < -20 || p.y > size.height + 20) continue
                    drawText(label, topLeft = Offset(p.x + 7.dp.toPx(), p.y - label.size.height / 2f))
                }
            }
        }

        IconButton(
            onClick = { camera.fit(view) },
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(Icons.Filled.ZoomOutMap, contentDescription = "Fit all routes", tint = cs.onSurface)
        }
    }
}

/** The airport nearest a tap, within finger reach, on any copy of the world. */
private fun airportAt(network: RouteNetwork, camera: MapCamera, tap: Offset, radius: Float): MapAirport? =
    network.airports
        .flatMap { airport ->
            (-1..1).map { k -> airport to (camera.toScreen(airport.position.lon + 360.0 * k, airport.position.lat) - tap).getDistance() }
        }
        .filter { (_, distance) -> distance <= radius }
        .minByOrNull { (_, distance) -> distance }
        ?.first

// ---------------- Airport card ----------------

@Composable
private fun AirportCard(
    airport: MapAirport,
    network: RouteNetwork,
    isHub: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val details = remember(airport.icao) { AirportRepository.getByIcao(airport.icao) }
    // Every airport flown to or from here, most flown first
    val connections = remember(network, airport.icao) {
        network.routes
            .filter { it.from == airport.icao || it.to == airport.icao }
            .map { (if (it.from == airport.icao) it.to else it.from) to it.flights }
            .sortedByDescending { it.second }
    }

    Column(
        modifier = modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .raised(raisedColors())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listOfNotNull(airport.icao, details?.name?.takeIf { it.isNotBlank() }).joinToString(" · "),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val place = listOfNotNull(
                    details?.city?.takeIf { it.isNotBlank() },
                    details?.country?.takeIf { it.isNotBlank() }
                ).joinToString(", ")
                if (place.isNotEmpty()) {
                    Text(place, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
        }
        Text(
            text = buildString {
                append(plural(airport.visits, "visit"))
                append(" · ")
                append(plural(connections.size, "destination"))
                if (isHub) append(" · your hub")
            },
            style = MaterialTheme.typography.bodyMedium
        )
        connections.take(3).forEach { (other, flights) ->
            Text(
                text = "$other · ${plural(flights, "flight")}",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
    }
}