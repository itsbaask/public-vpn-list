package com.corvus.vpn.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvus.vpn.R
import com.corvus.vpn.ui.components.CorvusStatusRing
import com.corvus.vpn.ui.theme.*

@Composable
fun HomeScreen(
    isConnected: Boolean,
    isConnecting: Boolean,
    serverName: String,
    countryCode: String,
    serverTier: String = "free",
    selectedMode: String = "smart",
    onModeChange: (String) -> Unit = {},
    duration: String,
    ping: String,
    downloadSpeed: String,
    uploadSpeed: String,
    unityAdsManager: com.corvus.vpn.ads.UnityAdsManager? = null,
    onToggleClick: () -> Unit,
    onServerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProClick: () -> Unit,
    onAddTimeClick: (Int) -> Unit
) {
    var showModeDialog by remember { mutableStateOf(false) }

    if (showModeDialog) {
        AlertDialog(
            onDismissRequest = { showModeDialog = false },
            containerColor = CyberSurfaceDark,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Protocol & Mode",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryWhite,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val modes = listOf(
                        "smart" to ("Smart Mode" to "Auto selection & secure split tunneling"),
                        "openvpn" to ("OpenVPN" to "High security OpenVPN protocol"),
                        "wireguard" to ("WireGuard" to "Ultra-fast WireGuard protocol")
                    )
                    modes.forEach { (key, info) ->
                        val isSelected = selectedMode == key
                        Surface(
                            onClick = {
                                onModeChange(key)
                                showModeDialog = false
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) ElectricCyan.copy(alpha = 0.12f) else CyberSurfaceVariant,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) ElectricCyan else Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onModeChange(key)
                                        showModeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = info.first,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryWhite,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = info.second,
                                        color = TextSecondaryMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModeDialog = false }) {
                    Text("Close", color = ElectricCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDarkObsidian)
    ) {
        // High-Tech Cyber Ambient Gradient Background
        val stateColor = when {
            isConnected -> ElectricNeonGreen
            isConnecting -> GlowingAmber
            else -> ElectricCyan
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.42f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            stateColor.copy(alpha = 0.16f),
                            stateColor.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(CyberDarkObsidian),
                    contentAlignment = Alignment.Center
                ) {
                    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
                    if (activity != null && unityAdsManager != null) {
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = {
                                unityAdsManager.createBannerView(activity)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                val promoMessages = remember {
                    listOf(
                        "🔥 Save 87% • Unlock VIP Speed",
                        "⚡ Ultra-Fast 10Gbps Servers",
                        "🛡️ 100% Zero-Logs & Privacy",
                        "🚀 10x Faster Gaming & Streaming",
                        "👑 Access All VIP Locations",
                        "💎 Premium Anti-Track Protection",
                        "🔥 87% OFF • Limited Time Offer",
                        "⚡ Instant HD & 4K Streaming",
                        "🔒 Military-Grade Encryption",
                        "✨ Try VIP Premium Today"
                    )
                }

                var currentMessageIndex by remember { mutableIntStateOf(0) }

                LaunchedEffect(Unit) {
                    while (true) {
                        delay(2800L)
                        currentMessageIndex = (currentMessageIndex + 1) % promoMessages.size
                    }
                }

                // World-Class Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Minimalist Settings Button
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CyberSurfaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimaryWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Dynamic VIP Marquee Offer Capsule
                    Surface(
                        onClick = onProClick,
                        shape = RoundedCornerShape(20.dp),
                        color = CyberSurfaceDark,
                        border = BorderStroke(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(ElectricCyan, PureGold)
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.logo_no_bg),
                                    contentDescription = "Corvus VIP Logo",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "VIP",
                                    color = PureGold,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            AnimatedContent(
                                targetState = promoMessages[currentMessageIndex],
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(350)) + slideInVertically { height -> height })
                                        .togetherWith(fadeOut(animationSpec = tween(350)) + slideOutVertically { height -> -height })
                                },
                                label = "promo_text_animation"
                            ) { message ->
                                Text(
                                    text = message,
                                    color = TextPrimaryWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.8f))

                // High-Tech Connection Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = stateColor.copy(alpha = 0.12f),
                    border = BorderStroke(width = 1.dp, color = stateColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(stateColor)
                        )
                        Text(
                            text = when {
                                isConnected -> "PROTECTED"
                                isConnecting -> "CONNECTING..."
                                else -> "DISCONNECTED"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = stateColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Hero Power Toggle Ring Switch
                CorvusStatusRing(
                    isConnected = isConnected,
                    isConnecting = isConnecting,
                    onClick = onToggleClick
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Connection Duration Display
                Text(
                    text = duration,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 38.sp,
                        letterSpacing = 1.sp,
                        color = TextPrimaryWhite
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                // High-Tech Action Cards (Mode Selector & Free Premium Ad)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Card: Protocol / Mode Selector
                    Surface(
                        onClick = { showModeDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = CyberSurfaceDark,
                        border = BorderStroke(width = 1.dp, color = CyberBorderStroke)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = when (selectedMode) {
                                    "wireguard" -> "WireGuard"
                                    "openvpn" -> "OpenVPN"
                                    else -> "Smart Mode"
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryWhite
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextSecondaryMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Right Card: Free Premium Ad Button
                    Surface(
                        onClick = { onAddTimeClick(20) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = CyberSurfaceDark,
                        border = BorderStroke(width = 1.dp, color = ElectricCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Free Premium",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.OndemandVideo,
                                contentDescription = "Watch Ad",
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Full-Width Server Selector Card Tile
                Surface(
                    onClick = onServerClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CyberSurfaceDark,
                    border = BorderStroke(width = 1.dp, color = CyberBorderStroke)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (countryCode == "🌐" || countryCode.isEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(text = countryCode, fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = serverName,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryWhite
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = ping,
                            fontSize = 12.sp,
                            color = ElectricNeonGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondaryMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
