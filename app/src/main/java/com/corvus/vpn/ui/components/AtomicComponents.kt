package com.corvus.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvus.vpn.R
import com.corvus.vpn.ui.theme.*

/**
 * Primary Action Button (Crow Theme)
 */
@Composable
fun CorvusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = CrowAccent,
    contentColor: Color = CrowBlack
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "button_scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.4f)
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

/**
 * Surface Container Card (Crow Theme)
 */
@Composable
fun CorvusCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = CrowSurface,
    borderColor: Color = CrowBorder,
    cornerRadius: Dp = 18.dp,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        border = BorderStroke(width = 1.dp, color = borderColor)
    ) {
        Column(
            modifier = Modifier.padding(padding),
            content = content
        )
    }
}

/**
 * Toggle Switch (Crow Theme)
 */
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
            checkedThumbColor = CrowText,
            checkedTrackColor = CrowAccent,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = CrowMuted,
            uncheckedTrackColor = CrowBorder,
            uncheckedBorderColor = Color.Transparent
        )
    )
}

/**
 * List Row Item (Crow Theme)
 */
@Composable
fun CorvusListRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    subtitle: String? = null,
    iconContainerColor: Color = CrowAccentBorder,
    iconTintColor: Color = CrowAccentText,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTintColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = CrowText
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = CrowMuted
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailingContent != null) {
            trailingContent()
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CrowMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Concentric Ring Crow Power Switch (Crow Theme)
 */
@Composable
fun CorvusStatusRing(
    isConnected: Boolean,
    isConnecting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val on = isConnected || isConnecting

    val outerRing by animateColorAsState(if (on) CrowRingOuterOn else CrowRingOuterOff, tween(500), label = "outer")
    val innerRing by animateColorAsState(if (on) CrowRingInnerOn else CrowRingInnerOff, tween(500), label = "inner")
    val buttonRing by animateColorAsState(if (on) CrowAccent else CrowButtonRingOff, tween(500), label = "btn")
    val logoTint by animateColorAsState(if (isConnected) CrowLogoOn else CrowLogoOff, tween(500), label = "logo")
    val glow by animateColorAsState(if (isConnected) CrowAccentGlow else Color.Transparent, tween(500), label = "glow")

    Box(
        modifier = modifier.size(268.dp).border(1.dp, outerRing, CircleShape),
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
                        onClickLabel = if (isConnected) "Disconnect" else "Connect",
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_no_bg),
                    contentDescription = if (isConnected) "Disconnect" else "Connect",
                    colorFilter = ColorFilter.tint(logoTint),
                    modifier = Modifier.width(72.dp).height(86.dp),
                )
            }
        }
    }
}

/**
 * Status Pill Badge (Crow Theme)
 */
@Composable
fun StatusBadgePill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = CrowSurface,
        border = BorderStroke(width = 1.dp, color = CrowBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CrowText
            )
        }
    }
}

/**
 * Custom Divider (Crow Theme)
 */
@Composable
fun CorvusDivider(
    modifier: Modifier = Modifier,
    color: Color = CrowBorder
) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 0.5.dp,
        color = color
    )
}
