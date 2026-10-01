package com.corvus.vpn.vpn.engines

import android.content.Context
import android.content.Intent
import android.util.Log
import com.corvus.vpn.R
import com.corvus.vpn.data.ServerEntity
import com.corvus.vpn.data.ServerRepository
import com.corvus.vpn.data.SettingsRepository
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
    private val serverRepository: ServerRepository,
    private val settingsRepository: SettingsRepository
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

            val rawConfig = serverRepository.fetchFullConfig(server.id)
            if (rawConfig.isNullOrBlank()) {
                Log.w("OpenVpnEngine", "Config fetch blank for server=${server.id}, letting 20s watchdog run")
                return@withContext Result.failure(IllegalStateException("No valid OpenVPN configuration available for server=${server.id}"))
            }

            val config = sanitizeOpenVpnConfig(rawConfig)

            val cp = ConfigParser()
            cp.parseConfig(StringReader(config))
            val vp = cp.convertProfile()
            vp.mName = server.name

            // Power Feature: Allow LAN Devices
            vp.mAllowLocalLAN = settingsRepository.allowLanDevices

            // Split Tunneling Configuration
            val routingMode = settingsRepository.appRoutingMode
            val routingPackages = settingsRepository.appRoutingPackages
            if (routingMode == "include" && routingPackages.isNotEmpty()) {
                vp.mAllowedAppsVpn = java.util.HashSet(routingPackages)
                vp.mAllowedAppsVpnAreDisallowed = false
            } else if (routingMode == "exclude" && routingPackages.isNotEmpty()) {
                vp.mAllowedAppsVpn = java.util.HashSet(routingPackages)
                vp.mAllowedAppsVpnAreDisallowed = true
            } else {
                vp.mAllowedAppsVpn.clear()
            }

            // Connection Control: Kill Switch leak protection
            if (settingsRepository.killSwitchEnabled) {
                vp.mBlockUnusedAddressFamilies = true
            }

            // Prevent KeyChain null alias crash
            if (vp.mAlias == null) {
                vp.mAlias = "vpn"
            }

            // Ensure username and password default to "vpn" if missing
            if (vp.mUsername.isNullOrEmpty()) {
                vp.mUsername = "vpn"
            }
            if (vp.mPassword.isNullOrEmpty()) {
                vp.mPassword = "vpn"
            }

            if (vp.mAuthenticationType == VpnProfile.TYPE_KEYSTORE && (vp.mClientCertFilename.isNullOrEmpty() && vp.mPKCS12Filename.isNullOrEmpty() && vp.mAlias.isNullOrEmpty())) {
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
            Log.e("OpenVpnEngine", "OpenVPN start exception for server=${server.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun stop() = withContext(Dispatchers.IO) {
        try {
            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = null
            Log.d("OpenVpnEngine", "Stop requested")
            running = false
            val stopIntent = Intent(context, OpenVPNService::class.java).apply {
                action = OpenVPNService.DISCONNECT_VPN
            }
            try { context.startService(stopIntent) } catch (_: Exception) {}
            try { context.stopService(Intent(context, OpenVPNService::class.java)) } catch (_: Exception) {}
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
                        if (_state.value is VpnState.Disconnecting) {
                            connectionTimeoutJob?.cancel()
                            connectionTimeoutJob = null
                            running = false
                            _state.value = VpnState.Idle
                        }
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

    private fun sanitizeOpenVpnConfig(rawConfig: String): String {
        val lines = rawConfig.split("\n")
        val validLines = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) {
                continue
            }
            // Filter out non-directive lines such as relative paths (/v1/profiles/...) or URLs
            if (trimmed.startsWith("/") || trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                Log.w("OpenVpnEngine", "Sanitizing non-directive line from config: $trimmed")
                continue
            }
            validLines.add(line)
        }
        var result = validLines.joinToString("\n")
        if (!result.contains("client") && !result.contains("dev tun") && !result.contains("dev tap")) {
            result = "client\ndev tun\n$result"
        }
        if (!result.contains("auth-user-pass")) {
            result = "auth-user-pass\n$result"
        }
        if (!result.contains("data-ciphers") && !result.contains("cipher ")) {
            result = "data-ciphers AES-256-GCM:AES-128-GCM:AES-256-CBC:AES-128-CBC:BF-CBC\ndata-ciphers-fallback BF-CBC\n$result"
        }
        if (!result.contains("<ca>") && !result.contains("ca ")) {
            result = "$result\n$DEFAULT_FALLBACK_CA\n"
        }
        return result
    }
}

