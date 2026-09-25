package com.corvus.vpn.ui.paywall

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvus.vpn.R
import com.corvus.vpn.ui.theme.*

data class PrivilegesRow(
    val feature: String,
    val freeValue: String,
    val premiumValue: String,
    val isCheckmark: Boolean = false,
    val isFreeCheckmark: Boolean = false,
    val isPremiumCheckmark: Boolean = true
)

data class PaywallPlan(
    val id: String,
    val title: String,
    val price: String,
    val periodInfo: String,
    val badge: String? = null,
    val isBestValue: Boolean = false
)

@Composable
fun CorvusPaywallScreen(
    totalNodes: Int = 0,
    totalCountries: Int = 0,
    onClose: () -> Unit = {},
    onUnlock: (planId: String) -> Unit = {}
) {
    BackHandler { onClose() }

    var selectedPlanId by remember { mutableStateOf("yearly") }
    val scrollState = rememberScrollState()

    val countriesFormat = stringResource(R.string.countries_count)
    val nodesFormat = stringResource(R.string.nodes_count)
    val defaultCountries = stringResource(R.string.countries_count, 35)
    val defaultNodes = stringResource(R.string.nodes_count, 150)

    val liveNodes = if (totalNodes > 0) String.format(nodesFormat, totalNodes) else defaultNodes
    val liveCountries = if (totalCountries > 0) String.format(countriesFormat, totalCountries) else defaultCountries

    val yearlyTitle = stringResource(R.string.yearly_pass)
    val monthlyTitle = stringResource(R.string.monthly_pass)
    val weeklyTitle = stringResource(R.string.weekly_pass)
    val bestValueText = stringResource(R.string.best_value_badge)
    val popularText = stringResource(R.string.popular_badge)

    val yearlyPeriod = stringResource(R.string.yearly_period_info)
    val monthlyPeriod = stringResource(R.string.monthly_period_info)
    val weeklyPeriod = stringResource(R.string.weekly_period_info)

    val paywallPlans = remember(yearlyTitle, monthlyTitle, weeklyTitle, bestValueText, popularText, yearlyPeriod, monthlyPeriod, weeklyPeriod) {
        listOf(
            PaywallPlan("yearly", yearlyTitle, "$49.99", yearlyPeriod, badge = bestValueText, isBestValue = true),
            PaywallPlan("monthly", monthlyTitle, "$9.99", monthlyPeriod, badge = popularText),
            PaywallPlan("weekly", weeklyTitle, "$4.99", weeklyPeriod)
        )
    }

    val speedLabel = stringResource(R.string.feature_speed)
    val standardLabel = stringResource(R.string.standard_label)
    val ultraFastLabel = stringResource(R.string.corvus_ultra_fast)
    val locationsLabel = stringResource(R.string.feature_locations)
    val limitedLabel = stringResource(R.string.limited_label)
    val globalServersLabel = stringResource(R.string.feature_global_servers)
    val freeNodesLabel = stringResource(R.string.free_nodes_label)
    val noAdsLabel = stringResource(R.string.feature_no_ads)
    val dedicatedLinesLabel = stringResource(R.string.feature_dedicated_lines)
    val prioritySupportLabel = stringResource(R.string.feature_priority_support)

    val privileges = remember(liveNodes, liveCountries, speedLabel, locationsLabel, globalServersLabel, noAdsLabel, dedicatedLinesLabel, prioritySupportLabel) {
        listOf(
            PrivilegesRow(speedLabel, standardLabel, ultraFastLabel),
            PrivilegesRow(locationsLabel, limitedLabel, liveCountries),
            PrivilegesRow(globalServersLabel, freeNodesLabel, liveNodes),
            PrivilegesRow(noAdsLabel, "", "", isCheckmark = true, isFreeCheckmark = false, isPremiumCheckmark = true),
            PrivilegesRow(dedicatedLinesLabel, "", "", isCheckmark = true, isFreeCheckmark = false, isPremiumCheckmark = true),
            PrivilegesRow(prioritySupportLabel, "", "", isCheckmark = true, isFreeCheckmark = false, isPremiumCheckmark = true)
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = CrowBlack,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CrowBlack)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.ic_custom_back),
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.vip_privileges),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CrowText
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CrowBlack)
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero section with the purple raven character
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CrowSurface)
                    .border(1.dp, CrowBorder, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.vip_membership),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = CrowText
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.unlock_sovereign_features),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CrowMuted,
                                fontSize = 12.5.sp
                            )
                        )
                    }
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.raven_paywall_hero),
                        contentDescription = "VIP Raven",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Comparison Table Card
            PrivilegesTableCard(privileges)

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Plans Section (Yearly, Monthly, Weekly)
            paywallPlans.forEach { plan ->
                val isSelected = plan.id == selectedPlanId
                PlanCard(
                    plan = plan,
                    isSelected = isSelected,
                    onClick = { selectedPlanId = plan.id }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Action Button
            Button(
                onClick = { onUnlock(selectedPlanId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrowAccent,
                    contentColor = CrowBlack
                )
            ) {
                Text(
                    text = stringResource(R.string.unlock_vip),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Restore Purchase Link Only
            Text(
                text = stringResource(R.string.restore_purchase),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = CrowAccentText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { /* Handle Restore */ }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Clean 1-Line Disclaimer Text
            Text(
                text = stringResource(R.string.auto_renewable_disclaimer),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = CrowMuted.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PrivilegesTableCard(privileges: List<PrivilegesRow>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CrowSurface,
        border = BorderStroke(1.dp, CrowBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1.3f))
                Text(
                    text = stringResource(R.string.free_tier),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = CrowMuted,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.vip_tier),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CrowAccent,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.weight(1.2f)
                )
            }

            HorizontalDivider(color = CrowBorder, thickness = 0.5.dp)

            // Content Rows
            privileges.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.feature,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = CrowText,
                            fontSize = 13.5.sp
                        ),
                        modifier = Modifier.weight(1.3f)
                    )

                    // Free Column
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isCheckmark) {
                            if (item.isFreeCheckmark) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = CrowAccent, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Default.Close, contentDescription = null, tint = CrowMuted.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Text(
                                text = item.freeValue,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = CrowMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }

                    // Premium Column
                    Box(
                        modifier = Modifier.weight(1.2f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isCheckmark) {
                            if (item.isPremiumCheckmark) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = CrowAccent, modifier = Modifier.size(19.dp))
                            } else {
                                Icon(Icons.Default.Close, contentDescription = null, tint = CrowMuted.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Text(
                                text = item.premiumValue,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CrowAccent,
                                    fontSize = 12.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }

                if (index < privileges.lastIndex) {
                    HorizontalDivider(color = CrowBorder, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: PaywallPlan,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) CrowAccent else CrowBorder,
        animationSpec = tween(250),
        label = "planBorder"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CrowSurface,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isSelected) CrowAccentText else CrowText
                        )
                    )

                    if (plan.badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (plan.isBestValue) CrowGold.copy(alpha = 0.2f) else CrowAccent.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, if (plan.isBestValue) CrowGold else CrowAccent)
                        ) {
                            Text(
                                text = plan.badge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (plan.isBestValue) CrowGold else CrowAccentText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = plan.periodInfo,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = CrowMuted,
                        fontSize = 12.5.sp
                    )
                )
            }

            Text(
                text = plan.price,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = CrowText
                )
            )
        }
    }
}
