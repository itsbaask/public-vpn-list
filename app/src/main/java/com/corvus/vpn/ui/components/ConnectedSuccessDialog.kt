package com.corvus.vpn.ui.components

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.corvus.vpn.ui.screens.ConnectedScreen
import com.corvus.vpn.ui.servers.Server
import com.corvus.vpn.util.CountryUtils

/**
 * ConnectedSuccessDialog — World-Class Cyber-Encrypted Connection Success Screen.
 *
 * Renders ConnectedScreen with falling hex code, animated grid,
 * encrypted lock core with expanding rings, and packet-flowing bezier tunnel.
 */
@Composable
fun ConnectedSuccessDialog(
    server: Server?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val serverName = if (server != null) {
        val flag = CountryUtils.getFlagEmoji(server.countryCode ?: "")
        val tier = (server.tier ?: "FREE").replace("_", " ").uppercase()
        val name = server.name
        "$tier Server — $flag $name"
    } else {
        "Secure Server — 🌐 Corvus Core"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        ConnectedScreen(
            serverName = serverName,
            onShare = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "I am securely browsing using Corvus VPN! Download it now for fast, private, and encrypted internet."
                    )
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Corvus VPN"))
            },
            onBack = onDismiss
        )
    }
}
