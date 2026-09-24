package com.corvus.vpn.vpn

import com.corvus.vpn.vpn.model.ConnectionConfig
import com.corvus.vpn.vpn.model.ProtocolType
import org.junit.Assert.assertEquals
import org.junit.Test

class EngineTestFixtures {

    companion object {
        val OpenVpnTestConfig = ConnectionConfig.OpenVpnConfig(
            ovpnConfig = "client\ndev tun\nproto udp\nremote 127.0.0.1 1194\n"
        )

        val SingBoxTestConfig = ConnectionConfig.SingBoxConfig(
            protocol = ProtocolType.VLESS,
            configJsonOrUri = "vless://sample-uuid@127.0.0.1:443?encryption=none"
        )

        val ProxyTestConfig = ConnectionConfig.ProxyConfig(
            protocol = ProtocolType.SOCKS5,
            host = "127.0.0.1",
            port = 1080
        )
    }

    @Test
    fun testConfigsInstantiation() {
        assertEquals(ProtocolType.OPENVPN_UDP, OpenVpnTestConfig.protocol)
        assertEquals(ProtocolType.VLESS, SingBoxTestConfig.protocol)
        assertEquals(ProtocolType.SOCKS5, ProxyTestConfig.protocol)
        assertEquals("127.0.0.1", ProxyTestConfig.host)
        assertEquals(1080, ProxyTestConfig.port)
    }
}
