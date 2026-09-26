package com.corvus.vpn.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.corvus.vpn.data.ServerRepository
import com.corvus.vpn.data.SettingsRepository
import com.corvus.vpn.ui.servers.Server
import com.corvus.vpn.vpn.VpnManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var vpnManager: VpnManager

    @Inject
    lateinit var serverRepository: ServerRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.d("BootReceiver", "Boot action received: $action. AutoStart=${settingsRepository.autoStartEnabled}")
            if (settingsRepository.autoStartEnabled) {
                CoroutineScope(Dispatchers.IO).launch {
                    val servers = serverRepository.getServers()
                    val targetServer = servers.find { it.id == settingsRepository.lastConnectedServerId }
                        ?: servers.maxByOrNull { it.score ?: 0L }
                        ?: servers.firstOrNull()

                    if (targetServer != null) {
                        Log.d("BootReceiver", "Auto-starting VPN on boot for server=${targetServer.name}")
                        vpnManager.startVpn(
                            Server(
                                id = targetServer.id,
                                protocol = targetServer.protocol,
                                engine = targetServer.engine,
                                name = targetServer.name,
                                countryCode = targetServer.countryCode,
                                countryName = targetServer.countryName,
                                ping = targetServer.ping,
                                signal = 3,
                                ovpnConfig = targetServer.ovpnConfig,
                                tier = targetServer.tier
                            )
                        )
                    }
                }
            }
        }
    }
}
