package com.corvus.vpn.vpn

import android.content.Context
import android.util.Log
import com.corvus.vpn.data.ServerEntity
import com.corvus.vpn.data.ServerRepository
import com.corvus.vpn.ui.servers.Server
import com.corvus.vpn.vpn.engines.VpnConnectionEngine
import com.corvus.vpn.vpn.model.VpnCommand
import com.corvus.vpn.vpn.model.VpnState
import dagger.hilt.android.qualifiers.ApplicationContext
import de.blinkt.openvpn.core.VpnStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectionEngine: VpnConnectionEngine,
    private val serverRepository: ServerRepository
) : VpnStatus.ByteCountListener {

    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val vpnState: StateFlow<VpnState> = connectionEngine.state

    private val _byteCount = MutableStateFlow(Triple(0L, 0L, 0L))
    val byteCount: StateFlow<Triple<Long, Long, Long>> = _byteCount

    init {
        VpnStatus.addByteCountListener(this)
    }

    fun startVpn(server: Server) {
        val serverEntity = serverRepository.getServers().find { it.id == server.id }
            ?: serverRepository.getServers().find { it.name == server.name }
            ?: serverRepository.getServers().firstOrNull()

        val entityToConnect = serverEntity ?: ServerEntity(
            id = server.id,
            protocol = server.protocol,
            engine = server.engine,
            name = server.name,
            countryCode = server.countryCode ?: "US",
            countryName = server.countryName ?: "Custom Server",
            flag = "🌐",
            ping = server.ping,
            speed = server.speed,
            score = 1000L,
            ovpnConfig = server.ovpnConfig,
            tier = server.tier
        )

        connectionEngine.processCommand(VpnCommand.Connect(entityToConnect))
    }

    fun stopVpn() {
        connectionEngine.processCommand(VpnCommand.Disconnect)
    }

    fun switchServer(server: Server) {
        val serverEntity = serverRepository.getServers().find { it.id == server.id } ?: return
        connectionEngine.processCommand(VpnCommand.Switch(serverEntity))
    }

    override fun updateByteCount(inBytes: Long, outBytes: Long, diffIn: Long, diffOut: Long) {
        _byteCount.value = Triple(inBytes, outBytes, diffIn)
    }
}
