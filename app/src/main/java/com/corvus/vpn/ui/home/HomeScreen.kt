package com.corvus.vpn.ui.home

import androidx.compose.animation.*
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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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

private val HomeErrorRed = Color(0xFFFF5252)

enum class ConnectionState { Disconnected, Connecting, Connected, Failed }

@Composable
fun HomeScreen(
    isConnected: Boolean,
    isConnecting: Boolean,
    isFailed: Boolean = false,
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
    onSpeedTestClick: () -> Unit = {}
) {
    var showModeDialog by remember { mutableStateOf(false) }

    val connectionState = when {
        isConnected  -> ConnectionState.Connected
        isConnecting -> ConnectionState.Connecting
        isFailed     -> ConnectionState.Failed
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
            // Ambient radial glow behind connect button
            AmbientGlow(state = connectionState)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(14.dp))

                HomeTopBar()

                Spacer(Modifier.weight(0.8f))

                StatusPill(connectionState)

                Spacer(Modifier.height(32.dp))

                ConnectButton(state = connectionState, onClick = onToggleClick)

                Spacer(Modifier.height(28.dp))

                // Duration counter
                Text(
                    text = duration,
                    color = if (connectionState == ConnectionState.Connected) CrowText else CrowMuted,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 3.sp,
                    fontFamily = TechnicalFontFamily
                )

                Spacer(Modifier.weight(1f))

                // Speed stats slide in when connected
                AnimatedVisibility(
                    visible = isConnected,
                    enter = fadeIn(tween(400)) + expandVertically(tween(400)),
                    exit = fadeOut(tween(250)) + shrinkVertically(tween(250))
                ) {
                    Column {
                        SpeedRow(downloadSpeed = downloadSpeed, uploadSpeed = uploadSpeed, onClick = onSpeedTestClick)
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Action chip
                val wireguardLabel = stringResource(R.string.wireguard_mode)
                val openvpnLabel   = stringResource(R.string.openvpn_mode)
                val smartLabel     = stringResource(R.string.smart_mode)
                val vlessLabel     = stringResource(R.string.vless_mode)
                val vmessLabel     = stringResource(R.string.vmess_mode)
                val trojanLabel    = stringResource(R.string.trojan_mode)
                val shadowsocksLabel = stringResource(R.string.shadowsocks_mode)
                val hysteria2Label = stringResource(R.string.hysteria2_mode)

                ActionChip(
                    label = when (selectedMode) {
                        "vless" -> "$vlessLabel Mode"
                        "vmess" -> "$vmessLabel Mode"
                        "trojan" -> "$trojanLabel Mode"
                        "shadowsocks" -> "$shadowsocksLabel Mode"
                        "hysteria2" -> "$hysteria2Label Mode"
                        "wireguard" -> wireguardLabel
                        "openvpn"   -> openvpnLabel
                        else        -> smartLabel
                    },
                    trailing = Icons.Outlined.ChevronRight,
                    highlighted = false,
                    onClick = { showModeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(10.dp))

                LocationRow(
                    serverName  = serverName,
                    countryCode = countryCode,
                    ping        = ping,
                    connected   = isConnected,
                    onClick     = onServerClick,
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Ambient radial glow behind the connect button ────────────────────────────
@Composable
private fun AmbientGlow(state: ConnectionState) {
    val glowAlpha by animateFloatAsState(
        targetValue = when (state) {
            ConnectionState.Connected  -> 0.08f
            ConnectionState.Connecting -> 0.04f
            ConnectionState.Failed     -> 0.05f
            else                       -> 0f
        },
        animationSpec = tween(800),
        label = "glowAlpha"
    )
    val glowColor by animateColorAsState(
        targetValue = if (state == ConnectionState.Failed) HomeErrorRed else CrowAccent,
        animationSpec = tween(600),
        label = "glowColor"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (glowAlpha > 0f) {
            val center = Offset(size.width / 2f, size.height * 0.42f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = glowAlpha), Color.Transparent),
                    center = center,
                    radius = size.width * 0.72f
                ),
                radius = size.width * 0.72f,
                center = center
            )
        }
    }
}

// ── Top bar ──────────────────────────────────────────────────────────────────
@Composable
private fun HomeTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "CORVUS",
            color = CrowTextSub,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 12.sp,
        )
    }
}

