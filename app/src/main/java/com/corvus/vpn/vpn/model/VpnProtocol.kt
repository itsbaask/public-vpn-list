package com.corvus.vpn.vpn.model

import kotlinx.serialization.Serializable

@Serializable
enum class VpnEngineType {
    SING_BOX,
    OPENVPN,
    IKEV2
}

@Serializable
enum class VpnProtocol {
    VLESS,
    VMESS,
    TROJAN,
    SHADOWSOCKS,
    HYSTERIA2,
    TUIC,
    WIREGUARD,
    OPENVPN,
    IKEV2;

    companion object {
        fun fromString(value: String): VpnProtocol {
            return try {
                valueOf(value.uppercase().replace("-", "_").replace(".", "_"))
            } catch (e: Exception) {
                OPENVPN
            }
        }
    }
}

@Serializable
data class VpnStats(
    val bytesIn: Long = 0,
    val bytesOut: Long = 0,
    val latency: Long = 0,
    val downloadSpeed: Long = 0, // bps
    val uploadSpeed: Long = 0    // bps
)
