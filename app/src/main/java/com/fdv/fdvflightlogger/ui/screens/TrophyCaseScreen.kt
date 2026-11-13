package com.fdv.fdvflightlogger.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.fdv.fdvflightlogger.data.fdv.AwardKind
import com.fdv.fdvflightlogger.data.fdv.FdvAward
import com.fdv.fdvflightlogger.ui.AppViewModel
import com.fdv.fdvflightlogger.ui.theme.inset
import com.fdv.fdvflightlogger.ui.theme.raised
import com.fdv.fdvflightlogger.ui.theme.raisedColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// FDV's trophy rarities, lowest to highest; anything unknown goes on a shelf at the end
private val RARITY_ORDER = listOf(
    "Common", "Uncommon", "Rare", "Very Rare", "Extremely Rare",
    "Highly Sought", "Extremely Coveted", "Priceless", "Legendary", "Ultimate"
)

private val EarnedDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

// Greyscale for locked artwork
private val Locked = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrophyCaseScreen(appViewModel: AppViewModel, navController: NavController) {
    val awards by appViewModel.awards.collectAsStateWithLifecycle()
    val loading by appViewModel.awardsLoading.collectAsStateWithLifecycle()
    val session by appViewModel.fdvSession.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showLocked by rememberSaveable { mutableStateOf(true) }
    var selected by remember { mutableStateOf<FdvAward?>(null) }

    // Loads on first visit; the refresh button reloads on demand
    LaunchedEffect(Unit) { if (awards == null) appViewModel.loadAwards() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trophy Case") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(22.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { appViewModel.loadAwards() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            val all = awards
            val awardItems = all.orEmpty().filter { it.kind == AwardKind.AWARD }
            val trophyItems = all.orEmpty().filter { it.kind == AwardKind.TROPHY }

            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("Awards · ${awardItems.count { it.earned }}") }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("Trophies · ${trophyItems.count { it.earned }}") }
                )
            }

            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                FilterChip(
                    selected = showLocked,
                    onClick = { showLocked = !showLocked },
                    label = { Text("Show locked") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when {
                all == null && loading -> CenterMessage { CircularProgressIndicator() }
                all == null -> CenterMessage {
                    Text("Your trophy case couldn't be loaded.", style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { appViewModel.loadAwards() }) { Text("Try again") }
                }
                tab == 0 -> AwardsWall(
                    awards = awardItems.filter { showLocked || it.earned },
                    token = session?.token,
                    onOpen = { selected = it }
                )
                else -> TrophyShelves(
                    trophies = trophyItems.filter { showLocked || it.earned },
                    onOpen = { selected = it }
                )
            }
        }
    }

    selected?.let { item ->
        AwardDialog(item = item, token = session?.token, onDismiss = { selected = null })
    }
}

// ---------------- Awards wall ----------------

@Composable
private fun AwardsWall(awards: List<FdvAward>, token: String?, onOpen: (FdvAward) -> Unit) {
    // "group flight" → "Group flight"; sections in alphabetical order
    val sections = awards
        .groupBy { it.category?.replaceFirstChar(Char::titlecase) ?: "Other" }
        .toSortedMap()

    LazyVerticalGrid(
        // As many 150 dp columns as fit: 2 on a phone, 5 or 6 on your tablet
        columns = GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        sections.forEach { (category, items) ->
            // A header that spans the whole row
            item(key = "header-$category", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(title = category, earned = items.count { it.earned }, total = items.size)
            }
            items(
                items = items.sortedWith(compareByDescending<FdvAward> { it.earned }.thenBy { it.name }),
                key = { "award-${it.id}" }
            ) { award ->
                AwardPlaque(award = award, token = token, onClick = { onOpen(award) })
            }
        }
    }
}

@Composable
private fun AwardPlaque(award: FdvAward, token: String?, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .alpha(if (award.earned) 1f else 0.55f)
            .fillMaxWidth()
            .raised(raisedColors())
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // The artwork sits in a sunken frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .inset(raisedColors())
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            AwardArt(award = award, token = token, modifier = Modifier.fillMaxSize())
            if (!award.earned) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Locked",
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.TopEnd).size(18.dp)
                )
            }
        }
        Text(
            text = award.name,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = award.earnedAtMillis?.let { "Earned ${formatDate(it)}" } ?: award.requirement ?: "Not yet earned",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** FDV's artwork for an award, greyed out while locked; a medal icon when there's none or it won't load. */
@Composable
private fun AwardArt(award: FdvAward, token: String?, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val medal = rememberVectorPainter(Icons.Filled.WorkspacePremium)
    val url = award.imageUrl

    if (url == null) {
        Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = cs.primary, modifier = modifier.padding(16.dp))
        return
    }

    val context = LocalPlatformContext.current
    val request = remember(url, token) {
        ImageRequest.Builder(context)
            .data(url)
            .apply {
                // Award art lives under FDV's /private/ path: send the pilot's token, but only to FDV itself
                if (token != null && Uri.parse(url).host == "flydeltavirtual.com") {
                    httpHeaders(NetworkHeaders.Builder().set("Authorization", "Bearer $token").build())
                }
            }
            .crossfade(true)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = award.name,
        contentScale = ContentScale.Fit,
        placeholder = medal,
        error = medal,
        colorFilter = if (award.earned) null else Locked,
        modifier = modifier
    )
}