// ── Status pill ───────────────────────────────────────────────────────────────
@Composable
private fun StatusPill(state: ConnectionState) {
    val border by animateColorAsState(
        when (state) {
            ConnectionState.Connected  -> CrowAccent.copy(alpha = 0.4f)
            ConnectionState.Connecting -> CrowGold.copy(alpha = 0.4f)
            ConnectionState.Failed     -> HomeErrorRed.copy(alpha = 0.5f)
            else                       -> CrowBorderMid
        },
        tween(400), label = "pillBorder"
    )
    val dot by animateColorAsState(
        when (state) {
            ConnectionState.Connected  -> CrowAccent
            ConnectionState.Connecting -> CrowGold
            ConnectionState.Failed     -> HomeErrorRed
            else                       -> CrowDotOff
        },
        tween(400), label = "pillDot"
    )
    val textColor by animateColorAsState(
        when (state) {
            ConnectionState.Connected, ConnectionState.Connecting -> CrowText
            ConnectionState.Failed -> HomeErrorRed
            else -> CrowTextSub
        },
        tween(400), label = "pillText"
    )

    val notConnectedText = stringResource(R.string.status_disconnected)
    val connectingText   = stringResource(R.string.status_connecting)
    val connectedText    = stringResource(R.string.status_connected)
    val failedText       = stringResource(R.string.status_failed)

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
        StatusDot(color = dot, pulsing = state == ConnectionState.Connecting)

        Text(
            text = when (state) {
                ConnectionState.Disconnected -> notConnectedText
                ConnectionState.Connecting   -> connectingText
                ConnectionState.Connected    -> connectedText
                ConnectionState.Failed       -> failedText
            },
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun StatusDot(color: Color, pulsing: Boolean) {
    // The infinite animation only runs while connecting
    val scale = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "dot_pulse")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_scale"
        ).value
    } else 1f

    Box(
        modifier = Modifier
            .size(6.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CircleShape)
            .background(color)
    )
}

