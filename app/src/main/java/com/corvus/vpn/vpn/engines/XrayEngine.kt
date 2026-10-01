package com.corvus.vpn.vpn.engines

import android.content.Context
import android.util.Log
import com.corvus.vpn.R
import com.corvus.vpn.data.ServerEntity
import com.corvus.vpn.data.ServerRepository
import com.corvus.vpn.engine.XrayCoreEngine
import com.corvus.vpn.vpn.model.ConnectionStats
import com.corvus.vpn.vpn.model.VpnState
import com.corvus.vpn.vpn.model.VpnStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XrayEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository
) : VpnEngine {

    private val _state = MutableStateFlow<VpnState>(VpnState.Idle)
    private val stats = VpnStats()
    private var activeServer: ServerEntity? = null
    private var running = false
    private var connectionTimeoutJob: Job? = null
    private val xrayCore = XrayCoreEngine(context)

    override suspend fun start(server: ServerEntity, activityContext: Context?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("XrayEngine", "Start requested for server=${server.name} (${server.protocol})")
            activeServer = server
            _state.value = VpnState.Connecting(server)
            running = true

            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = CoroutineScope(Dispatchers.Main).launch {
                delay(20000L)
                val currentState = _state.value
                if (currentState is VpnState.Connecting) {
                    Log.w("XrayEngine", "Connection timeout reached for Xray server=${server.name}")
                    stop()
                    _state.value = VpnState.Error(context.getString(R.string.connection_failed_try_another))
                }
            }

            val config = serverRepository.fetchFullConfig(server.id) ?: server.ovpnConfig ?: server.configUri
            if (config.isNullOrBlank()) {
                Log.w("XrayEngine", "Config fetch blank for Xray server=${server.id}")
                return@withContext Result.failure(IllegalStateException("No valid Xray configuration available for server=${server.id}"))
            }

            // Start Android VpnService to establish kernel TUN interface
            val serviceIntent = android.content.Intent(context, CorvusXrayVpnService::class.java).apply {
                action = CorvusXrayVpnService.ACTION_START
            }
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.w("XrayEngine", "Error starting CorvusXrayVpnService: ${e.message}")
            }

            // Wait for VpnService to initialize and establish TUN interface
            var fd = -1
            var tries = 0
            while (tries < 15 && fd <= 0) {
                fd = CorvusXrayVpnService.instance?.establishTun() ?: CorvusXrayVpnService.tunFd?.fd ?: -1
                if (fd > 0) break
                delay(200L)
                tries++
            }

            Log.d("XrayEngine", "Starting Xray Core with TUN fd=$fd")
            xrayCore.initialize()
            xrayCore.start(config, if (fd > 0) fd else 0)

            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = null
            running = true
            _state.value = VpnState.Connected(server, System.currentTimeMillis(), ConnectionStats())

            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e("XrayEngine", "Xray start exception for server=${server.id}", e)
            stop()
            _state.value = VpnState.Error(e.localizedMessage ?: "Xray connection failed")
            Result.failure(e)
        }
    }

    override suspend fun stop() = withContext(Dispatchers.IO) {
        try {
            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = null
            Log.d("XrayEngine", "Stop requested")
            running = false

            xrayCore.stop()
            try {
                val stopIntent = android.content.Intent(context, CorvusXrayVpnService::class.java).apply {
                    action = CorvusXrayVpnService.ACTION_STOP
                }
                context.startService(stopIntent)
            } catch (e: Exception) {
                Log.w("XrayEngine", "Error stopping CorvusXrayVpnService: ${e.message}")
            }

            _state.value = VpnState.Idle
            activeServer = null
        } catch (e: Throwable) {
            Log.e("XrayEngine", "Error in stop", e)
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
}
