package com.corvus.vpn.vpn.engines

import com.corvus.vpn.vpn.model.VpnEngineType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnEngineFactory @Inject constructor(
    private val singBoxEngine: SingBoxEngine,
    private val openVpnEngine: OpenVpnEngine,
    private val ikev2Engine: Ikev2Engine
) {
    fun create(engineType: VpnEngineType): VpnEngine {
        return when (engineType) {
            VpnEngineType.SING_BOX -> singBoxEngine
            VpnEngineType.OPENVPN -> openVpnEngine
            VpnEngineType.IKEV2 -> ikev2Engine
        }
    }
}
