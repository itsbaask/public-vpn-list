package com.corvus.vpn.vpn.engines

import android.content.Context
import android.net.Ikev2VpnProfile
import android.net.VpnManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.corvus.vpn.R
import com.corvus.vpn.data.ServerEntity
import com.corvus.vpn.vpn.model.ConnectionStats
import com.corvus.vpn.vpn.model.VpnState
import com.corvus.vpn.vpn.model.VpnStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ikev2Engine — Real IKEv2/IPSec implementation using Android's built-in VPN framework.
 *
 * Uses [android.net.VpnManager] + [android.net.Ikev2VpnProfile] (API 29+) for real IKEv2 tunnels.
 * Falls back to a clear error on API < 29 instead of a fake connection.
 *
 * IKEv2 server requirements:
 * - The server must support IKEv2/IPSec with either:
 *   a) Username/password (EAP-MSCHAPv2)
 *   b) RSA certificate
 *   c) Pre-shared key (PSK)
 * - The server.host field must contain the IKEv2 gateway address
 */
@Singleton
class Ikev2Engine @Inject constructor(
    @ApplicationContext private val context: Context
) : VpnEngine {

    companion object {
        private const val TAG = "Ikev2Engine"
        private const val CONNECTION_TIMEOUT_MS = 20_000L
    }

    private val _state = MutableStateFlow<VpnState>(VpnState.Idle)
    private var running = false
    private val stats = VpnStats()
    private var connectionTimeoutJob: Job? = null

    override suspend fun start(server: ServerEntity, activityContext: Context?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting IKEv2 engine for server=${server.name} host=${server.host}")
            _state.value = VpnState.Connecting(server)
            running = true

            // IKEv2/IPSec via VpnManager requires API 29+
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Log.w(TAG, "IKEv2 VpnManager API requires Android 10+. Device is API ${Build.VERSION.SDK_INT}.")
                _state.value = VpnState.Error("IKEv2 requires Android 10 or newer. Your device runs Android ${Build.VERSION.RELEASE}.")
                running = false
                return@withContext Result.failure(UnsupportedOperationException("IKEv2 requires API 29+"))
            }

            val host = server.host.ifBlank {
                Log.e(TAG, "IKEv2 server has no host address — server.id=${server.id}")
                _state.value = VpnState.Error("IKEv2 server has no gateway address.")
                running = false
                return@withContext Result.failure(IllegalArgumentException("IKEv2 server host is blank"))
            }

            // Parse credentials from config or URI
            val (username, password, psk) = parseIkev2Credentials(server)

            // Connection timeout watchdog (20 seconds)
            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = CoroutineScope(Dispatchers.Main).launch {
                delay(CONNECTION_TIMEOUT_MS)
                if (_state.value is VpnState.Connecting) {
                    Log.w(TAG, "IKEv2 connection timeout for server=${server.name}")
                    stop()
                    _state.value = VpnState.Error(context.getString(R.string.connection_failed_try_another))
                }
            }

            // Build and provision the IKEv2 profile
            val profile = buildIkev2Profile(host, username, password, psk, server)

            if (profile == null) {
                connectionTimeoutJob?.cancel()
                _state.value = VpnState.Error("IKEv2 configuration is incomplete — missing credentials or server settings.")
                running = false
                return@withContext Result.failure(IllegalStateException("Failed to build IKEv2 profile"))
            }

            val vpnManager = context.getSystemService(Context.VPN_MANAGEMENT_SERVICE) as android.net.VpnManager
            vpnManager.provisionVpnProfile(profile)

            Log.d(TAG, "IKEv2 profile provisioned. Starting connection to $host")
            vpnManager.startProvisionedVpnProfile()

            // Monitor the connection until confirmed or timeout
            monitorIkev2Connection(server, vpnManager)

            Result.success(Unit)

        } catch (e: Exception) {
            connectionTimeoutJob?.cancel()
            Log.e(TAG, "IKEv2 start failed for server=${server.id}", e)
            _state.value = VpnState.Error("IKEv2 connection failed: ${e.localizedMessage ?: "Unknown error"}")
            running = false
            Result.failure(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun buildIkev2Profile(
        host: String,
        username: String?,
        password: String?,
        psk: String?,
        server: ServerEntity
    ): Ikev2VpnProfile? {
        return try {
            val builder = Ikev2VpnProfile.Builder(host, host)

            when {
                !username.isNullOrBlank() && !password.isNullOrBlank() -> {
                    // EAP-MSCHAPv2 authentication (most common for IKEv2)
                    builder.setAuthUsernamePassword(username, password, null)
                    Log.d(TAG, "IKEv2 auth: EAP-MSCHAPv2 for user=$username")
                }
                !psk.isNullOrBlank() -> {
                    // Pre-shared key authentication
                    builder.setAuthPsk(psk.toByteArray())
                    Log.d(TAG, "IKEv2 auth: PSK")
                }
                else -> {
                    // Fallback: try EAP with default credentials
                    Log.w(TAG, "IKEv2: No credentials found, using default vpn/vpn")
                    builder.setAuthUsernamePassword("vpn", "vpn", null)
                }
            }

            builder.setBypassable(false)
            builder.build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build IKEv2 profile: ${e.message}", e)
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun monitorIkev2Connection(server: ServerEntity, vpnManager: VpnManager) {
        CoroutineScope(Dispatchers.IO).launch {
            // Poll VPN state for up to 30s
            val deadline = System.currentTimeMillis() + 30_000L
            while (System.currentTimeMillis() < deadline && running) {
                delay(1000)
                // Android VPN framework fires system connectivity events.
                // We check that VPN is still active by trying a quick network probe.
                // The definitive "CONNECTED" is when we can reach a known IP.
                // For now, we use a simple liveness check.
                if (isVpnConnected()) {
                    connectionTimeoutJob?.cancel()
                    running = true
                    _state.value = VpnState.Connected(server, System.currentTimeMillis(), ConnectionStats())
                    Log.d(TAG, "IKEv2 VPN connected for server=${server.name}")
                    return@launch
                }
            }
            // If we reach here, the connection wasn't confirmed
            if (_state.value is VpnState.Connecting) {
                connectionTimeoutJob?.cancel()
                running = false
                _state.value = VpnState.Error("IKEv2 connection was not confirmed by the system.")
            }
        }
    }

    private fun isVpnConnected(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val networks = cm.allNetworks
            networks.any { network ->
                val caps = cm.getNetworkCapabilities(network) ?: return@any false
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
            }
        } catch (_: Exception) {
            false
        }
    }

    private data class Credentials(val username: String?, val password: String?, val psk: String?)

    private fun parseIkev2Credentials(server: ServerEntity): Credentials {
        val config = server.ovpnConfig ?: ""
        // Try to extract credentials from ikev2://user:pass@host format
        val uri = config.takeIf { it.startsWith("ikev2://") }
        if (uri != null) {
            val userInfo = uri.substringAfter("ikev2://").substringBefore("@")
            if (":" in userInfo) {
                val user = userInfo.substringBefore(":").trim()
                val pass = userInfo.substringAfter(":").trim()
                return Credentials(user.ifBlank { null }, pass.ifBlank { null }, null)
            }
        }
        // No credentials found — caller will use defaults
        return Credentials(null, null, null)
    }

    override suspend fun stop() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Stopping IKEv2 engine")
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = null
        running = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val vpnManager = context.getSystemService(Context.VPN_MANAGEMENT_SERVICE) as VpnManager
                vpnManager.stopProvisionedVpnProfile()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping IKEv2 VPN: ${e.message}")
        }
        _state.value = VpnState.Idle
    }

    override suspend fun restart(server: ServerEntity, activityContext: Context?): Result<Unit> {
        stop()
        return start(server, activityContext)
    }

    override fun isRunning(): Boolean = running

    override fun observeState(): Flow<VpnState> = _state.asStateFlow()

    override fun getStats(): VpnStats = stats
}