// ---------------- Trophy shelves ----------------

@Composable
private fun TrophyShelves(trophies: List<FdvAward>, onOpen: (FdvAward) -> Unit) {
    val shelves = trophies
        .groupBy { it.category ?: "Other" }
        .toList()
        .sortedBy { (rarity, _) -> RARITY_ORDER.indexOf(rarity).let { if (it < 0) Int.MAX_VALUE else it } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(shelves, key = { it.first }) { (rarity, items) ->
            Shelf(
                rarity = rarity,
                trophies = items.sortedWith(compareByDescending<FdvAward> { it.earned }.thenBy { it.name }),
                onOpen = onOpen
            )
        }
    }
}

@Composable
private fun Shelf(rarity: String, trophies: List<FdvAward>, onOpen: (FdvAward) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val color = rarityColor(rarity)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(8.dp))
            Text(rarity, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                "${trophies.count { it.earned }} of ${trophies.size}",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }

        // One board under each row of trophies, however many fit across this screen
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val perRow = (maxWidth / TROPHY_SLOT).toInt().coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                trophies.chunked(perRow).forEach { row ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { trophy -> TrophyItem(trophy, color, onClick = { onOpen(trophy) }) }
                            // Empty slots keep a short last row aligned with the rows above
                            repeat(perRow - row.size) { Spacer(modifier = Modifier.width(TROPHY_SLOT)) }
                        }
                        ShelfBoard()
                    }
                }
            }
        }
    }
}

private val TROPHY_SLOT = 100.dp

@Composable
private fun TrophyItem(trophy: FdvAward, color: Color, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val tint = if (trophy.earned) color else cs.onSurfaceVariant

    Column(
        modifier = Modifier
            .width(TROPHY_SLOT)
            .alpha(if (trophy.earned) 1f else 0.4f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(56.dp)
                    // A soft glow in the rarity's colour behind earned trophies
                    .drawBehind {
                        if (trophy.earned) {
                            drawCircle(
                                Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent)),
                                radius = size.minDimension * 0.75f
                            )
                        }
                    }
            )
            if (trophy.timesEarned > 1) {
                Text(
                    text = "×${trophy.timesEarned}",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(50))
                        .background(cs.primary)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
        Text(
            text = trophy.name,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** The shelf itself: a polished bar with a highlight on its front edge. */
@Composable
private fun ShelfBoard() {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Brush.verticalGradient(listOf(cs.outline, cs.surfaceVariant, cs.outlineVariant)))
    )
}

// Bronze through gold and beyond, so a shelf's colour says how rare it is at a glance
private fun rarityColor(rarity: String): Color = when (rarity) {
    "Common" -> Color(0xFFB08D57)
    "Uncommon" -> Color(0xFFA7B1BC)
    "Rare" -> Color(0xFF4F8DDB)
    "Very Rare" -> Color(0xFF7B5BD6)
    "Extremely Rare" -> Color(0xFFC04BCB)
    "Highly Sought" -> Color(0xFFE07A2E)
    "Extremely Coveted" -> Color(0xFFE0B12E)
    "Priceless" -> Color(0xFF2EC4B6)
    "Legendary" -> Color(0xFFFFD54F)
    "Ultimate" -> Color(0xFFFF5A5A)
    else -> Color(0xFF8A94A6)
}

// ---------------- Shared ----------------

@Composable
private fun SectionHeader(title: String, earned: Int, total: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(
            "$earned of $total",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CenterMessage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { content() }
}

@Composable
private fun AwardDialog(item: FdvAward, token: String?, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            if (item.kind == AwardKind.AWARD) {
                AwardArt(award = item, token = token, modifier = Modifier.size(120.dp))
            } else {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = if (item.earned) rarityColor(item.category.orEmpty()) else cs.onSurfaceVariant,
                    modifier = Modifier.size(72.dp)
                )
            }
        },
        title = { Text(item.name, textAlign = TextAlign.Center) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.category?.let {
                    Text(
                        text = (if (item.kind == AwardKind.TROPHY) "$it trophy" else "${it.replaceFirstChar(Char::titlecase)} award"),
                        style = MaterialTheme.typography.labelLarge,
                        color = cs.secondary
                    )
                }
                if (item.description.isNotEmpty()) Text(item.description)
                item.requirement?.let {
                    Text("Requirement: $it", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
                Text(
                    text = when {
                        !item.earned -> "Not earned yet"
                        item.timesEarned > 1 -> "Earned ${item.timesEarned} times, most recently ${item.earnedAtMillis?.let(::formatDate) ?: ""}"
                        else -> "Earned ${item.earnedAtMillis?.let(::formatDate) ?: ""}"
                    }.trim(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(EarnedDate)