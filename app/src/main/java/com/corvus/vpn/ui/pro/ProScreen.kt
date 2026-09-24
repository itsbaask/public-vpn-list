package com.corvus.vpn.ui.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvus.vpn.R
import com.corvus.vpn.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProScreen(
    onClose: () -> Unit,
    onPurchase: (String) -> Unit
) {
    var selectedPlan by remember { mutableStateOf("yearly") }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDarkObsidian)
    ) {
        // High-end Cyber Cyan aura gradient at the top background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ElectricCyan.copy(alpha = 0.20f),
                            ElectricCyan.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Corvus VIP Pro",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.5).sp,
                            color = TextPrimaryWhite
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(36.dp)
                                .background(
                                    color = CyberSurfaceDark,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(18.dp),
                                tint = TextPrimaryWhite
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Glowing Premium Hero Emblem
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        ElectricCyan.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    Surface(
                        shape = CircleShape,
                        color = CyberSurfaceDark,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(56.dp)
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        ElectricCyan,
                                        PureGold
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoGraph,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = ElectricCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.pro_unleash),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    textAlign = TextAlign.Center,
                    color = TextPrimaryWhite
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.pro_subtitle),
                    fontSize = 15.sp,
                    color = TextSecondaryMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Features Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = CyberSurfaceDark,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = CyberBorderStroke,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CyberFeatureRow(Icons.Default.AdsClick, stringResource(R.string.pro_no_ads), stringResource(R.string.pro_no_ads_desc))
                    HorizontalDivider(color = CyberBorderStroke, thickness = 0.5.dp)
                    CyberFeatureRow(Icons.Default.Timer, stringResource(R.string.pro_unlimited_time), stringResource(R.string.pro_unlimited_time_desc))
                    HorizontalDivider(color = CyberBorderStroke, thickness = 0.5.dp)
                    CyberFeatureRow(Icons.Default.Bolt, stringResource(R.string.pro_premium_servers), stringResource(R.string.pro_premium_servers_desc))
                    HorizontalDivider(color = CyberBorderStroke, thickness = 0.5.dp)
                    CyberFeatureRow(Icons.Default.Lock, stringResource(R.string.pro_military_encryption), stringResource(R.string.pro_military_encryption_desc))
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Subscriptions Options
                CyberPlanCard(
                    id = "weekly",
                    title = stringResource(R.string.pro_weekly),
                    price = "$4.99",
                    period = "/ week",
                    subtitle = stringResource(R.string.pro_weekly_desc),
                    isSelected = selectedPlan == "weekly",
                    badgeText = null,
                    onClick = { selectedPlan = "weekly" }
                )

                Spacer(modifier = Modifier.height(12.dp))

                CyberPlanCard(
                    id = "monthly",
                    title = stringResource(R.string.pro_monthly),
                    price = "$8.88",
                    period = "/ month",
                    subtitle = stringResource(R.string.pro_monthly_desc),
                    isSelected = selectedPlan == "monthly",
                    badgeText = "POPULAR",
                    onClick = { selectedPlan = "monthly" }
                )

                Spacer(modifier = Modifier.height(12.dp))

                CyberPlanCard(
                    id = "yearly",
                    title = stringResource(R.string.pro_yearly),
                    price = "$44.99",
                    period = "/ year",
                    subtitle = stringResource(R.string.pro_yearly_desc),
                    isSelected = selectedPlan == "yearly",
                    badgeText = "BEST VALUE",
                    onClick = { selectedPlan = "yearly" }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Cyber Gradient Premium Button
                Button(
                    onClick = { onPurchase(selectedPlan) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = ElectricCyan,
                            spotColor = ElectricCyan
                        ),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        ElectricCyan,
                                        RoyalPurple
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Cancel anytime in Google Play settings.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Terms of Service",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryMuted,
                        modifier = Modifier.clickable { }
                    )
                    Text(
                        text = "  •  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryMuted
                    )
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryMuted,
                        modifier = Modifier.clickable { }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun CyberFeatureRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = ElectricNeonGreen.copy(alpha = 0.12f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElectricNeonGreen,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimaryWhite
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = TextSecondaryMuted
            )
        }
    }
}

@Composable
private fun CyberPlanCard(
    id: String,
    title: String,
    price: String,
    period: String,
    subtitle: String,
    isSelected: Boolean,
    badgeText: String?,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) ElectricCyan else CyberBorderStroke
    val bgColor = if (isSelected) ElectricCyan.copy(alpha = 0.08f) else CyberSurfaceDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimaryWhite
                    )
                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(PureGold, GlowingAmber)
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF2A1500)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondaryMuted
                )
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = price,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = TextPrimaryWhite
                )
                Text(
                    text = period,
                    fontSize = 12.sp,
                    color = TextSecondaryMuted,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}
