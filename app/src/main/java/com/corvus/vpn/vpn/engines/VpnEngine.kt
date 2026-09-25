package com.corvus.vpn.vpn.engines

import android.content.Context
import com.corvus.vpn.data.ServerEntity
import com.corvus.vpn.vpn.model.VpnState
import com.corvus.vpn.vpn.model.VpnStats
import kotlinx.coroutines.flow.Flow

/**
 * Common abstraction interface for all VPN engines (SingBoxEngine, OpenVpnEngine, Ikev2Engine).
 */
interface VpnEngine {
    suspend fun start(server: ServerEntity, activityContext: Context? = null): Result<Unit>
    suspend fun stop()
    suspend fun restart(server: ServerEntity, activityContext: Context? = null): Result<Unit>
    fun isRunning(): Boolean
    fun observeState(): Flow<VpnState>
    fun getStats(): VpnStats
}
