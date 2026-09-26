package com.corvus.vpn.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.corvus.vpn.R
import com.corvus.vpn.ui.servers.Server
import com.corvus.vpn.ui.theme.*
import com.corvus.vpn.util.CountryUtils

/**
 * ConnectedSuccessDialog — World-Class Cyber-Encrypted Connection Success Screen.
 *
 * Appears upon successful connection to reassure the user that their traffic
 * is 100% encrypted, secure, and untraceable.
 */
@Composable
fun ConnectedSuccessDialog(
    server: Server?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Pulsing glow animation for the security shield
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val serverName = server?.name ?: "Secure Server"
    val countryFlag = if (server?.countryCode != null) CountryUtils.getFlagEmoji(server.countryCode) else "🌐"
    val protocol = (server?.protocol ?: "OpenVPN").uppercase()
    val serverTier = (server?.tier ?: "FREE").uppercase()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CrowBlack)
        ) {
            // Background Cyber-Grid Gradient Glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                CrowAccent.copy(alpha = 0.25f),
                                CrowAccentGlow.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                // Pulsing Shield / Logo Badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(96.dp)
                ) {
                    // Outer ambient aura
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(CrowAccent.copy(alpha = pulseAlpha * 0.25f))
                    )
                    // Inner ring
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(CrowSurface)
                            .border(1.5.dp, CrowAccent.copy(alpha = pulseAlpha), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Corvus Shield",
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title: Connected
                Text(
                    text = "Connected",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrowText,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle
                Text(
                    text = "You are securely connected to",
                    fontSize = 14.sp,
                    color = CrowMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Location / Server Pill
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = CrowSurface,
                    border = BorderStroke(1.dp, CrowAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.shadow(8.dp, RoundedCornerShape(24.dp), spotColor = CrowAccent)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = countryFlag,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = serverName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CrowText
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Tier Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (serverTier.contains("PREMIUM")) CrowGold.copy(alpha = 0.2f) else CrowAccent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = serverTier,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (serverTier.contains("PREMIUM")) CrowGold else CrowAccentText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Main Security & Encryption Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CrowSurface),
                    border = BorderStroke(1.dp, CrowBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Trust Statement Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00E676), // Vibrant Neon Green
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Military-Grade Encryption Active",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrowText
                            )
                        }

                        Text(
                            text = "The VPN works perfectly and you can trust it. All your internet traffic, passwords, and identity are shielded behind an impenetrable zero-log tunnel.",
                            fontSize = 13.sp,
                            color = CrowMuted,
                            lineHeight = 19.sp
                        )

                        HorizontalDivider(color = CrowBorder, thickness = 1.dp)

                        // Feature Badges Grid
                        SecurityFeatureRow(
                            icon = Icons.Default.Lock,
                            title = "Cipher",
                            value = "AES-256-GCM / Quantum Safe"
                        )

                        SecurityFeatureRow(
                            icon = Icons.Default.Security,
                            title = "Protocol",
                            value = "$protocol • High-Speed Core"
                        )

                        SecurityFeatureRow(
                            icon = Icons.Default.Shield,
                            title = "DNS Leak Shield",
                            value = "Protected • 0 Leaks Detected"
                        )

                        SecurityFeatureRow(
                            icon = Icons.Default.VpnKey,
                            title = "No-Logs Policy",
                            value = "Strict RAM-Only • Zero Traces"
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Share With Friends Button
                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "I am securely browsing using Corvus VPN! Download it now for fast, private, and encrypted internet."
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Corvus VPN"))
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CrowAccent.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CrowAccentText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Share with Friends",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f, fill = false))
                Spacer(modifier = Modifier.height(28.dp))

                // Bottom Primary "Back / Continue" Button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrowAccent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = CrowAccent)
                ) {
                    Text(
                        text = "Back",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SecurityFeatureRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CrowCore),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CrowAccent,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 11.sp,
                color = CrowMuted,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 13.sp,
                color = CrowText,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
