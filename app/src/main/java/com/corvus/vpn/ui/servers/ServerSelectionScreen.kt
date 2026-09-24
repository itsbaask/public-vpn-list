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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvus.vpn.R
import com.corvus.vpn.ui.theme.getLatencyColor
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.sovereign_network_title), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isRefreshing) " ${stringResource(R.string.syncing_nodes)} " else " $totalServers ${stringResource(R.string.nodes_live)} ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                // Tier Selector Tab Row
                TabRow(
                    selectedTabIndex = when(selectedTier) {
                        "premium" -> 1
                        "premium_plus" -> 2
                        else -> 0
                    },
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTier == "free",
                        onClick = { selectedTier = "free" },
                        text = { Text(stringResource(R.string.tier_free), fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTier == "premium",
                        onClick = { selectedTier = "premium" },
                        text = { Text(stringResource(R.string.tier_premium), fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTier == "premium_plus",
                        onClick = { selectedTier = "premium_plus" },
                        text = { Text(stringResource(R.string.tier_premium_plus), fontWeight = FontWeight.Bold) }
                    )
                }

                // Continent Tabs
                ScrollableTabRow(
                    selectedTabIndex = continents.indexOf(selectedContinent).coerceAtLeast(0),
                    containerColor = MaterialTheme.colorScheme.background,
                    edgePadding = 20.dp,
                    indicator = {},
                    divider = {}
                ) {
                    continents.forEach { continent ->
                        Tab(
                            selected = selectedContinent == continent,
                            onClick = { selectedContinent = continent },
                            text = {
                                Text(
                                    continent,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selectedContinent == continent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
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
                .padding(paddingValues)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                placeholder = { Text(stringResource(R.string.search_placeholder), color = MaterialTheme.colorScheme.secondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            if (selectedTier == "free") {
                Surface(
                    onClick = onProClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ultra Fast Premium Nodes. Upgrade to Unlock All!",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
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
                    tint = MaterialTheme.colorScheme.primary,
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
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.rotate(rotation),
                tint = MaterialTheme.colorScheme.secondary
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
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.best_automatic),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = server.protocol.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Encrypted Line",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        if (server.speed != null && server.speed > 0) {
            Text(
                text = "${server.speed} Mbps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
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
        SignalIndicator(signal = server.signal, ping = server.ping)
    }
}

@Composable
fun SignalIndicator(signal: Int, ping: Int?) {
    val barColor = getLatencyColor(ping)
    Row(verticalAlignment = Alignment.Bottom) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((6 + (index * 4)).dp)
                    .background(
                        if (index < signal) barColor
                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
    }
}
