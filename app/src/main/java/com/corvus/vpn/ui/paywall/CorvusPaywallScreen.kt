package com.corvus.vpn.ui.paywall

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvus.vpn.ui.theme.CorvusVPNTheme
import com.corvus.vpn.ui.theme.CrowAccent
import com.corvus.vpn.ui.theme.CrowAccentBorder
import com.corvus.vpn.ui.theme.CrowAccentGlow
import com.corvus.vpn.ui.theme.CrowAccentText
import com.corvus.vpn.ui.theme.CrowBlack
import com.corvus.vpn.ui.theme.CrowBorder
import com.corvus.vpn.ui.theme.CrowCore
import com.corvus.vpn.ui.theme.CrowGold
import com.corvus.vpn.ui.theme.CrowMuted
import com.corvus.vpn.ui.theme.CrowSurface
import com.corvus.vpn.ui.theme.CrowText

// ============================================================================
// Data Models
// ============================================================================

data class CrowPerk(
    val icon: ImageVector,
    val title: String,
    val description: String
)

data class CrowPlan(
    val id: String,
    val name: String,
    val price: String,
    val period: String,
    val tag: String? = null,
    val tagIsGold: Boolean = false
)

private val perks = listOf(
    CrowPerk(Icons.Filled.RemoveCircleOutline, "No Ads", "Zero interruptions. Ever."),
    CrowPerk(Icons.Filled.Timer, "Unlimited Time", "Fly as long as you want, no session caps."),
    CrowPerk(Icons.Filled.Bolt, "Premium Servers", "Access to the fastest locations worldwide."),
    CrowPerk(Icons.Filled.Lock, "Military Encryption", "Absolute protection, sharp as a raven's talon.")
)

private val plans = listOf(
    CrowPlan("weekly", "Weekly Pass", "$4.99", "week"),
    CrowPlan("monthly", "Monthly Pass", "$8.88", "month", tag = "POPULAR"),
    CrowPlan("yearly", "Yearly Pass", "$44.99", "year", tag = "BEST VALUE", tagIsGold = true)
)

// ============================================================================
// Main Screen
// ============================================================================

@Composable
fun CorvusPaywallScreen(
    onClose: () -> Unit = {},
    onUnlock: (planId: String) -> Unit = {}
) {
    var selectedPlanId by remember { mutableStateOf("yearly") }

    Scaffold(
        containerColor = CrowBlack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CrowBlack),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { TopBar(onClose = onClose) }
            item { HeroSection() }
            item { FeatherDivider() }
            item { PerksCard() }
            item { PlansSection(selectedPlanId = selectedPlanId, onSelect = { selectedPlanId = it }) }
            item {
                CtaSection(
                    onUnlock = { onUnlock(selectedPlanId) }
                )
            }
        }
    }
}

// ============================================================================
// Top Bar
// ============================================================================

@Composable
private fun TopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(CrowSurface)
                .border(1.dp, CrowBorder, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = CrowMuted,
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = "CORVUS PRO",
            color = CrowText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            letterSpacing = 3.sp
        )

        Spacer(modifier = Modifier.size(34.dp))
    }
}

// ============================================================================
// Hero — Raven Eye Badge
// ============================================================================

@Composable
private fun HeroSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "eyeGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(CrowAccentGlow.copy(alpha = 0.28f), CrowSurface),
                    center = Offset(0.5f, 0f),
                    radius = 700f
                )
            )
            .border(1.dp, CrowBorder, RoundedCornerShape(26.dp))
            .padding(vertical = 34.dp, horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing eye badge
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CrowCore, CrowBlack),
                        center = Offset(0.35f, 0.3f)
                    )
                )
                .border(1.dp, CrowAccent.copy(alpha = glowAlpha), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(CrowBlack)
                    .border(1.dp, CrowGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(CrowAccent)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Unleash Full Raven Power",
            color = CrowText,
            fontWeight = FontWeight.Bold,
            fontSize = 23.sp,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Unbound speed, absolute freedom, and protection watched over by the raven's eye.",
            color = CrowMuted,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

// ============================================================================
// Feather Divider
// ============================================================================

@Composable
private fun FeatherDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalLine(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(CrowMuted.copy(alpha = 0.6f))
        )
        Spacer(modifier = Modifier.width(10.dp))
        HorizontalLine(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HorizontalLine(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, CrowBorder, Color.Transparent)
                )
            )
    )
}

// ============================================================================
// Perks Card
// ============================================================================

@Composable
private fun PerksCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(CrowSurface)
            .border(1.dp, CrowBorder, RoundedCornerShape(22.dp))
    ) {
        perks.forEachIndexed { index, perk ->
            PerkRow(perk)
            if (index != perks.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CrowBorder)
                )
            }
        }
    }
}

@Composable
private fun PerkRow(perk: CrowPerk) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(CrowCore)
                .border(1.dp, CrowBorder, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = perk.icon,
                contentDescription = perk.title,
                tint = CrowAccent,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = perk.title,
                color = CrowText,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = perk.description,
                color = CrowMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

// ============================================================================
// Plans Section
// ============================================================================

@Composable
private fun PlansSection(
    selectedPlanId: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        plans.forEach { plan ->
            PlanRow(
                plan = plan,
                selected = plan.id == selectedPlanId,
                onClick = { onSelect(plan.id) }
            )
        }
    }
}

@Composable
private fun PlanRow(
    plan: CrowPlan,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) CrowAccent else CrowBorder
    val backgroundBrush = if (selected) {
        Brush.linearGradient(
            colors = listOf(CrowAccentGlow.copy(alpha = 0.22f), CrowAccentGlow.copy(alpha = 0.05f))
        )
    } else {
        Brush.linearGradient(colors = listOf(CrowSurface, CrowSurface))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundBrush)
            .border(
                width = if (selected) 1.4.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioIndicator(selected = selected)
            Column {
                Text(
                    text = plan.name,
                    color = CrowText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                )
                plan.tag?.let { tag ->
                    Text(
                        text = tag,
                        color = if (plan.tagIsGold) CrowGold else CrowAccentText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = plan.price,
                color = CrowText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "/ ${plan.period}",
                color = CrowMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun RadioIndicator(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (selected) CrowAccent else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (selected) CrowAccent else CrowText.copy(alpha = 0.25f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CrowBlack)
            )
        }
    }
}

// ============================================================================
// CTA Button
// ============================================================================

@Composable
private fun CtaSection(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(CrowAccentText, CrowAccent, CrowAccentBorder)
                    )
                )
                .clickable { onUnlock() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "UNLOCK CORVUS PRO",
                color = CrowBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 1.2.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Cancel anytime — auto-renews unless cancelled",
            color = CrowMuted,
            fontSize = 10.5.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================================
// Preview
// ============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0D)
@Composable
private fun CorvusPaywallPreview() {
    CorvusVPNTheme {
        CorvusPaywallScreen()
    }
}
