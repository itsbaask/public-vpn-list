package com.corvus.vpn.vpn.model

import com.corvus.vpn.data.ServerEntity

/**
 * Represents the immutable stats of a VPN connection.
 */
data class ConnectionStats(
    val bytesIn: Long = 0,
    val bytesOut: Long = 0,
    val latency: Long = 0,
    val downloadSpeed: Long = 0, // bps
    val uploadSpeed: Long = 0    // bps
)

/**
 * The single source of truth for the VPN connection engine's state.
 */
sealed class VpnState {
    object Idle : VpnState()
    
    data class Connecting(val server: ServerEntity) : VpnState()
    
    data class Connected(
        val server: ServerEntity,
        val connectedSince: Long, // timestamp
        val stats: ConnectionStats = ConnectionStats()
    ) : VpnState()
    
    object Cancelling : VpnState()
    
    object Disconnecting : VpnState()
    
    data class Switching(
        val fromServer: ServerEntity,
        val toServer: ServerEntity
    ) : VpnState()
    
    data class Error(val reason: String) : VpnState()
}
