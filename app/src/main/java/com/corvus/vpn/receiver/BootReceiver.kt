package com.corvus.vpn.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.corvus.vpn.data.SettingsRepository
import com.corvus.vpn.vpn.VpnManager
import com.corvus.vpn.data.ServerRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var vpnManager: VpnManager

    @Inject
    lateinit var serverRepository: ServerRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (settingsRepository.autoStartEnabled) {
                CoroutineScope(Dispatchers.IO).launch {
                    val servers = serverRepository.getServers()
                    val firstServer = servers.firstOrNull()
                    if (firstServer != null) {
                        vpnManager.startVpn(com.corvus.vpn.ui.servers.Server(
                            id = firstServer.id,
                            protocol = firstServer.protocol,
                            engine = firstServer.engine,
                            name = firstServer.name,
                            countryCode = firstServer.countryCode,
                            countryName = firstServer.countryName,
                            ping = firstServer.ping,
                            signal = 3
                        ))
                    }
                }
            }
        }
    }
}
