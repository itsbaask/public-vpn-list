package com.corvus.vpn.vpn.engines

import android.content.Context
import android.util.Log
import com.corvus.vpn.data.ServerEntity
import com.corvus.vpn.data.ServerRepository
import com.corvus.vpn.vpn.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import de.blinkt.openvpn.core.ConnectionStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnConnectionEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository,
    private val openVpnEngine: OpenVpnEngine,
    private val proxyEngine: ProxyEngine
) : ConnectionEngine {

    private val engineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val commandChannel = Channel<VpnCommand>(Channel.UNLIMITED)
    
    private val _state = MutableStateFlow<VpnState>(VpnState.Idle)
    override val state: StateFlow<VpnState> = _state.asStateFlow()

    private var connectJob: Job? = null
    private var activeEngine: VpnEngine = openVpnEngine

    init {
        startCommandProcessor()
        observeEngineStates()
    }

    private fun startCommandProcessor() {
        engineScope.launch {
            for (command in commandChannel) {
                handleCommand(command)
            }
        }
    }

    private fun observeEngineStates() {
        merge(openVpnEngine.legacyEngineState, proxyEngine.legacyEngineState)
            .onEach { level ->
                Log.d("VpnConnectionEngine", "Engine state changed to: $level")
                handleLegacyStateChange(level)
            }
            .launchIn(engineScope)
    }

    private var activeConnectingServer: ServerEntity? = null

    private fun handleLegacyStateChange(level: ConnectionStatus) {
        val currentState = _state.value
        Log.d("VpnConnectionEngine", "handleLegacyStateChange level=$level, currentState=$currentState")
        
        when (level) {
            ConnectionStatus.LEVEL_CONNECTED -> {
                val targetServer = when (currentState) {
                    is VpnState.Connecting -> currentState.server
                    is VpnState.Switching -> currentState.toServer
                    is VpnState.Connected -> currentState.server
                    else -> activeConnectingServer
                }
                if (targetServer != null) {
                    _state.value = VpnState.Connected(
                        server = targetServer,
                        connectedSince = System.currentTimeMillis()
                    )
                }
            }
            ConnectionStatus.LEVEL_NOTCONNECTED -> {
                if (currentState is VpnState.Connected || currentState is VpnState.Disconnecting || currentState is VpnState.Cancelling) {
                    _state.value = VpnState.Idle
                }
            }
            ConnectionStatus.LEVEL_AUTH_FAILED -> {
                _state.value = VpnState.Error("Authentication failed")
            }
            ConnectionStatus.LEVEL_NONETWORK -> {
                _state.value = VpnState.Error("No network connection")
            }
            else -> {
                // Keep current state
            }
        }
    }

    private var currentActivityContext: Context? = null

    override fun processCommand(command: VpnCommand) {
        commandChannel.trySend(command)
    }

    fun processCommand(command: VpnCommand, activityContext: Context?) {
        currentActivityContext = activityContext
        commandChannel.trySend(command)
    }

    private suspend fun handleCommand(command: VpnCommand) {
        Log.d("VpnConnectionEngine", "Handling command: $command | Current state: ${_state.value}")
        
        when (command) {
            is VpnCommand.Connect -> handleConnect(command.server)
            is VpnCommand.Disconnect -> handleDisconnect()
            is VpnCommand.Switch -> handleSwitch(command.targetServer)
            is VpnCommand.SelectBestAndConnect -> handleSelectBest()
        }
    }

    private suspend fun handleConnect(server: ServerEntity) {
        val currentState = _state.value
        if (currentState is VpnState.Connected || currentState is VpnState.Connecting) {
            if (currentState is VpnState.Connecting && currentState.server.id == server.id) return
            if (currentState is VpnState.Connected && currentState.server.id == server.id) return
            handleSwitch(server)
            return
        }

        if (currentState !is VpnState.Idle && currentState !is VpnState.Error) {
            Log.w("VpnConnectionEngine", "Ignore Connect command in state $currentState")
            return
        }

        performConnect(server)
    }

    private fun performConnect(server: ServerEntity) {
        activeConnectingServer = server
        _state.value = VpnState.Connecting(server)

        val actCtx = currentActivityContext
        currentActivityContext = null

        activeEngine = if (server.engine.uppercase() == "PROXY" || server.protocol.uppercase() != "OPENVPN") {
            proxyEngine
        } else {
            openVpnEngine
        }
        
        connectJob?.cancel()
        connectJob = engineScope.launch {
            try {
                // 1. Fetch config or URI with 25s timeout
                val config = withTimeout(25000L) {
                    serverRepository.fetchFullConfig(server.id)
                }
                
                yield() // Check for cancellation

                // 2. Start VPN with selected engine
                activeEngine.start(config, server.name, actCtx)
                
            } catch (e: CancellationException) {
                Log.d("VpnConnectionEngine", "Connection cancelled")
                _state.value = VpnState.Idle
            } catch (e: Exception) {
                Log.e("VpnConnectionEngine", "Connection failed", e)
                _state.value = VpnState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun handleDisconnect() {
        Log.d("VpnConnectionEngine", "handleDisconnect called")
        activeConnectingServer = null
        connectJob?.cancel()
        connectJob = null
        try {
            openVpnEngine.stop()
            proxyEngine.stop()
        } catch (e: Exception) {
            Log.e("VpnConnectionEngine", "Error stopping vpn engine", e)
        }
        _state.value = VpnState.Idle
    }

    private suspend fun handleSwitch(targetServer: ServerEntity) {
        val currentState = _state.value
        val fromServer = when (currentState) {
            is VpnState.Connected -> currentState.server
            is VpnState.Connecting -> currentState.server
            else -> null
        }

        _state.value = VpnState.Switching(fromServer ?: targetServer, targetServer)
        
        // 1. Disconnect current
        handleDisconnect()
        
        if (fromServer != null) {
            withTimeoutOrNull(3000L) {
                merge(openVpnEngine.legacyEngineState, proxyEngine.legacyEngineState)
                    .filter { it == ConnectionStatus.LEVEL_NOTCONNECTED }
                    .first()
            }
        }
        
        // 2. Connect new
        performConnect(targetServer)
    }

    private suspend fun handleSelectBest() {
        val bestServer = calculateBestServer()
        if (bestServer != null) {
            handleConnect(bestServer)
        } else {
            _state.value = VpnState.Error("No suitable server found")
        }
    }

    private fun calculateBestServer(): ServerEntity? {
        val servers = serverRepository.getServers()
        if (servers.isEmpty()) return null

        // High precision multi-factor auto selection formula
        return servers.maxByOrNull { server ->
            val pingVal = server.ping ?: 150
            val speedVal = server.speed ?: 10L
            val scoreVal = server.score ?: 100L

            val latencyScore = (1000 - pingVal).coerceAtLeast(0) * 1.5
            val speedScore = speedVal * 10.0
            val networkScore = (scoreVal / 1000.0).coerceAtMost(100.0)

            latencyScore + speedScore + networkScore
        }
    }
}
