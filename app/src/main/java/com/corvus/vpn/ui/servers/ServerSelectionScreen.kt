package com.corvus.vpn.ui.servers

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvus.vpn.R
import com.corvus.vpn.ui.theme.*
import com.corvus.vpn.util.CountryUtils

data class Server(
    val id: String,
    val protocol: String,
    val engine: String,
    val name: String,
    val countryCode: String?,
    val countryName: String?,
    val ping: Int?,
    val signal: Int,
    val speed: Long? = null,
    val configUri: String? = null,
    val ovpnConfig: String? = null,
    val tier: String = "free"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSelectionScreen(
    servers: Map<String, List<Server>>,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onServerSelect: (Server) -> Unit,
    onSelectBest: () -> Unit,
    onProClick: () -> Unit = {},
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    val totalServers = remember(servers) { servers.values.sumOf { it.size } }

    val continentGrouped = remember(servers) {
        val flatList = servers.values.flatten()
        flatList.groupBy { server ->
            CountryUtils.getContinent(server.countryCode ?: "UN")
        }
    }

    val continents = remember(continentGrouped) {
        listOf("All") + continentGrouped.keys.sorted()
    }

    var selectedContinent by remember { mutableStateOf("All") }
    var selectedTier by remember { mutableStateOf("free") }

    Scaffold(
        containerColor = CrowBlack,
        topBar = {
            Column(modifier = Modifier.background(CrowBlack)) {
                // Top app bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.sovereign_network_title),
                                fontWeight = FontWeight.Bold,
                                color = CrowText,
                                fontSize = 17.sp,
                                letterSpacing = (-0.3).sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (isRefreshing) CrowGold else CrowAccent)
                                )
                                Text(
                                    text = if (isRefreshing)
                                        " ${stringResource(R.string.syncing_nodes)}"
                                    else
                                        "$totalServers ${stringResource(R.string.nodes_live)}",
                                    fontSize = 11.sp,
                                    color = if (isRefreshing) CrowGold else CrowAccent,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_custom_back),
                                contentDescription = "Back",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CrowBlack
                    )
                )

                // Tier selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("free", "premium", "premium_plus").forEachIndexed { index, tier ->
                        val label = when (tier) {
                            "premium"      -> stringResource(R.string.tier_premium)
                            "premium_plus" -> stringResource(R.string.tier_premium_plus)
                            else           -> stringResource(R.string.tier_free)
                        }
                        val isSelected = selectedTier == tier
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) CrowAccent.copy(alpha = 0.12f) else CrowSurface)
                                .border(
                                    0.5.dp,
                                    if (isSelected) CrowAccent.copy(alpha = 0.5f) else CrowBorderMid,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTier = tier },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CrowAccent else CrowTextSub
                            )
                        }
                    }
                }

                // Continent tabs
                ScrollableTabRow(
                    selectedTabIndex = continents.indexOf(selectedContinent).coerceAtLeast(0),
                    containerColor = CrowBlack,
                    contentColor = CrowAccent,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        val idx = continents.indexOf(selectedContinent).coerceAtLeast(0)
                        if (idx < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[idx]),
                                color = CrowAccent,
                                height = 1.5.dp
                            )
                        }
                    },
                    divider = {
                        HorizontalDivider(color = CrowBorder, thickness = 0.5.dp)
                    }
                ) {
                    continents.forEach { continent ->
                        Tab(
                            selected = selectedContinent == continent,
                            onClick = { selectedContinent = continent },
                            text = {
                                val continentText = if (continent == "All") {
                                    stringResource(R.string.all_filter)
                                } else {
                                    when (continent) {
                                        "Europe"  -> stringResource(R.string.continent_europe)
                                        "Asia"    -> stringResource(R.string.continent_asia)
                                        "America" -> stringResource(R.string.continent_america)
                                        "Africa"  -> stringResource(R.string.continent_africa)
                                        "Oceania" -> stringResource(R.string.continent_oceania)
                                        else      -> stringResource(R.string.continent_other)
                                    }
                                }
                                Text(
                                    text = continentText,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedContinent == continent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedContinent == continent) CrowAccent else CrowMuted
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CrowBlack)
                .padding(paddingValues)
        ) {
            // Search field
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                placeholder = {
                    Text(
                        stringResource(R.string.search_placeholder),
                        color = CrowMuted,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = CrowMuted, modifier = Modifier.size(18.dp))
                },
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor   = CrowSurface,
                    unfocusedContainerColor = CrowSurface,
                    focusedTextColor        = CrowText,
                    unfocusedTextColor      = CrowText
                ),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
            )

            // Premium upsell banner (only on free tab)
            if (selectedTier == "free") {
                Surface(
                    onClick = onProClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = CrowAccentGlow,
                    border = BorderStroke(0.5.dp, CrowAccent.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bolt,
                            contentDescription = null,
                            tint = CrowAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ultra_fast_premium_nodes),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CrowAccentText,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = CrowAccent.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(270f)
                        )
                    }
                }
            }

            // Server list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (searchQuery.isBlank() && selectedContinent == "All") {
                    item { BestAutomaticRow(onClick = onSelectBest) }
                }

                val displayedCountries = servers.keys.filter { countryName ->
                    val serverList = (servers[countryName] ?: emptyList()).filter {
                        it.tier == selectedTier || (selectedTier == "free" && (it.tier.isBlank() || it.tier == "free"))
                    }
                    if (serverList.isEmpty()) return@filter false

                    val continent = CountryUtils.getContinent(serverList.firstOrNull()?.countryCode ?: "UN")
                    val matchesContinent = selectedContinent == "All" || continent == selectedContinent
                    val matchesSearch    = countryName.contains(searchQuery, ignoreCase = true) ||
                            serverList.any { it.name.contains(searchQuery, ignoreCase = true) }

                    matchesContinent && matchesSearch
                }.sortedBy { it }

                displayedCountries.forEach { country ->
                    val serverList = (servers[country] ?: emptyList()).filter {
                        it.tier == selectedTier || (selectedTier == "free" && (it.tier.isBlank() || it.tier == "free"))
                    }
                    val isExpanded = expandedStates[country] ?: false

                    item(key = "header_$country") {
                        CountryHeader(
                            name       = country,
                            flag       = CountryUtils.getFlagEmoji(serverList.firstOrNull()?.countryCode ?: "UN"),
                            serverCount = serverList.size,
                            isExpanded = isExpanded,
                            onClick    = { expandedStates[country] = !isExpanded }
                        )
                    }

                    if (isExpanded || searchQuery.isNotBlank()) {
                        items(serverList, key = { it.id }) { server ->
                            ServerRow(server = server, onClick = { onServerSelect(server) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CountryHeader(
    name: String,
    flag: String,
    serverCount: Int = 0,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "arrow_rotation")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (flag == "🌐" || flag.isEmpty()) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = CrowTextSub,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(text = flag, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = CrowText,
            modifier = Modifier.weight(1f)
        )
        if (serverCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CrowSurface)
                    .border(0.5.dp, CrowBorderMid, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$serverCount",
                    fontSize = 10.sp,
                    color = CrowTextSub,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.rotate(rotation).size(18.dp),
            tint = CrowMuted
        )
    }
}

@Composable
fun BestAutomaticRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CrowSurface)
            .border(0.5.dp, CrowBorderMid, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CrowAccentGlow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = CrowAccent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.best_automatic),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CrowText
            )
            Text(
                text = "AI-optimized selection",
                fontSize = 11.sp,
                color = CrowTextSub
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = CrowMuted,
            modifier = Modifier.rotate(270f).size(16.dp)
        )
    }
}

@Composable
fun ServerRow(server: Server, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 54.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = CrowText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.encrypted_line),
                fontSize = 11.sp,
                color = CrowMuted
            )
        }

        if (server.speed != null && server.speed > 0) {
            Text(
                text = "${server.speed}M",
                fontSize = 11.sp,
                color = CrowAccent,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (server.ping != null) {
            Text(
                text = "${server.ping}ms",
                fontSize = 11.sp,
                color = getLatencyColor(server.ping),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        SignalIndicator(signal = server.signal)
    }
}

@Composable
fun SignalIndicator(signal: Int) {
    Row(verticalAlignment = Alignment.Bottom) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((5 + (index * 4)).dp)
                    .background(
                        if (index < signal) CrowAccent.copy(alpha = 0.9f)
                        else CrowMuted.copy(alpha = 0.18f),
                        shape = CircleShape
                    )
            )
            if (index < 2) Spacer(modifier = Modifier.width(2.dp))
        }
    }
}
