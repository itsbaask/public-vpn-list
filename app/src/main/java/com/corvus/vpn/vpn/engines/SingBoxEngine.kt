package com.corvus.vpn.vpn.engines

import android.content.Context
import android.net.VpnService
import android.util.Log
import com.corvus.vpn.data.ServerEntity
import com.corvus.vpn.data.ServerRepository
import com.corvus.vpn.vpn.model.ConnectionStats
import com.corvus.vpn.vpn.model.VpnState
import com.corvus.vpn.vpn.model.VpnStats
import com.corvus.vpn.vpn.services.SingBoxVpnService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SingBoxEngine — Real implementation supporting modern VPN protocols:
 * VLESS, VMess, Trojan, Shadowsocks, Hysteria2, TUIC, WireGuard.
 *
 * Architecture:
 * - Parses the config URI received from the backend (e.g. vless://..., ss://..., trojan://...)
 * - Starts [SingBoxVpnService] which builds a TUN interface via Android VpnService.Builder
 * - The service runs the actual sing-box tunnel in a background thread
 * - Engine observes the service state and propagates it to VpnManager
 */
@Singleton
class SingBoxEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository
) : VpnEngine {

    companion object {
        private const val TAG = "SingBoxEngine"
        private const val CONNECTION_TIMEOUT_MS = 30_000L

        // Supported URI schemes
        private val SUPPORTED_SCHEMES = setOf(
            "vless", "vmess", "trojan", "ss", "shadowsocks",
            "hysteria2", "hy2", "tuic", "wireguard", "wg"
        )
    }

    private val _state = MutableStateFlow<VpnState>(VpnState.Idle)
    private var running = false
    private val stats = VpnStats()
    private var activeServer: ServerEntity? = null
    private var connectionTimeoutJob: Job? = null
    private var stateObserverJob: Job? = null

    override suspend fun start(server: ServerEntity, activityContext: Context?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sing-box engine: protocol=${server.protocol}, server=${server.name}")
            activeServer = server
            _state.value = VpnState.Connecting(server)
            running = true

            // Resolve the connection URI from the server entity
            val connectionUri = resolveConnectionUri(server)
            if (connectionUri == null) {
                Log.e(TAG, "No valid connection URI found for server=${server.id} (protocol=${server.protocol})")
                _state.value = VpnState.Error("No connection configuration available for ${server.protocol} server. Please refresh the server list.")
                running = false
                return@withContext Result.failure(IllegalStateException("Missing connection URI for server=${server.id}"))
            }

            Log.d(TAG, "Resolved connection URI scheme=${extractScheme(connectionUri)} for server=${server.id}")

            // Validate URI scheme
            val scheme = extractScheme(connectionUri)
            if (scheme !in SUPPORTED_SCHEMES) {
                Log.e(TAG, "Unsupported protocol scheme: $scheme for server=${server.id}")
                _state.value = VpnState.Error("Protocol '$scheme' is not supported by this engine.")
                running = false
                return@withContext Result.failure(UnsupportedOperationException("Unsupported scheme: $scheme"))
            }

            // Check VPN permission
            val permissionIntent = VpnService.prepare(context)
            if (permissionIntent != null) {
                Log.w(TAG, "VPN permission not yet granted. Activity must call startActivityForResult.")
                _state.value = VpnState.Error("VPN permission required. Please grant permission when prompted.")
                running = false
                return@withContext Result.failure(SecurityException("VPN permission not granted"))
            }

            // Start the SingBoxVpnService with the connection URI and server metadata
            val started = SingBoxVpnService.startTunnel(
                context = activityContext ?: context,
                connectionUri = connectionUri,
                server = server
            )

            if (!started) {
                Log.e(TAG, "Failed to start SingBoxVpnService for server=${server.id}")
                _state.value = VpnState.Error("Failed to start VPN service. Please try again.")
                running = false
                return@withContext Result.failure(RuntimeException("SingBoxVpnService failed to start"))
            }

            // Start connection timeout watchdog
            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = CoroutineScope(Dispatchers.Main).launch {
                delay(CONNECTION_TIMEOUT_MS)
                if (_state.value is VpnState.Connecting) {
                    Log.w(TAG, "Sing-box connection timeout for server=${server.name}")
                    stop()
                    _state.value = VpnState.Error("Connection timeout — server unreachable or blocked.")
                }
            }

            // Observe SingBoxVpnService state
            stateObserverJob?.cancel()
            stateObserverJob = CoroutineScope(Dispatchers.IO).launch {
                SingBoxVpnService.observeState().collect { serviceState ->
                    when (serviceState) {
                        SingBoxVpnService.TunnelState.CONNECTED -> {
                            connectionTimeoutJob?.cancel()
                            running = true
                            _state.value = VpnState.Connected(
                                server = server,
                                connectedSince = System.currentTimeMillis(),
                                stats = ConnectionStats()
                            )
                            Log.d(TAG, "Sing-box tunnel connected for server=${server.name}")
                        }
                        SingBoxVpnService.TunnelState.DISCONNECTED -> {
                            if (_state.value !is VpnState.Idle) {
                                connectionTimeoutJob?.cancel()
                                running = false
                                _state.value = VpnState.Idle
                            }
                        }
                        SingBoxVpnService.TunnelState.ERROR -> {
                            connectionTimeoutJob?.cancel()
                            running = false
                            _state.value = VpnState.Error("Tunnel connection failed — server rejected the connection.")
                            Log.e(TAG, "SingBoxVpnService reported ERROR for server=${server.name}")
                        }
                        SingBoxVpnService.TunnelState.CONNECTING -> {
                            // Already in Connecting state — no update needed
                        }
                    }
                }
            }

            Result.success(Unit)

        } catch (e: Exception) {
            connectionTimeoutJob?.cancel()
            Log.e(TAG, "Failed to start sing-box engine for server=${server.id}", e)
            _state.value = VpnState.Error("Connection failed: ${e.localizedMessage ?: "Unknown error"}")
            running = false
            Result.failure(e)
        }
    }

    override suspend fun stop() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Stopping sing-box engine")
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = null
        stateObserverJob?.cancel()
        stateObserverJob = null
        running = false

        try {
            SingBoxVpnService.stopTunnel(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping SingBoxVpnService: ${e.message}")
        }

        _state.value = VpnState.Idle
        activeServer = null
    }

    override suspend fun restart(server: ServerEntity, activityContext: Context?): Result<Unit> {
        stop()
        return start(server, activityContext)
    }

    override fun isRunning(): Boolean = running

    override fun observeState(): Flow<VpnState> = _state.asStateFlow()

    override fun getStats(): VpnStats = stats

    // ─────────────────────────────────────────────────
    // URI Resolution helpers
    // ─────────────────────────────────────────────────

    /**
     * Resolves the connection URI for a server in priority order:
     * 1. ServerRepository.getConnectionUri() — checks ovpnConfig/configUri fields for a URI scheme
     * 2. server.ovpnConfig directly (if it's a URI)
     * 3. server.configUri directly (if it's a URI)
     */
    private fun resolveConnectionUri(server: ServerEntity): String? {
        // Try repository lookup first (handles cache)
        val repoUri = serverRepository.getConnectionUri(server.id)
        if (repoUri != null) return repoUri

        // Direct check on entity fields
        val candidates = listOf(server.ovpnConfig, server.configUri)
        for (candidate in candidates) {
            if (!candidate.isNullOrBlank() && isValidConnectionUri(candidate)) {
                return candidate
            }
        }

        // Last resort: construct from host/port if we have them for known URI-based protocols
        val proto = server.protocol.lowercase()
        if (proto != "openvpn" && proto != "ikev2" && server.host.isNotBlank()) {
            val portPart = if (server.port > 0) ":${server.port}" else ""
            return "$proto://${server.host}$portPart"
        }

        return null
    }

    private fun isValidConnectionUri(value: String): Boolean {
        if (!value.contains("://")) return false
        val scheme = extractScheme(value)
        return scheme in SUPPORTED_SCHEMES
    }

    private fun extractScheme(uri: String): String {
        return uri.substringBefore("://").lowercase()
    }
}
