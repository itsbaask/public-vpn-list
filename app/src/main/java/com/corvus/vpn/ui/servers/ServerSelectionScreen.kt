package com.corvus.vpn.ui.servers

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.sovereign_network_title),
                                fontWeight = FontWeight.Bold,
                                color = CrowText,
                                fontSize = 18.sp
                            )
                            Surface(
                                color = CrowAccentBorder,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isRefreshing) " ${stringResource(R.string.syncing_nodes)} " else " $totalServers ${stringResource(R.string.nodes_live)} ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrowAccentText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

                // Tier Selector Tab Row
                TabRow(
                    selectedTabIndex = when(selectedTier) {
                        "premium" -> 1
                        "premium_plus" -> 2
                        else -> 0
                    },
                    containerColor = CrowBlack,
                    contentColor = CrowAccent
                ) {
                    Tab(
                        selected = selectedTier == "free",
                        onClick = { selectedTier = "free" },
                        text = { Text(stringResource(R.string.tier_free), fontWeight = FontWeight.Bold, color = if (selectedTier == "free") CrowAccent else CrowMuted) }
                    )
                    Tab(
                        selected = selectedTier == "premium",
                        onClick = { selectedTier = "premium" },
                        text = { Text(stringResource(R.string.tier_premium), fontWeight = FontWeight.Bold, color = if (selectedTier == "premium") CrowAccent else CrowMuted) }
                    )
                    Tab(
                        selected = selectedTier == "premium_plus",
                        onClick = { selectedTier = "premium_plus" },
                        text = { Text(stringResource(R.string.tier_premium_plus), fontWeight = FontWeight.Bold, color = if (selectedTier == "premium_plus") CrowAccent else CrowMuted) }
                    )
                }

                // Continent Tabs
                ScrollableTabRow(
                    selectedTabIndex = continents.indexOf(selectedContinent).coerceAtLeast(0),
                    containerColor = CrowBlack,
                    edgePadding = 20.dp,
                    indicator = {},
                    divider = {}
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
                                        "Europe" -> stringResource(R.string.continent_europe)
                                        "Asia" -> stringResource(R.string.continent_asia)
                                        "America" -> stringResource(R.string.continent_america)
                                        "Africa" -> stringResource(R.string.continent_africa)
                                        "Oceania" -> stringResource(R.string.continent_oceania)
                                        else -> stringResource(R.string.continent_other)
                                    }
                                }
                                Text(
                                    text = continentText,
                                    style = MaterialTheme.typography.labelLarge,
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
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                placeholder = { Text(stringResource(R.string.search_placeholder), color = CrowMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CrowMuted) },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = CrowSurface,
                    unfocusedContainerColor = CrowSurface,
                    focusedTextColor = CrowText,
                    unfocusedTextColor = CrowText
                )
            )

            if (selectedTier == "free") {
                Surface(
                    onClick = onProClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = CrowSurface,
                    border = BorderStroke(1.dp, CrowAccentBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_custom_lock),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.ultra_fast_premium_nodes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CrowAccentText,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                if (searchQuery.isBlank() && selectedContinent == "All") {
                    item {
                        BestAutomaticRow(onClick = onSelectBest)
                    }
                }

                val displayedCountries = servers.keys.filter { countryName ->
                    val serverList = (servers[countryName] ?: emptyList()).filter { 
                        it.tier == selectedTier || (selectedTier == "free" && (it.tier.isBlank() || it.tier == "free"))
                    }
                    if (serverList.isEmpty()) return@filter false

                    val continent = CountryUtils.getContinent(serverList.firstOrNull()?.countryCode ?: "UN")
                    val matchesContinent = selectedContinent == "All" || continent == selectedContinent
                    val matchesSearch = countryName.contains(searchQuery, ignoreCase = true) ||
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
                            name = country,
                            flag = CountryUtils.getFlagEmoji(serverList.firstOrNull()?.countryCode ?: "UN"),
                            isExpanded = isExpanded,
                            onClick = { expandedStates[country] = !isExpanded }
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
fun CountryHeader(name: String, flag: String, isExpanded: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "arrow_rotation")

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (flag == "🌐" || flag.isEmpty()) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = CrowAccent,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(text = flag, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = CrowText,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.rotate(rotation),
                tint = CrowMuted
            )
        }
    }
}

@Composable
fun BestAutomaticRow(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = CrowSurface,
        border = BorderStroke(
            width = 1.dp,
            color = CrowBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(CrowAccentBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = CrowAccentText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.best_automatic),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = CrowText
            )
        }
    }
}

@Composable
fun ServerRow(server: Server, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 48.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = CrowText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.encrypted_line),
                style = MaterialTheme.typography.labelSmall,
                color = CrowMuted
            )
        }

        if (server.speed != null && server.speed > 0) {
            Text(
                text = "${server.speed} Mbps",
                style = MaterialTheme.typography.labelSmall,
                color = CrowAccent,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (server.ping != null) {
            Text(
                text = "${server.ping}ms",
                style = MaterialTheme.typography.labelSmall,
                color = getLatencyColor(server.ping),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
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
                    .height((6 + (index * 4)).dp)
                    .background(
                        if (index < signal) CrowAccent
                        else CrowMuted.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
    }
}
