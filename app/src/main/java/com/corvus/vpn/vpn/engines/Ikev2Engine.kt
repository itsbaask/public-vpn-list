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
class Ikev2Engine @Inject constructor(
    @ApplicationContext private val context: Context
) : VpnEngine {

    private val _state = MutableStateFlow<VpnState>(VpnState.Idle)
    private var running = false
    private val stats = VpnStats()

    override suspend fun start(server: ServerEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("Ikev2Engine", "Starting IKEv2 engine for server=${server.name} (Sanitized)")
            _state.value = VpnState.Connecting(server)
            running = true
            delay(400)
            _state.value = VpnState.Connected(server, System.currentTimeMillis(), com.corvus.vpn.vpn.model.ConnectionStats())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Ikev2Engine", "Failed to start IKEv2 engine for server=${server.id}", e)
            _state.value = VpnState.Error("IKEv2 connection failed")
            Result.failure(e)
        }
    }

    override suspend fun stop() = withContext(Dispatchers.IO) {
        Log.d("Ikev2Engine", "Stopping IKEv2 engine")
        running = false
        _state.value = VpnState.Idle
    }

    override suspend fun restart(server: ServerEntity): Result<Unit> {
        stop()
        return start(server)
    }

    override fun isRunning(): Boolean = running

    override fun observeState(): Flow<VpnState> = _state.asStateFlow()

    override fun getStats(): VpnStats = stats
}
