package com.corvus.vpn.vpn.model

import com.corvus.vpn.data.ServerEntity

/**
 * Commands that can be sent to the Connection Engine.
 */
sealed class VpnCommand {
    data class Connect(val server: ServerEntity) : VpnCommand()
    object Disconnect : VpnCommand()
    data class Switch(val targetServer: ServerEntity) : VpnCommand()
    object SelectBestAndConnect : VpnCommand()
}
