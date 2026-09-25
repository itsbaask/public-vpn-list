package com.corvus.vpn.vpn

import com.corvus.vpn.data.ServerEntity
import com.corvus.vpn.vpn.model.VpnEngineType
import com.corvus.vpn.vpn.model.VpnProtocol
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProtocolRouterTest {

    private lateinit var router: ProtocolRouter

    @Before
    fun setUp() {
        router = ProtocolRouter()
    }

    @Test
    fun testProtocolEngineMapping() {
        assertEquals(VpnEngineType.SING_BOX, router.resolveEngine(VpnProtocol.VLESS))
        assertEquals(VpnEngineType.SING_BOX, router.resolveEngine(VpnProtocol.VMESS))
        assertEquals(VpnEngineType.SING_BOX, router.resolveEngine(VpnProtocol.TROJAN))
        assertEquals(VpnEngineType.SING_BOX, router.resolveEngine(VpnProtocol.SHADOWSOCKS))
        assertEquals(VpnEngineType.SING_BOX, router.resolveEngine(VpnProtocol.HYSTERIA2))
        assertEquals(VpnEngineType.SING_BOX, router.resolveEngine(VpnProtocol.TUIC))
        assertEquals(VpnEngineType.SING_BOX, router.resolveEngine(VpnProtocol.WIREGUARD))

        assertEquals(VpnEngineType.OPENVPN, router.resolveEngine(VpnProtocol.OPENVPN))
        assertEquals(VpnEngineType.IKEV2, router.resolveEngine(VpnProtocol.IKEV2))
    }

    @Test
    fun testValidServerValidation() {
        val validVless = ServerEntity(
            id = "de-01",
            protocol = "vless",
            engine = "sing_box",
            name = "Germany 01"
        )
        assertTrue(router.validateServer(validVless))

        val validOpenVpn = ServerEntity(
            id = "us-01",
            protocol = "openvpn",
            engine = "openvpn",
            name = "US 01"
        )
        assertTrue(router.validateServer(validOpenVpn))

        val validIkev2 = ServerEntity(
            id = "uk-01",
            protocol = "ikev2",
            engine = "ikev2",
            name = "UK 01"
        )
        assertTrue(router.validateServer(validIkev2))
    }

    @Test
    fun testInvalidServerValidationMismatch() {
        val mismatchServer = ServerEntity(
            id = "malformed-01",
            protocol = "vless",
            engine = "openvpn", // Conflict! VLESS requires sing_box
            name = "Bad Server"
        )
        assertFalse(router.validateServer(mismatchServer))
    }

    @Test
    fun testInvalidServerBlankId() {
        val blankIdServer = ServerEntity(
            id = "",
            protocol = "vless",
            engine = "sing_box",
            name = "Blank ID"
        )
        assertFalse(router.validateServer(blankIdServer))
    }
}
