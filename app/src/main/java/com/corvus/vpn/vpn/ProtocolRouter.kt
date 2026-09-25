package com.corvus.vpn.vpn

import com.corvus.vpn.data.ServerEntity
import com.corvus.vpn.vpn.model.VpnEngineType
import com.corvus.vpn.vpn.model.VpnProtocol
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtocolRouter @Inject constructor() {

    fun resolveEngine(protocol: VpnProtocol): VpnEngineType {
        return when (protocol) {
            VpnProtocol.VLESS,
            VpnProtocol.VMESS,
            VpnProtocol.TROJAN,
            VpnProtocol.SHADOWSOCKS,
            VpnProtocol.HYSTERIA2,
            VpnProtocol.TUIC,
            VpnProtocol.WIREGUARD -> VpnEngineType.SING_BOX

            VpnProtocol.OPENVPN -> VpnEngineType.OPENVPN
            VpnProtocol.IKEV2 -> VpnEngineType.IKEV2
        }
    }

    fun resolveEngine(protocolString: String): VpnEngineType {
        val proto = VpnProtocol.fromString(protocolString)
        return resolveEngine(proto)
    }

    /**
     * Validates a server entity for required fields, protocol, engine, and protocol/engine compatibility.
     * Returns true if valid, false if invalid or unsupported.
     */
    fun validateServer(server: ServerEntity): Boolean {
        if (server.id.isBlank()) {
            println("ProtocolRouter: Validation failed: Server id is blank")
            return false
        }

        val protocol = try {
            VpnProtocol.fromString(server.protocol)
        } catch (e: Exception) {
            println("ProtocolRouter: Validation failed: Invalid protocol '${server.protocol}' for server ${server.id}")
            return false
        }

        val expectedEngine = resolveEngine(protocol)
        val declaredEngineStr = server.engine.uppercase()

        val declaredEngine = try {
            when {
                "SING" in declaredEngineStr || "PROXY" in declaredEngineStr -> VpnEngineType.SING_BOX
                "OPEN" in declaredEngineStr -> VpnEngineType.OPENVPN
                "IKE" in declaredEngineStr -> VpnEngineType.IKEV2
                else -> VpnEngineType.valueOf(declaredEngineStr)
            }
        } catch (e: Exception) {
            println("ProtocolRouter: Warning: Engine '${server.engine}' missing or unrecognized for server ${server.id}. Deriving from protocol $protocol.")
            expectedEngine
        }

        if (declaredEngine != expectedEngine) {
            println("ProtocolRouter: SECURITY / CONFIG ERROR: Protocol/Engine mismatch for server ${server.id}! Protocol=$protocol requires $expectedEngine, but declared engine=$declaredEngine. REJECTING.")
            return false
        }

        return true
    }
}
