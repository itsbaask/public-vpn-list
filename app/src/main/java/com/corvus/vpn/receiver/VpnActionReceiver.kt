package com.corvus.vpn.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.corvus.vpn.vpn.VpnManager
import dagger.hilt.android.AndroidEntryPoint
import de.blinkt.openvpn.core.CorvusNotificationHelper
import javax.inject.Inject

/**
 * VpnActionReceiver — Handles instant disconnect actions triggered directly
 * from the Corvus VPN notification without any confirmation dialogs.
 */
@AndroidEntryPoint
class VpnActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var vpnManager: VpnManager

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("VpnActionReceiver", "Received broadcast action: ${intent.action}")
        if (intent.action == CorvusNotificationHelper.ACTION_DISCONNECT) {
            CorvusNotificationHelper.cancelNotification(context)
            vpnManager.stopVpn()
        }
    }
}
