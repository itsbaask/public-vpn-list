package com.corvus.vpn.ui.routing

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvus.vpn.ui.theme.*

@Composable
fun RoutingScreen(
    isPro: Boolean = false,
    onTriggerGate: () -> Unit = {},
    appRoutingMode: String,
    onAppRoutingModeChange: (String) -> Unit,
    multiTunnelingEnabled: Boolean,
    onMultiTunnelingChange: (Boolean) -> Unit,
    onConfigureAppsClick: () -> Unit
) {
    var splitTunnelingEnabled by remember(appRoutingMode) {
        mutableStateOf(appRoutingMode == "include" || appRoutingMode == "exclude")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CrowBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CrowAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AltRoute,
                    contentDescription = null,
                    tint = CrowAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "Smart Routing",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrowText
                )
                Text(
                    text = "Control how your apps use the VPN",
                    fontSize = 13.sp,
                    color = CrowMuted
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        RoutingFeatureCard(
            icon = Icons.Default.AltRoute,
            title = "Split Tunneling",
            description = "Let trusted apps and websites connect directly without a VPN to boost local speed",
            checked = splitTunnelingEnabled,
            onCheckedChange = { checked ->
                if (!isPro) {
                    onTriggerGate()
                } else {
                    splitTunnelingEnabled = checked
                    onAppRoutingModeChange(if (checked) "include" else "all")
                }
            },
            onConfigureClick = {
                if (!isPro) {
                    onTriggerGate()
                } else {
                    if (splitTunnelingEnabled) onConfigureAppsClick()
                }
            }
        )

        RoutingFeatureCard(
            icon = Icons.Default.DeviceHub,
            title = "Multi Tunneling",
            description = "Let certain apps and websites use a fixed VPN location, no matter where you're connected",
            checked = multiTunnelingEnabled,
            onCheckedChange = { checked ->
                if (!isPro) {
                    onTriggerGate()
                } else {
                    onMultiTunnelingChange(checked)
                }
            },
            onConfigureClick = null
        )
    }
}

@Composable
private fun RoutingFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onConfigureClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(18.dp)

    val borderColor by animateColorAsState(
        targetValue = if (checked) CrowAccent.copy(alpha = 0.55f) else CrowBorderMid,
        animationSpec = tween(250),
        label = "routingBorder"
    )
    val cardColor by animateColorAsState(
        targetValue = if (checked) CrowAccent.copy(alpha = 0.05f).compositeOver(CrowSurface) else CrowSurface,
        animationSpec = tween(250),
        label = "routingCard"
    )
    val tileColor by animateColorAsState(
        targetValue = CrowAccent.copy(alpha = if (checked) 0.22f else 0.1f),
        animationSpec = tween(250),
        label = "routingTile"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        color = cardColor,
        border = BorderStroke(if (checked) 1.dp else 0.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = checked,
                        role = Role.Switch,
                        onValueChange = onCheckedChange
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tileColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CrowAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrowText
                        )
                        if (checked) {
                            Text(
                                text = "ON",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrowAccent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CrowAccent.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = description,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        color = CrowMuted
                    )
                }

                Switch(
                    checked = checked,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CrowBlack,
                        checkedTrackColor = CrowAccent,
                        uncheckedThumbColor = CrowMuted,
                        uncheckedTrackColor = CrowSurface,
                        uncheckedBorderColor = CrowBorderMid
                    )
                )
            }

            if (onConfigureClick != null) {
                Button(
                    onClick = onConfigureClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrowAccent.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "Configure Apps",
                        color = CrowAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}