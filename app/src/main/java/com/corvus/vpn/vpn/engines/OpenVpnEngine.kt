package com.corvus.vpn.vpn.engines

import android.content.Context
import android.content.Intent
import android.util.Log
import com.corvus.vpn.R
import com.corvus.vpn.data.ServerEntity
import com.corvus.vpn.data.ServerRepository
import com.corvus.vpn.vpn.model.ConnectionStats
import com.corvus.vpn.vpn.model.VpnState
import com.corvus.vpn.vpn.model.VpnStats
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VPNLaunchHelper
import de.blinkt.openvpn.core.VpnStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenVpnEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository
) : VpnEngine, VpnStatus.StateListener, VpnStatus.LogListener {

    private val _legacyEngineState = MutableStateFlow(ConnectionStatus.LEVEL_NOTCONNECTED)
    val legacyEngineState: StateFlow<ConnectionStatus> = _legacyEngineState.asStateFlow()

    private val _state = MutableStateFlow<VpnState>(VpnState.Idle)
    private val stats = VpnStats()
    private var activeServer: ServerEntity? = null
    private var running = false
    private var connectionTimeoutJob: Job? = null

    init {
        try {
            VpnStatus.addStateListener(this)
            VpnStatus.addLogListener(this)
            val active = VpnStatus.isVPNActive()
            _legacyEngineState.value = if (active) ConnectionStatus.LEVEL_CONNECTED else ConnectionStatus.LEVEL_NOTCONNECTED
            
            de.blinkt.openvpn.core.Preferences.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean("showlogwindow", true)
                .putBoolean("disableconfirmation", true)
                .apply()
        } catch (e: Throwable) {
            Log.e("OpenVpnEngine", "Error in init", e)
        }
    }

    override suspend fun start(server: ServerEntity, activityContext: Context?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("OpenVpnEngine", "Start requested for server=${server.name}")
            activeServer = server
            _state.value = VpnState.Connecting(server)
            running = true

            // 20-second connection timeout watchdog (as requested by user)
            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = CoroutineScope(Dispatchers.Main).launch {
                delay(20000L)
                val currentState = _state.value
                if (currentState is VpnState.Connecting) {
                    Log.w("OpenVpnEngine", "Connection timeout reached for server=${server.name}")
                    stop()
                    _state.value = VpnState.Error(context.getString(R.string.connection_failed_try_another))
                }
            }

            val config = serverRepository.fetchFullConfig(server.id)
            if (config.isNullOrBlank()) {
                connectionTimeoutJob?.cancel()
                _state.value = VpnState.Error(context.getString(R.string.connection_failed_try_another))
                return@withContext Result.failure(IllegalStateException("No valid OpenVPN configuration available for server=${server.id}"))
            }

            val cp = ConfigParser()
            cp.parseConfig(StringReader(config))
            val vp = cp.convertProfile()
            vp.mName = server.name

            // Prevent KeyChain null alias crash
            if (vp.mAlias == null) {
                vp.mAlias = "vpn"
            }
            if (vp.mAuthenticationType == VpnProfile.TYPE_KEYSTORE || (vp.mClientCertFilename.isNullOrEmpty() && vp.mPKCS12Filename.isNullOrEmpty() && vp.mAlias.isNullOrEmpty())) {
                vp.mAuthenticationType = VpnProfile.TYPE_USERPASS
            }

            if ("auth-user-pass" in config.lowercase() || vp.mUsername.isNullOrEmpty()) {
                if (vp.mUsername.isNullOrEmpty()) vp.mUsername = "vpn"
                if (vp.mPassword.isNullOrEmpty()) vp.mPassword = "vpn"
                vp.mAuthenticationType = VpnProfile.TYPE_USERPASS
            }

            vp.mAuthRetry = VpnProfile.AUTH_RETRY_NOINTERACT
            vp.mUseLegacyProvider = true

            val pm = ProfileManager.getInstance(context)
            pm.addProfile(vp)
            ProfileManager.saveProfile(context, vp)
            ProfileManager.setConnectedVpnProfile(context, vp)

            val launchCtx = activityContext ?: context
            VPNLaunchHelper.startOpenVpn(vp, launchCtx, "AppConnection", false)

            Result.success(Unit)
        } catch (e: Throwable) {
            connectionTimeoutJob?.cancel()
            Log.e("OpenVpnEngine", "OpenVPN start failed for server=${server.id}", e)
            _state.value = VpnState.Error("OpenVPN start failed: ${e.localizedMessage ?: "Unknown error"}")
            Result.failure(e)
        }
    }

    override suspend fun stop() = withContext(Dispatchers.IO) {
        try {
            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = null
            Log.d("OpenVpnEngine", "Stop requested")
            running = false
            val stopIntent = Intent(context, OpenVPNService::class.java)
            stopIntent.action = OpenVPNService.DISCONNECT_VPN
            context.startService(stopIntent)
            ProfileManager.setConntectedVpnProfileDisconnected(context)
            _state.value = VpnState.Idle
            activeServer = null
        } catch (e: Throwable) {
            Log.e("OpenVpnEngine", "Error in stop", e)
        }
        Unit
    }

    override suspend fun restart(server: ServerEntity, activityContext: Context?): Result<Unit> {
        stop()
        return start(server, activityContext)
    }

    override fun isRunning(): Boolean = running

    override fun observeState(): Flow<VpnState> = _state.asStateFlow()

    override fun getStats(): VpnStats = stats

    override fun updateState(
        state: String,
        logmessage: String,
        localizedResId: Int,
        level: ConnectionStatus,
        intent: Intent?
    ) {
        try {
            Log.d("OpenVpnEngine", "State update: $state ($level) - Message: $logmessage")
            _legacyEngineState.value = level

            val server = activeServer
            if (server != null) {
                when (level) {
                    ConnectionStatus.LEVEL_CONNECTED -> {
                        connectionTimeoutJob?.cancel()
                        connectionTimeoutJob = null
                        running = true
                        _state.value = VpnState.Connected(server, System.currentTimeMillis(), ConnectionStats())
                    }
                    ConnectionStatus.LEVEL_NOTCONNECTED -> {
                        if (_state.value is VpnState.Connecting) {
                            connectionTimeoutJob?.cancel()
                            connectionTimeoutJob = null
                            running = false
                            _state.value = VpnState.Error(context.getString(R.string.connection_failed_try_another))
                        } else if (_state.value is VpnState.Disconnecting) {
                            connectionTimeoutJob?.cancel()
                            connectionTimeoutJob = null
                            running = false
                            _state.value = VpnState.Idle
                        }
                    }
                    ConnectionStatus.LEVEL_AUTH_FAILED -> {
                        connectionTimeoutJob?.cancel()
                        connectionTimeoutJob = null
                        running = false
                        _state.value = VpnState.Error(context.getString(R.string.connection_failed_try_another))
                    }
                    ConnectionStatus.LEVEL_NONETWORK -> {
                        connectionTimeoutJob?.cancel()
                        connectionTimeoutJob = null
                        running = false
                        _state.value = VpnState.Error(context.getString(R.string.connection_failed_try_another))
                    }
                    else -> {}
                }
            }
        } catch (e: Throwable) {
            Log.e("OpenVpnEngine", "Error in updateState", e)
        }
    }

    override fun newLog(logItem: de.blinkt.openvpn.core.LogItem?) {
        Log.i("OpenVpnLog", logItem?.toString() ?: "")
    }

    override fun setConnectedVPN(uuid: String?) {
        try {
            // No-op
        } catch (e: Throwable) {
            // Ignored
        }
    }
}
