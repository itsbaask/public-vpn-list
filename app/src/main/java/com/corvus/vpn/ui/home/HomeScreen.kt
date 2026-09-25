package com.corvus.vpn.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvus.vpn.R
import com.corvus.vpn.ui.theme.*

enum class ConnectionState { Disconnected, Connecting, Connected }

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
    downloadSpeed: String = "0.0 Mbps",
    uploadSpeed: String = "0.0 Mbps",
    unityAdsManager: com.corvus.vpn.ads.UnityAdsManager? = null,
    onToggleClick: () -> Unit,
    onServerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProClick: () -> Unit,
    onAddTimeClick: (Int) -> Unit
) {
    var showModeDialog by remember { mutableStateOf(false) }

    val connectionState = when {
        isConnected -> ConnectionState.Connected
        isConnecting -> ConnectionState.Connecting
        else -> ConnectionState.Disconnected
    }

    if (showModeDialog) {
        AlertDialog(
            onDismissRequest = { showModeDialog = false },
            containerColor = CrowSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = stringResource(R.string.protocol_mode_title),
                    fontWeight = FontWeight.Bold,
                    color = CrowText,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val modes = listOf(
                        "smart" to Pair(stringResource(R.string.smart_mode), stringResource(R.string.smart_mode_desc)),
                        "openvpn" to Pair(stringResource(R.string.openvpn_mode), stringResource(R.string.openvpn_mode_desc)),
                        "wireguard" to Pair(stringResource(R.string.wireguard_mode), stringResource(R.string.wireguard_mode_desc))
                    )
                    modes.forEach { (key, info) ->
                        val isSelected = selectedMode == key
                        Surface(
                            onClick = {
                                onModeChange(key)
                                showModeDialog = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) CrowAccentGlow.copy(alpha = 0.25f) else CrowSurface,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) CrowAccent else CrowBorder
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
                                    colors = RadioButtonDefaults.colors(selectedColor = CrowAccent)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = info.first,
                                        fontWeight = FontWeight.Bold,
                                        color = CrowText,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = info.second,
                                        color = CrowMuted,
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
                    Text(stringResource(R.string.close_button), color = CrowAccent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        containerColor = CrowBlack,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(CrowBlack),
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
                .background(CrowBlack)
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            TopBar(onOpenSettings = onSettingsClick, onOpenPro = onProClick)

            Spacer(Modifier.weight(1f))

            StatusPill(connectionState)
            Spacer(Modifier.height(36.dp))
            ConnectButton(state = connectionState, onClick = onToggleClick)
            Spacer(Modifier.height(32.dp))
            Text(
                text = duration,
                color = CrowText,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val wireguardLabel = stringResource(R.string.wireguard_mode)
                val openvpnLabel = stringResource(R.string.openvpn_mode)
                val smartLabel = stringResource(R.string.smart_mode)
                val freePremiumLabel = stringResource(R.string.free_premium_label)

                ActionChip(
                    label = when (selectedMode) {
                        "wireguard" -> wireguardLabel
                        "openvpn" -> openvpnLabel
                        else -> smartLabel
                    },
                    trailing = Icons.Outlined.ChevronRight,
                    highlighted = false,
                    onClick = { showModeDialog = true },
                    modifier = Modifier.weight(1f),
                )
                ActionChip(
                    label = freePremiumLabel,
                    customIconRes = R.drawable.ic_footage_media,
                    highlighted = true,
                    onClick = { onAddTimeClick(20) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            LocationRow(
                serverName = serverName,
                countryCode = countryCode,
                ping = ping,
                connected = isConnected,
                onClick = onServerClick,
            )

            Spacer(Modifier.height(24.dp))
            Text(
                text = "CORVUS",
                color = CrowMuted.copy(alpha = 0.8f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 12.sp,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TopBar(onOpenSettings: () -> Unit, onOpenPro: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(CrowSurface)
                .border(1.dp, CrowBorder, CircleShape)
                .clickable(role = Role.Button, onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = CrowMuted,
                modifier = Modifier.size(20.dp),
            )
        }

    val infiniteTransition = rememberInfiniteTransition(label = "vip_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = Modifier
            .graphicsLayer(alpha = glowAlpha)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(10.dp), clip = false)
            .clickable(role = Role.Button, onClick = onOpenPro)
    ) {
        Image(
            painter = painterResource(id = R.drawable.vip_card_raven),
            contentDescription = "VIP Badge",
            modifier = Modifier
                .width(68.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    }
    }
}

@Composable
private fun StatusPill(state: ConnectionState) {
    val on = state != ConnectionState.Disconnected
    val border by animateColorAsState(if (on) CrowRingOuterOn else CrowBorder, tween(400), label = "pillBorder")
    val dot by animateColorAsState(if (on) CrowAccent else CrowDotOff, tween(400), label = "pillDot")
    val text by animateColorAsState(if (on) CrowAccentText else CrowMuted, tween(400), label = "pillText")

    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(CrowSurface)
            .border(1.dp, border, CircleShape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        val notConnectedText = stringResource(R.string.status_disconnected)
        val connectingText = stringResource(R.string.status_connecting)
        val connectedText = stringResource(R.string.status_connected)

        Text(
            text = when (state) {
                ConnectionState.Disconnected -> notConnectedText
                ConnectionState.Connecting -> connectingText
                ConnectionState.Connected -> connectedText
            },
            color = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Crow-head logo inside two concentric rings. Logo is tinted per state. */
@Composable
private fun ConnectButton(state: ConnectionState, onClick: () -> Unit) {
    val on = state != ConnectionState.Disconnected
    val connected = state == ConnectionState.Connected

    val outerRing by animateColorAsState(if (on) CrowRingOuterOn else CrowRingOuterOff, tween(500), label = "outer")
    val innerRing by animateColorAsState(if (on) CrowRingInnerOn else CrowRingInnerOff, tween(500), label = "inner")
    val buttonRing by animateColorAsState(if (on) CrowAccent else CrowButtonRingOff, tween(500), label = "btn")
    val logoTint by animateColorAsState(if (connected) CrowLogoOn else CrowLogoOff, tween(500), label = "logo")
    val glow by animateColorAsState(if (connected) CrowAccentGlow else Color.Transparent, tween(500), label = "glow")

    Box(
        modifier = Modifier.size(268.dp).border(1.dp, outerRing, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(222.dp).border(1.dp, innerRing, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(176.dp)
                    .shadow(28.dp, CircleShape, ambientColor = glow, spotColor = glow)
                    .clip(CircleShape)
                    .background(CrowCore)
                    .border(if (on) 2.dp else 1.5.dp, buttonRing, CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (connected) "Disconnect" else "Connect",
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_no_bg),
                    contentDescription = if (connected) "Disconnect" else "Connect",
                    colorFilter = ColorFilter.tint(logoTint),
                    modifier = Modifier.width(72.dp).height(86.dp),
                )
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    trailing: ImageVector? = null,
    customIconRes: Int? = null,
    highlighted: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)

    if (highlighted) {
        // Static Illuminated Glow Button with Custom Crow Media Icon
        val borderGradient = Brush.horizontalGradient(
            colors = listOf(
                CrowAccentBorder,
                CrowAccent,
                CrowAccentText
            )
        )

        Surface(
            onClick = onClick,
            modifier = modifier
                .height(48.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = shape,
                    ambientColor = CrowAccentGlow,
                    spotColor = CrowAccentGlow
                ),
            shape = shape,
            color = CrowSurface,
            border = BorderStroke(1.5.dp, borderGradient)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label,
                    color = CrowAccentText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                
                if (customIconRes != null) {
                    Icon(
                        painter = painterResource(id = customIconRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (trailing != null) {
                    Icon(
                        imageVector = trailing,
                        contentDescription = null,
                        tint = CrowAccentText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    } else {
        Row(
            modifier = modifier
                .height(48.dp)
                .clip(shape)
                .background(CrowSurface)
                .border(BorderStroke(1.dp, CrowBorder), shape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                color = CrowText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (customIconRes != null) {
                Icon(
                    painter = painterResource(id = customIconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(22.dp)
                )
            } else if (trailing != null) {
                Icon(trailing, contentDescription = null, tint = CrowMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun LocationRow(
    serverName: String,
    countryCode: String,
    ping: String,
    connected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(CrowSurface)
            .border(1.dp, CrowBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (countryCode == "🌐" || countryCode.isEmpty()) {
                Icon(
                    imageVector = Icons.Outlined.Public,
                    contentDescription = null,
                    tint = CrowAccent,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(text = countryCode, fontSize = 18.sp)
            }
        }

        val selectLocationText = stringResource(R.string.select_location)
        Text(
            text = serverName.ifBlank { selectLocationText },
            color = CrowText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = ping,
            color = CrowAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = CrowMuted, modifier = Modifier.size(18.dp))
    }
}