// ── Connect button — 3-ring concentric design with raven logo ────────────────
@Composable
private fun ConnectButton(state: ConnectionState, onClick: () -> Unit) {
    val connected  = state == ConnectionState.Connected
    val connecting = state == ConnectionState.Connecting
    val failed     = state == ConnectionState.Failed
    val on         = connected || connecting

    val outerRing by animateColorAsState(
        when {
            failed -> HomeErrorRed.copy(alpha = 0.25f)
            on     -> CrowRingOuterOn
            else   -> CrowRingOuterOff
        }, tween(500), label = "outer"
    )
    val innerRing by animateColorAsState(
        when {
            failed -> HomeErrorRed.copy(alpha = 0.35f)
            on     -> CrowRingInnerOn
            else   -> CrowRingInnerOff
        }, tween(500), label = "inner"
    )
    val buttonRing by animateColorAsState(
        when {
            failed -> HomeErrorRed
            on     -> CrowAccent
            else   -> CrowButtonRingOff
        }, tween(500), label = "btn"
    )
    val logoTint by animateColorAsState(
        when {
            connected -> CrowLogoOn
            failed    -> HomeErrorRed.copy(alpha = 0.9f)
            else      -> CrowLogoOff
        }, tween(500), label = "logo"
    )
    val glowColor by animateColorAsState(
        when {
            connected -> CrowAccentGlow
            failed    -> HomeErrorRed.copy(alpha = 0.5f)
            else      -> Color.Transparent
        }, tween(500), label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .widthIn(max = 290.dp)
            .aspectRatio(1f)
            .border(0.5.dp, outerRing, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (connected) ConnectedRipple()
        if (connecting) ConnectingArcs()

        Box(
            modifier = Modifier
                .fillMaxSize(0.815f)
                .border(0.5.dp, innerRing, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.782f)
                    .shadow(
                        elevation = if (connected) 32.dp else if (failed) 16.dp else 0.dp,
                        shape = CircleShape,
                        ambientColor = glowColor,
                        spotColor = glowColor
                    )
                    .clip(CircleShape)
                    .background(CrowCore)
                    .border(if (on || failed) 1.5.dp else 1.dp, buttonRing, CircleShape)
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

// Expanding sonar ring while connected
@Composable
private fun ConnectedRipple() {
    val transition = rememberInfiniteTransition(label = "ripple")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearOutSlowInEasing)
        ),
        label = "rippleProgress"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val maxRadius = size.minDimension / 2f
        val baseRadius = maxRadius * 0.64f
        drawCircle(
            color = CrowAccent.copy(alpha = 0.22f * (1f - progress)),
            radius = baseRadius + (maxRadius - baseRadius) * progress,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

// Two rotating arcs while connecting
@Composable
private fun ConnectingArcs() {
    val transition = rememberInfiniteTransition(label = "connect_ring")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "ring_rotation"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 1.5.dp.toPx()
        val inset = strokeWidth / 2f
        listOf(0f, 180f).forEach { offset ->
            drawArc(
                color = CrowAccent.copy(alpha = 0.55f),
                startAngle = rotation + offset,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

// ── Speed row (slides in when connected) ─────────────────────────────────────
@Composable
private fun SpeedRow(downloadSpeed: String, uploadSpeed: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CrowSurface)
            .border(0.5.dp, CrowBorderMid, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        SpeedItem(icon = Icons.Rounded.ArrowDownward, value = downloadSpeed, color = CrowAccent)
        Box(modifier = Modifier.size(1.dp, 32.dp).background(CrowBorderMid))
        SpeedItem(icon = Icons.Rounded.ArrowUpward, value = uploadSpeed, color = CrowTextSub)
    }
}

@Composable
private fun SpeedItem(icon: ImageVector, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Text(
            text = value,
            color = CrowText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = TechnicalFontFamily
        )
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
            color = CrowAccent.copy(alpha = 0.08f).compositeOver(CrowSurface),
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
                    modifier = Modifier.weight(1f)
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
                modifier = Modifier.weight(1f)
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
    val shape = RoundedCornerShape(18.dp)
    val borderColor by animateColorAsState(
        if (connected) CrowAccent.copy(alpha = 0.35f) else CrowBorderMid,
        tween(400), label = "loc_border"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(shape)
            .background(CrowSurface)
            .border(if (connected) 1.dp else 0.5.dp, borderColor, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(CrowSurface2),
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
                Text(text = countryCode, fontSize = 20.sp)
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
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CrowAccent.copy(alpha = 0.1f))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(CrowAccent)
                )
                Text(
                    text = ping,
                    color = CrowAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = TechnicalFontFamily
                )
            }
        }

        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = CrowMuted,
            modifier = Modifier.size(18.dp)
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
                    "smart" to Pair(stringResource(R.string.smart_mode), stringResource(R.string.smart_mode_desc)),
                    "vless" to Pair(stringResource(R.string.vless_mode), stringResource(R.string.vless_mode_desc)),
                    "vmess" to Pair(stringResource(R.string.vmess_mode), stringResource(R.string.vmess_mode_desc)),
                    "trojan" to Pair(stringResource(R.string.trojan_mode), stringResource(R.string.trojan_mode_desc)),
                    "shadowsocks" to Pair(stringResource(R.string.shadowsocks_mode), stringResource(R.string.shadowsocks_mode_desc)),
                    "hysteria2" to Pair(stringResource(R.string.hysteria2_mode), stringResource(R.string.hysteria2_mode_desc)),
                    "openvpn" to Pair(stringResource(R.string.openvpn_mode), stringResource(R.string.openvpn_mode_desc))
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
                                    color = CrowText,
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