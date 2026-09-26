package com.corvus.vpn.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvus.vpn.R
import com.corvus.vpn.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

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
        isConnected  -> ConnectionState.Connected
        isConnecting -> ConnectionState.Connecting
        else         -> ConnectionState.Disconnected
    }

    if (showModeDialog) {
        ProtocolDialog(
            selectedMode = selectedMode,
            onModeChange = onModeChange,
            onDismiss = { showModeDialog = false }
        )
    }

    Scaffold(
        containerColor = CrowBlack,
        bottomBar = {
            // Ad banner
            val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
            if (activity != null && unityAdsManager != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(CrowBlack),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { unityAdsManager.createBannerView(activity) },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CrowBlack)
                .padding(paddingValues)
        ) {
            // Subtle ambient radial glow behind connect button
            AmbientGlow(isConnected = isConnected, isConnecting = isConnecting)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(14.dp))

                // Top bar
                HomeTopBar(
                    onOpenSettings = onSettingsClick,
                    onOpenPro = onProClick
                )

                Spacer(Modifier.weight(0.8f))

                // Status pill
                StatusPill(connectionState)

                Spacer(Modifier.height(32.dp))

                // Central connect button
                ConnectButton(state = connectionState, onClick = onToggleClick)

                Spacer(Modifier.height(28.dp))

                // Duration counter
                Text(
                    text = duration,
                    color = if (connectionState == ConnectionState.Connected) CrowText else CrowMuted,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 3.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )

                Spacer(Modifier.weight(1f))

                // Speed stats (only show when connected)
                if (isConnected) {
                    SpeedRow(downloadSpeed = downloadSpeed, uploadSpeed = uploadSpeed)
                    Spacer(Modifier.height(16.dp))
                }

                // Action chips row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val wireguardLabel = stringResource(R.string.wireguard_mode)
                    val openvpnLabel   = stringResource(R.string.openvpn_mode)
                    val smartLabel     = stringResource(R.string.smart_mode)
                    val freePremiumLabel = stringResource(R.string.free_premium_label)

                    ActionChip(
                        label = when (selectedMode) {
                            "wireguard" -> wireguardLabel
                            "openvpn"   -> openvpnLabel
                            else        -> smartLabel
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

                Spacer(Modifier.height(10.dp))

                // Location row
                LocationRow(
                    serverName  = serverName,
                    countryCode = countryCode,
                    ping        = ping,
                    connected   = isConnected,
                    onClick     = onServerClick,
                )

                Spacer(Modifier.height(20.dp))

                // Brand word-mark
                Text(
                    text = "CORVUS",
                    color = CrowMuted.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 10.sp,
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Ambient radial glow behind the connect button ────────────────────────────
@Composable
private fun AmbientGlow(isConnected: Boolean, isConnecting: Boolean) {
    val glowAlpha by animateFloatAsState(
        targetValue = when {
            isConnected  -> 0.06f
            isConnecting -> 0.03f
            else         -> 0f
        },
        animationSpec = tween(800),
        label = "glowAlpha"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (glowAlpha > 0f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF39FF88).copy(alpha = glowAlpha),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height * 0.42f),
                    radius = size.width * 0.72f
                ),
                radius = size.width * 0.72f,
                center = Offset(size.width / 2f, size.height * 0.42f)
            )
        }
    }
}

// ── Top bar ──────────────────────────────────────────────────────────────────
@Composable
private fun HomeTopBar(onOpenSettings: () -> Unit, onOpenPro: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Settings icon button
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(CrowSurface)
                .border(0.5.dp, CrowBorderMid, CircleShape)
                .clickable(role = Role.Button, onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = CrowTextSub,
                modifier = Modifier.size(19.dp),
            )
        }

        // Brand name centered
        Text(
            text = "CORVUS VPN",
            color = CrowText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 4.sp
        )

        // VIP badge
        val infiniteTransition = rememberInfiniteTransition(label = "vip_glow")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_alpha"
        )

        Box(
            modifier = Modifier
                .graphicsLayer(alpha = glowAlpha)
                .clickable(role = Role.Button, onClick = onOpenPro)
        ) {
            Image(
                painter = painterResource(id = R.drawable.vip_card_raven),
                contentDescription = "VIP Badge",
                modifier = Modifier
                    .width(64.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Fit
            )
        }
    }
}

