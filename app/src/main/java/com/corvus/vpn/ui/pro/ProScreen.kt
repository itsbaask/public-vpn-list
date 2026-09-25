package com.corvus.vpn.ui.pro

import androidx.compose.runtime.Composable
import com.corvus.vpn.ui.paywall.CorvusPaywallScreen

@Composable
fun ProScreen(
    totalNodes: Int = 0,
    totalCountries: Int = 0,
    onClose: () -> Unit,
    onPurchase: (String) -> Unit
) {
    CorvusPaywallScreen(
        totalNodes = totalNodes,
        totalCountries = totalCountries,
        onClose = onClose,
        onUnlock = onPurchase
    )
}