private const val DEFAULT_FALLBACK_CA = """<ca>
-----BEGIN CERTIFICATE-----
MIIFazCCA1OgAwIBAgIRAIIQz7DSQONZRGPgu2OCiwAwDQYJKoZIhvcNAQELBQAw
TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh
cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMTUwNjA0MTEwNDM4
WhcNMzUwNjA0MTEwNDM4WjBPMQswCQYDVQQGEwJVUzEpMCcGA1UEChMgSW50ZXJu
ZXQgU2VjdXJpdHkgUmVzZWFyY2ggR3JvdXAxFTATBgNVBAMTDElTUkcgUm9vdCBY
MTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAK3oJHP0FDfzm54rVygc
h77ct984kIxuPOZXoHj3dcKi/vVqbvYATyjb3miGbESTtrFj/RQSa78f0uoxmyF+
0TM8ukj13Xnfs7j/EvEhmkvBioZxaUpmZmyPfjxwv60pIgbz5MDmgK7iS4+3mX6U
A5/TR5d8mUgjU+g4rk8Kb4Mu0UlXjIB0ttov0DiNewNwIRt18jA8+o+u3dpjq+sW
T8KOEUt+zwvo/7V3LvSye0rgTBIlDHCNAymg4VMk7BPZ7hm/ELNKjD+Jo2FR3qyH
B5T0Y3HsLuJvW5iB4YlcNHlsdu87kGJ55tukmi8mxdAQ4Q7e2RCOFvu396j3x+UC
B5iPNgiV5+I3lg02dZ77DnKxHZu8A/lJBdiB3QW0KtZB6awBdpUKD9jf1b0SHzUv
KBds0pjBqAlkd25HN7rOrFleaJ1/ctaJxQZBKT5ZPt0m9STJEadao0xAH0ahmbWn
OlFuhjuefXKnEgV4We0+UXgVCwOPjdAvBbI+e0ocS3MFEvzG6uBQE3xDk3SzynTn
jh8BCNAw1FtxNrQHusEwMFxIt4I7mKZ9YIqioymCzLq9gwQbooMDQaHWBfEbwrbw
qHyGO0aoSCqI3Haadr8faqU9GY/rOPNk3sgrDQoo//fb4hVC1CLQJ13hef4Y53CI
rU7m2Ys6xt0nUW7/vGT1M0NPAgMBAAGjQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNV
HRMBAf8EBTADAQH/MB0GA1UdDgQWBBR5tFnme7bl5AFzgAiIyBpY9umbbjANBgkq
hkiG9w0BAQsFAAOCAgEAVR9YqbyyqFDQDLHYGmkgJykIrGF1XIpu+ILlaS/V9lZL
ubhzEFnTIZd+50xx+7LSYK05qAvqFyFWhfFQDlnrzuBZ6brJFe+GnY+EgPbk6ZGQ
3BebYhtF8GaV0nxvwuo77x/Py9auJ/GpsMiu/X1+mvoiBOv/2X/qkSsisRcOj/KK
NFtY2PwByVS5uCbMiogziUwthDyC3+6WVwW6LLv3xLfHTjuCvjHIInNzktHCgKQ5
ORAzI4JMPJ+GslWYHb4phowim57iaztXOoJwTdwJx4nLCgdNbOhdjsnvzqvHu7Ur
TkXWStAmzOVyyghqpZXjFaH3pO3JLF+l+/+sKAIuvtd7u+Nxe5AW0wdeRlN8NwdC
jNPElpzVmbUq4JUagEiuTDkHzsxHpFKVK7q4+63SM1N95R1NbdWhscdCb+ZAJzVc
oyi3B43njTOQ5yOf+1CceWxG1bQVs5ZufpsMljq4Ui0/1lvh+wjChP4kqKOJ2qxq
4RgqsahDYVvTH9w7jXbyLeiNdd8XM2w9U/t7y0Ff/9yi0GE44Za4rF2LN9d11TPA
mRGunUHBcnWEvgJBQl9nJEiU0Zsnvgc/ubhPgXRR4Xq37Z0j4r7g1SgEEzwxA57d
emyPxgcYxn/eR44/KJ4EBs+lVDR3veyJm+kXQ99b21/+jh5Xos1AnX5iItreGCc=
-----END CERTIFICATE-----
</ca>"""