// ── Status pill ───────────────────────────────────────────────────────────────
@Composable
private fun StatusPill(state: ConnectionState) {
    val on = state != ConnectionState.Disconnected
    val border by animateColorAsState(
        if (on) CrowAccent.copy(alpha = 0.4f) else CrowBorderMid,
        tween(400), label = "pillBorder"
    )
    val dot by animateColorAsState(
        when (state) {
            ConnectionState.Connected  -> CrowAccent
            ConnectionState.Connecting -> CrowGold
            else                       -> CrowDotOff
        },
        tween(400), label = "pillDot"
    )
    val text by animateColorAsState(
        if (on) CrowText else CrowTextSub,
        tween(400), label = "pillText"
    )

    // Pulsing dot when connecting
    val infiniteTransition = rememberInfiniteTransition(label = "dot_pulse")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )

    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(CrowSurface)
            .border(0.5.dp, border, CircleShape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (state == ConnectionState.Connecting) 6.dp * dotScale else 6.dp)
                .clip(CircleShape)
                .background(dot)
        )

        val notConnectedText = stringResource(R.string.status_disconnected)
        val connectingText   = stringResource(R.string.status_connecting)
        val connectedText    = stringResource(R.string.status_connected)

        Text(
            text = when (state) {
                ConnectionState.Disconnected -> notConnectedText
                ConnectionState.Connecting   -> connectingText
                ConnectionState.Connected    -> connectedText
            },
            color = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

// ── Connect button — 3-ring concentric design with raven logo ────────────────
@Composable
private fun ConnectButton(state: ConnectionState, onClick: () -> Unit) {
    val on       = state != ConnectionState.Disconnected
    val connected = state == ConnectionState.Connected

    val outerRing   by animateColorAsState(if (on) CrowRingOuterOn else CrowRingOuterOff,   tween(500), label = "outer")
    val innerRing   by animateColorAsState(if (on) CrowRingInnerOn else CrowRingInnerOff,   tween(500), label = "inner")
    val buttonRing  by animateColorAsState(if (on) CrowAccent else CrowButtonRingOff,        tween(500), label = "btn")
    val logoTint    by animateColorAsState(if (connected) CrowLogoOn else CrowLogoOff,        tween(500), label = "logo")
    val glowColor   by animateColorAsState(if (connected) CrowAccentGlow else Color.Transparent, tween(500), label = "glow")

    // Slow rotation for outer ring when connecting
    val infiniteTransition = rememberInfiniteTransition(label = "connect_ring")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "ring_rotation"
    )

    Box(
        modifier = Modifier
            .size(270.dp)
            .border(0.5.dp, outerRing, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // Rotating dashed arc when connecting
        if (state == ConnectionState.Connecting) {
            Canvas(modifier = Modifier.size(270.dp)) {
                val strokeWidth = 1.5.dp.toPx()
                drawArc(
                    color = Color(0xFF39FF88).copy(alpha = 0.5f),
                    startAngle = ringRotation,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
                drawArc(
                    color = Color(0xFF39FF88).copy(alpha = 0.5f),
                    startAngle = ringRotation + 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(220.dp)
                .border(0.5.dp, innerRing, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(172.dp)
                    .shadow(if (connected) 32.dp else 0.dp, CircleShape, ambientColor = glowColor, spotColor = glowColor)
                    .clip(CircleShape)
                    .background(CrowCore)
                    .border(if (on) 1.5.dp else 1.dp, buttonRing, CircleShape)
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
                    modifier = Modifier.width(68.dp).height(82.dp),
                )
            }
        }
    }
}

// ── Speed row (shown only when connected) ────────────────────────────────────
@Composable
private fun SpeedRow(downloadSpeed: String, uploadSpeed: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CrowSurface)
            .border(0.5.dp, CrowBorderMid, RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        SpeedItem(label = "↓", value = downloadSpeed, color = CrowAccent)
        Box(modifier = Modifier.size(1.dp, 32.dp).background(CrowBorderMid))
        SpeedItem(label = "↑", value = uploadSpeed, color = CrowTextSub)
    }
}

@Composable
private fun SpeedItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(2.dp))
        Text(text = value, color = CrowText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

// ── Action chip ───────────────────────────────────────────────────────────────
@Composable
private fun ActionChip(
    label: String,
    trailing: ImageVector? = null,
    customIconRes: Int? = null,
    highlighted: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)

    if (highlighted) {
        Surface(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = shape,
            color = CrowSurface,
            border = BorderStroke(1.dp, CrowAccent.copy(alpha = 0.5f))
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
                    color = CrowAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
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
                    Icon(trailing, contentDescription = null, tint = CrowAccent, modifier = Modifier.size(18.dp))
                }
            }
        }
    } else {
        Row(
            modifier = modifier
                .height(48.dp)
                .clip(shape)
                .background(CrowSurface)
                .border(BorderStroke(0.5.dp, CrowBorderMid), shape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                color = CrowText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            if (customIconRes != null) {
                Icon(
                    painter = painterResource(id = customIconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            } else if (trailing != null) {
                Icon(trailing, contentDescription = null, tint = CrowMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Location row ──────────────────────────────────────────────────────────────
@Composable
private fun LocationRow(
    serverName: String,
    countryCode: String,
    ping: String,
    connected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor by animateColorAsState(
        if (connected) CrowAccent.copy(alpha = 0.3f) else CrowBorderMid,
        tween(400), label = "loc_border"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(CrowSurface)
            .border(0.5.dp, borderColor, shape)
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
                    tint = CrowTextSub,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(text = countryCode, fontSize = 18.sp)
            }
        }

        val selectLocationText = stringResource(R.string.select_location)
        Text(
            text = serverName.ifBlank { selectLocationText },
            color = if (serverName.isBlank()) CrowTextSub else CrowText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        if (ping != "0 ms" && connected) {
            Text(
                text = ping,
                color = CrowAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Spacer(Modifier.width(4.dp))
        }

        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = CrowMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ── Protocol selection dialog ─────────────────────────────────────────────────
@Composable
private fun ProtocolDialog(
    selectedMode: String,
    onModeChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CrowSurface2,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.protocol_mode_title),
                fontWeight = FontWeight.Bold,
                color = CrowText,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val modes = listOf(
                    "smart"     to Pair(stringResource(R.string.smart_mode), stringResource(R.string.smart_mode_desc)),
                    "openvpn"   to Pair(stringResource(R.string.openvpn_mode), stringResource(R.string.openvpn_mode_desc)),
                    "wireguard" to Pair(stringResource(R.string.wireguard_mode), stringResource(R.string.wireguard_mode_desc))
                )
                modes.forEach { (key, info) ->
                    val isSelected = selectedMode == key
                    Surface(
                        onClick = {
                            onModeChange(key)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CrowAccentGlow else CrowSurface,
                        border = BorderStroke(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) CrowAccent.copy(alpha = 0.6f) else CrowBorderMid
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onModeChange(key)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CrowAccent,
                                    unselectedColor = CrowMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = info.first,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) CrowText else CrowText,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = info.second,
                                    color = CrowTextSub,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close_button), color = CrowAccent, fontWeight = FontWeight.Bold)
            }
        }
    )
}
