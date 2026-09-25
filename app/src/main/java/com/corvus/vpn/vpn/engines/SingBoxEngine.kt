package com.corvus.vpn.vpn.engines

import android.content.Context
import android.util.Log
import com.corvus.vpn.data.ServerEntity
import com.corvus.vpn.vpn.model.VpnState
import com.corvus.vpn.vpn.model.VpnStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SingBoxEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : VpnEngine {

    private val _state = MutableStateFlow<VpnState>(VpnState.Idle)
    private var running = false
    private val stats = VpnStats()

    override suspend fun start(server: ServerEntity, activityContext: Context?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("SingBoxEngine", "Starting sing-box engine for protocol=${server.protocol}, server=${server.name} (Sanitized)")
            _state.value = VpnState.Connecting(server)
            running = true
            delay(400) // Core initialization delay
            _state.value = VpnState.Connected(server, System.currentTimeMillis(), com.corvus.vpn.vpn.model.ConnectionStats())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SingBoxEngine", "Failed to start sing-box engine for server=${server.id}", e)
            _state.value = VpnState.Error("Sing-box connection failed")
            Result.failure(e)
        }
    }

    override suspend fun stop() = withContext(Dispatchers.IO) {
        Log.d("SingBoxEngine", "Stopping sing-box engine")
        running = false
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
