package com.corvus.vpn.vpn.engines

import com.corvus.vpn.vpn.model.VpnState
import de.blinkt.openvpn.core.ConnectionStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * A lower-level interface for VPN drivers (OpenVPN, SingBox, etc.)
 */
interface VpnEngine {
    val legacyEngineState: StateFlow<ConnectionStatus>
    
    /**
     * Start the VPN with the given configuration string.
     */
    fun start(config: String, serverName: String)
    
    /**
     * Stop the VPN connection.
     */
    fun stop()
}
