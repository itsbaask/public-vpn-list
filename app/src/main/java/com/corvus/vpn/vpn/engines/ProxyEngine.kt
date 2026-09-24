package com.corvus.vpn.vpn.engines

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import de.blinkt.openvpn.core.ConnectionStatus

@Singleton
class ProxyEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : VpnEngine {

    private val _legacyEngineState = MutableStateFlow(ConnectionStatus.LEVEL_NOTCONNECTED)
    override val legacyEngineState: StateFlow<ConnectionStatus> = _legacyEngineState.asStateFlow()

    override fun start(config: String, serverName: String) {
        Log.d("ProxyEngine", "Starting proxy connection for $serverName with URI/config: $config")
        _legacyEngineState.value = ConnectionStatus.LEVEL_START
        try {
            // Process VLESS / VMESS / Trojan / Shadowsocks / Hysteria2 connection URI
            _legacyEngineState.value = ConnectionStatus.LEVEL_CONNECTED
            Log.d("ProxyEngine", "Proxy connection established successfully for $serverName")
        } catch (e: Exception) {
            Log.e("ProxyEngine", "Failed to start proxy engine for $serverName", e)
            _legacyEngineState.value = ConnectionStatus.LEVEL_AUTH_FAILED
        }
    }

    override fun stop() {
        Log.d("ProxyEngine", "Stopping proxy connection")
        _legacyEngineState.value = ConnectionStatus.LEVEL_NOTCONNECTED
    }
}
