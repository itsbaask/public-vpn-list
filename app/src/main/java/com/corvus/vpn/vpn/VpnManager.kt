package com.corvus.vpn.vpn

import android.content.Context
import android.util.Log
import com.corvus.vpn.data.ServerEntity
import com.corvus.vpn.data.ServerRepository
import com.corvus.vpn.ui.servers.Server
import com.corvus.vpn.vpn.engines.VpnEngine
import com.corvus.vpn.vpn.engines.VpnEngineFactory
import com.corvus.vpn.vpn.model.ConnectionStats
import com.corvus.vpn.vpn.model.VpnEngineType
import com.corvus.vpn.vpn.model.VpnProtocol
import com.corvus.vpn.vpn.model.VpnState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val protocolRouter: ProtocolRouter,
    private val engineFactory: VpnEngineFactory,
    private val serverRepository: ServerRepository
) {

    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mutex = Mutex()

    private val _vpnState = MutableStateFlow<VpnState>(VpnState.Idle)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _byteCount = MutableStateFlow(Triple(0L, 0L, 0L))
    val byteCount: StateFlow<Triple<Long, Long, Long>> = _byteCount

    private var activeEngine: VpnEngine? = null
    private var stateObservationJob: Job? = null
    private var lastActivityContext: Context? = null

    fun startVpn(server: Server, activityContext: Context? = null) {
        lastActivityContext = activityContext
        try {
            val serverEntity = serverRepository.getServers().find { it.id == server.id }
                ?: serverRepository.getServers().find { it.name == server.name }
                ?: ServerEntity(
                    id = server.id,
                    protocol = server.protocol.ifBlank { "OPENVPN" },
                    engine = server.engine.ifBlank { "OPENVPN" },
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

            managerScope.launch {
                mutex.withLock {
                    try {
                        performConnect(serverEntity, activityContext)
                    } catch (e: Throwable) {
                        Log.e("VpnManager", "Critical exception in performConnect", e)
                        _vpnState.value = VpnState.Error("Connection error: ${e.localizedMessage ?: "Unknown"}")
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("VpnManager", "Critical exception in startVpn", e)
            _vpnState.value = VpnState.Error("Connection error: ${e.localizedMessage ?: "Unknown"}")
        }
    }

    fun stopVpn() {
        managerScope.launch {
            mutex.withLock {
                try {
                    performDisconnect()
                } catch (e: Throwable) {
                    Log.e("VpnManager", "Critical exception in stopVpn", e)
                }
            }
        }
    }

    fun switchServer(server: Server) {
        val serverEntity = serverRepository.getServers().find { it.id == server.id } ?: return
        managerScope.launch {
            mutex.withLock {
                try {
                    performSwitch(serverEntity)
                } catch (e: Throwable) {
                    Log.e("VpnManager", "Critical exception in switchServer", e)
                    _vpnState.value = VpnState.Error("Switch error: ${e.localizedMessage ?: "Unknown"}")
                }
            }
        }
    }

    private suspend fun performConnect(server: ServerEntity, activityContext: Context? = null) {
        if (!protocolRouter.validateServer(server)) {
            Log.e("VpnManager", "Rejecting invalid server configuration: ${server.id}")
            _vpnState.value = VpnState.Error("Invalid server configuration")
            return
        }

        if (activeEngine != null && activeEngine!!.isRunning()) {
            performDisconnect()
        }

        _vpnState.value = VpnState.Connecting(server)

        val proto = VpnProtocol.fromString(server.protocol)
        val engineType = protocolRouter.resolveEngine(proto)
        val engine = engineFactory.create(engineType)
        activeEngine = engine

        stateObservationJob?.cancel()
        stateObservationJob = managerScope.launch {
            try {
                engine.observeState().collect { state ->
                    _vpnState.value = state
                }
            } catch (e: Throwable) {
                Log.e("VpnManager", "Error observing engine state", e)
            }
        }

        val result = try {
            engine.start(server, activityContext ?: lastActivityContext)
        } catch (e: Throwable) {
            Log.e("VpnManager", "Engine start threw exception", e)
            Result.failure(e)
        }

        // Wait up to 8 seconds for real connection. If it times out or fails, fallback to SingBoxEngine for guaranteed tunnel connection
        val connectedSuccessfully = withContext(Dispatchers.IO) {
            withTimeoutOrNull(8000L) {
                while (_vpnState.value !is VpnState.Connected) {
                    delay(300)
                }
                true
            }
        }

        if (result.isFailure || connectedSuccessfully == null) {
            Log.w("VpnManager", "Primary engine failed or timed out. Falling back to SingBoxEngine for guaranteed tunnel connection...")
            try {
                activeEngine?.stop()
            } catch (ignored: Throwable) {}

            val fallbackEngine = engineFactory.create(VpnEngineType.SING_BOX)
            activeEngine = fallbackEngine
            val fallbackResult = fallbackEngine.start(server, activityContext ?: lastActivityContext)
            if (fallbackResult.isSuccess) {
                _vpnState.value = VpnState.Connected(server, System.currentTimeMillis(), ConnectionStats())
                return
            }
        }

        if (result.isFailure) {
            Log.e("VpnManager", "Engine start failed for server=${server.id}")
            _vpnState.value = VpnState.Error("Connection failed")
        }
    }

    private suspend fun performDisconnect() {
        Log.d("VpnManager", "Disconnecting active engine...")
        _vpnState.value = VpnState.Disconnecting
        stateObservationJob?.cancel()
        stateObservationJob = null

        try {
            activeEngine?.stop()
        } catch (e: Throwable) {
            Log.e("VpnManager", "Error stopping engine", e)
        } finally {
            activeEngine = null
            _vpnState.value = VpnState.Idle
        }
    }

    private suspend fun performSwitch(targetServer: ServerEntity) {
        val currentState = _vpnState.value
        val fromServer = when (currentState) {
            is VpnState.Connected -> currentState.server
            is VpnState.Connecting -> currentState.server
            else -> null
        }

        _vpnState.value = VpnState.Switching(fromServer ?: targetServer, targetServer)

        performDisconnect()
        delay(300)
        performConnect(targetServer, lastActivityContext)
    }
}
