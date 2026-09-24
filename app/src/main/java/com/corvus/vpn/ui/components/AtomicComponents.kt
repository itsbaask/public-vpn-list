package com.corvus.vpn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvus.vpn.ui.theme.*

@Composable
fun CorvusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "button_scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(scale),
        shape = RoundedCornerShape(27.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        )
    }
}

@Composable
fun CorvusCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CyberSurfaceDark,
        border = BorderStroke(width = 1.dp, color = CyberBorderStroke)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
fun CorvusToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = ElectricNeonGreen,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = TextSecondaryMuted,
            uncheckedTrackColor = CyberSurfaceVariant,
            uncheckedBorderColor = Color.Transparent
        )
    )
}

@Composable
fun CorvusListRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(ElectricCyan.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimaryWhite
                )
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = TextSecondaryMuted
                    )
                )
            }
        }
        if (trailingContent != null) {
            trailingContent()
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondaryMuted
            )
        }
    }
}

@Composable
fun CorvusStatusRing(
    isConnected: Boolean,
    isConnecting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stateColor = when {
        isConnected -> ElectricNeonGreen
        isConnecting -> GlowingAmber
        else -> ElectricCyan
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "buttonScale"
    )

    // Breathing Glow & Pulse Waves
    val infiniteTransition = rememberInfiniteTransition(label = "power_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = if (isConnected || isConnecting) 1.0f else 0.98f,
        targetValue = if (isConnected || isConnecting) 1.12f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isConnected || isConnecting) 0.25f else 0.10f,
        targetValue = if (isConnected || isConnecting) 0.50f else 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(210.dp)
            .scale(buttonScale)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = stateColor)
            ) { onClick() }
    ) {
        // Outer Ambient Cyber Radar Aura
        Canvas(
            modifier = Modifier
                .size(210.dp)
                .scale(glowScale),
            onDraw = {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            stateColor.copy(alpha = glowAlpha),
                            stateColor.copy(alpha = glowAlpha * 0.35f),
                            Color.Transparent
                        )
                    )
                )
            }
        )

        // Glassmorphic Inner Button Container
        Surface(
            shape = CircleShape,
            color = CyberSurfaceDark,
            border = BorderStroke(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        stateColor,
                        stateColor.copy(alpha = 0.25f)
                    )
                )
            ),
            shadowElevation = 16.dp,
            modifier = Modifier.size(146.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isConnecting) {
                    LoadingRingCanvas(stateColor = stateColor)
                }

                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Power Toggle",
                    tint = stateColor,
                    modifier = Modifier.size(58.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingRingCanvas(stateColor: Color) {
    val rotation by rememberInfiniteTransition(label = "rotateAnim").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing)
        ),
        label = "rotate"
    )

    Canvas(
        modifier = Modifier
            .size(146.dp)
            .scale(1.05f),
        onDraw = {
            drawArc(
                startAngle = rotation,
                sweepAngle = 120f,
                useCenter = false,
                color = stateColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    )
}
