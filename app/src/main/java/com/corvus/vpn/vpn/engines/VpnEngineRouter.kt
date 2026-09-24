package com.corvus.vpn.vpn.engines

import com.corvus.vpn.vpn.model.ConnectionState

enum class EngineType { OPENVPN }

data class GlobalVpnState(
    val engineType: EngineType = EngineType.OPENVPN,
    val state: ConnectionState = ConnectionState.DISCONNECTED,
    val error: String? = null
)
