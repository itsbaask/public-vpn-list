package com.corvus.vpn.vpn.engines

import android.content.Context
import android.content.Intent
import android.util.Log
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
    @ApplicationContext private val context: Context
) : VpnEngine, VpnStatus.StateListener {

    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _legacyEngineState = MutableStateFlow(ConnectionStatus.LEVEL_NOTCONNECTED)
    override val legacyEngineState: StateFlow<ConnectionStatus> = _legacyEngineState.asStateFlow()

    init {
        VpnStatus.addStateListener(this)
        val active = VpnStatus.isVPNActive()
        _legacyEngineState.value = if (active) ConnectionStatus.LEVEL_CONNECTED else ConnectionStatus.LEVEL_NOTCONNECTED
        
        de.blinkt.openvpn.core.Preferences.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean("showlogwindow", false)
            .putBoolean("disableconfirmation", true)
            .apply()
    }

    override fun start(config: String, serverName: String) {
        Log.d("OpenVpnEngine", "Start requested for $serverName")
        if (config.isEmpty()) return

        try {
            val cp = ConfigParser()
            cp.parseConfig(StringReader(config))
            val vp = cp.convertProfile()
            vp.mName = serverName

            if ("auth-user-pass" in config.lowercase()) {
                if (vp.mUsername.isNullOrEmpty()) vp.mUsername = "vpn"
                if (vp.mPassword.isNullOrEmpty()) vp.mPassword = "vpn"
                vp.mAuthenticationType = VpnProfile.TYPE_USERPASS
            }

            vp.mAuthRetry = VpnProfile.AUTH_RETRY_NOINTERACT
            vp.mUseLegacyProvider = true // Enable OpenSSL legacy provider for VPNGate cipher compatibility

            val pm = ProfileManager.getInstance(context)
            pm.addProfile(vp)
            ProfileManager.saveProfile(context, vp)
            ProfileManager.setConnectedVpnProfile(context, vp)

            // Direct instant service launch bypassing background activity restrictions!
            VPNLaunchHelper.startOpenVpn(vp, context, "AppConnection", true)
            
        } catch (e: Exception) {
            Log.e("OpenVpnEngine", "Parsing failed", e)
        }
    }

    override fun stop() {
        Log.d("OpenVpnEngine", "Stop requested")
        val stopIntent = Intent(context, OpenVPNService::class.java)
        stopIntent.action = OpenVPNService.DISCONNECT_VPN
        context.startService(stopIntent)
        ProfileManager.setConntectedVpnProfileDisconnected(context)
    }

    override fun updateState(
        state: String,
        logmessage: String,
        localizedResId: Int,
        level: ConnectionStatus,
        intent: Intent?
    ) {
        Log.d("OpenVpnEngine", "State update: $state ($level)")
        _legacyEngineState.value = level
    }

    override fun setConnectedVPN(uuid: String?) {}
}
