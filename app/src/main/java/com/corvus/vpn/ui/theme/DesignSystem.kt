package com.corvus.vpn.ui.theme

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ============================================================================
// 1. Corvus Palette — Minimal & Professional (Unified with Crow Theme)
// ============================================================================

val ColorBlack          = CrowBlack
val ColorSurface        = CrowSurface
val ColorSurfaceVariant = CrowCore
val ColorBorder         = CrowBorder

val ColorAccent         = CrowAccent
val ColorAccentMuted    = CrowAccentBorder

val ColorTextPrimary    = CrowText
val ColorTextSecondary  = CrowMuted
val ColorTextTertiary   = CrowFaint

val ColorSuccess        = Color(0xFF4CAF7D) // Soft Emerald Connected
val ColorDanger         = CrowBlood
val ColorPending        = CrowGold

// ============================================================================
// 2. Color Scheme & Theme Provider
// ============================================================================
private val CorvusColorScheme = darkColorScheme(
    primary = CrowAccent,
    onPrimary = CrowBlack,
    primaryContainer = CrowAccentBorder,
    onPrimaryContainer = CrowAccentText,
    secondary = CrowMuted,
    onSecondary = CrowText,
    tertiary = CrowGold,
    onTertiary = CrowBlack,
    background = CrowBlack,
    onBackground = CrowText,
    surface = CrowSurface,
    onSurface = CrowText,
    surfaceVariant = CrowCore,
    onSurfaceVariant = CrowMuted,
    outline = CrowBorder,
    outlineVariant = CrowBorder,
    error = CrowBlood,
    onError = CrowText
)

@Composable
fun CorvusUnifiedTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CorvusColorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = CorvusColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

// ============================================================================
// 3. Corvus Atomic Components Library
// ============================================================================

@Composable
fun CorvusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = ColorAccent,
    contentColor: Color = ColorTextPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "button_scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .scale(scale),
        shape = RoundedCornerShape(14.dp),
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
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
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
        shape = RoundedCornerShape(16.dp),
        color = ColorSurface,
        border = BorderStroke(width = 1.dp, color = ColorBorder)
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
            checkedThumbColor = ColorTextPrimary,
            checkedTrackColor = ColorAccent,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = ColorTextSecondary,
            uncheckedTrackColor = ColorSurfaceVariant,
            uncheckedBorderColor = ColorBorder
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ColorSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ColorTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                    color = ColorTextPrimary
                )
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        letterSpacing = (-0.1).sp,
                        color = ColorTextSecondary
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
                tint = ColorTextTertiary
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
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val activeColor = when {
        isConnected -> ColorSuccess
        isConnecting -> ColorPending
        else -> ColorAccent
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "buttonScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(200.dp)
            .scale(if (isConnected || isConnecting) pulseScale else 1f)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.dp,
                color = if (isConnected || isConnecting) activeColor.copy(alpha = 0.35f) else ColorBorder
            )
        ) {}

        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(148.dp)
                .scale(buttonScale),
            shape = CircleShape,
            color = ColorSurface,
            border = BorderStroke(width = 1.5.dp, color = activeColor),
            shadowElevation = 0.dp,
            interactionSource = interactionSource
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "VPN Power Toggle",
                    tint = if (isConnected || isConnecting) activeColor else ColorTextPrimary,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
    }
}
