package com.corvus.vpn.vpn.engines

import com.corvus.vpn.vpn.model.VpnCommand
import com.corvus.vpn.vpn.model.VpnState
import kotlinx.coroutines.flow.StateFlow

interface ConnectionEngine {
    /**
     * The current state of the VPN connection.
     */
    val state: StateFlow<VpnState>

    /**
     * Send a command to the engine. Commands are processed sequentially.
     */
    fun processCommand(command: VpnCommand)
}
