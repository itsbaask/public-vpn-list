package com.corvus.vpn.vpn.model

sealed class ConnectionConfig {
    abstract val type: ConnectionType
    abstract val protocol: ProtocolType

    data class OpenVpnConfig(
        val ovpnConfig: String,
        override val protocol: ProtocolType = ProtocolType.OPENVPN_UDP
    ) : ConnectionConfig() {
        override val type = ConnectionType.OPENVPN
    }

    data class SingBoxConfig(
        override val protocol: ProtocolType,
        val configJsonOrUri: String
    ) : ConnectionConfig() {
        override val type = ConnectionType.SING_BOX
    }

    data class ProxyConfig(
        override val protocol: ProtocolType,
        val host: String,
        val port: Int,
        val username: String? = null,
        val password: String? = null
    ) : ConnectionConfig() {
        override val type = ConnectionType.PROXY
    }
}
