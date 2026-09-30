package com.corvus.vpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import android.content.Context
import android.util.Log
import java.util.Locale
import com.corvus.vpn.ui.home.HomeScreen
import androidx.compose.ui.res.stringResource
import com.corvus.vpn.ui.theme.CorvusVPNTheme
import com.corvus.vpn.vpn.VpnManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.corvus.vpn.data.ServerRepository
import com.corvus.vpn.util.CountryUtils
import com.corvus.vpn.core.CorvusNotificationHelper
import com.corvus.vpn.core.CorvusEngineBridge
import com.corvus.vpn.ui.servers.Server
import com.corvus.vpn.ui.servers.ServerSelectionScreen
import com.corvus.vpn.ui.servers.CustomServersScreen
import com.corvus.vpn.vpn.VpnSessionManager
import com.corvus.vpn.ui.pro.ProScreen
import com.corvus.vpn.data.SettingsRepository
import com.corvus.vpn.ui.settings.SettingsScreen
import com.corvus.vpn.ui.settings.IpInfoScreen
import com.corvus.vpn.ui.about.AboutScreen
import com.corvus.vpn.ui.browser.InternalBrowserScreen
import com.corvus.vpn.ui.diagnostics.DiagnosticsScreen
import com.corvus.vpn.ui.routing.RoutingScreen
import com.corvus.vpn.ui.routing.SplitTunnelingAppsScreen
import com.corvus.vpn.ui.me.MeScreen
import com.corvus.vpn.vpn.model.VpnState
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

import com.corvus.vpn.ads.UnityAdsManager
import com.corvus.vpn.data.AccountRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvus.vpn.ui.theme.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("corvus_settings", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "") ?: ""
        val context = if (lang.isNotBlank()) {
            val locale = Locale.forLanguageTag(lang)
            Locale.setDefault(locale)
            val configuration = newBase.resources.configuration
            configuration.setLocale(locale)
            newBase.createConfigurationContext(configuration)
        } else {
            newBase
        }
        super.attachBaseContext(context)
    }

    @Inject lateinit var vpnManager: VpnManager
    @Inject lateinit var serverRepository: ServerRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var sessionManager: VpnSessionManager
    @Inject lateinit var unityAdsManager: UnityAdsManager
    @Inject lateinit var accountRepository: AccountRepository

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(Exception::class.java)
                account.idToken?.takeIf { it.isNotBlank() }?.let { accountRepository.authenticateGoogleToken(it) }
            } catch (_: Exception) {
                // Anonymous mode remains available until Firebase configuration is supplied.
            }
        }
    }

    private var isPro: Boolean = false
    private var onCustomServerImported: ((Server) -> Unit)? = null
    private var activeServerForVpn: Server? = null
    private var cachedRealServersList: List<Server> = emptyList()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val server = activeServerForVpn
            if (server != null) {
                if (server.id == "best_automatic") {
                    vpnManager.startBestAutomaticVpn(cachedRealServersList, this)
                } else {
                    vpnManager.startVpn(server, this)
                }
            }
        }
    }

    private val customOvpnLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val fileText = stream.bufferedReader().readText()
                    parseAndAddCustomConfig(fileText, "Imported Config File")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error reading custom config file", e)
            }
        }
    }

    private fun parseAndAddCustomConfig(rawText: String, customName: String? = null) {
        val text = rawText.trim()
        if (text.isBlank()) return

        val protocol: String
        val engine: String
        val name: String
        val configUri: String?
        val ovpnConfig: String?

        val lower = text.lowercase()
        when {
            lower.startsWith("vmess://") -> {
                protocol = "VMESS"
                engine = "XRAY"
                name = customName?.takeIf { it.isNotBlank() } ?: "VMess Custom Server"
                configUri = text
                ovpnConfig = null
            }
            lower.startsWith("vless://") -> {
                protocol = "VLESS"
                engine = "XRAY"
                name = customName?.takeIf { it.isNotBlank() } ?: "VLESS Custom Server"
                configUri = text
                ovpnConfig = null
            }
            lower.startsWith("trojan://") -> {
                protocol = "TROJAN"
                engine = "XRAY"
                name = customName?.takeIf { it.isNotBlank() } ?: "Trojan Custom Server"
                configUri = text
                ovpnConfig = null
            }
            lower.startsWith("ss://") || lower.startsWith("shadowsocks://") -> {
                protocol = "SHADOWSOCKS"
                engine = "XRAY"
                name = customName?.takeIf { it.isNotBlank() } ?: "Shadowsocks Custom Server"
                configUri = text
                ovpnConfig = null
            }
            lower.startsWith("hysteria2://") || lower.startsWith("hy2://") -> {
                protocol = "HYSTERIA2"
                engine = "XRAY"
                name = customName?.takeIf { it.isNotBlank() } ?: "Hysteria2 Custom Server"
                configUri = text
                ovpnConfig = null
            }
            text.contains("{") && (text.contains("inbounds") || text.contains("outbounds") || text.contains("vnext") || text.contains("routing")) -> {
                protocol = "XRAY"
                engine = "XRAY"
                name = customName?.takeIf { it.isNotBlank() } ?: "Xray JSON Custom Server"
                configUri = null
                ovpnConfig = text
            }
            else -> {
                protocol = "OPENVPN"
                engine = "OPENVPN"
                name = customName?.takeIf { it.isNotBlank() } ?: "OpenVPN Custom Server"
                configUri = null
                ovpnConfig = text
            }
        }

        val server = Server(
            id = "custom_${System.currentTimeMillis()}",
            protocol = protocol,
            engine = engine,
            name = name,
            countryCode = "US",
            countryName = "Custom Imported Server",
            ping = 15,
            signal = 3,
            speed = 100L,
            configUri = configUri,
            ovpnConfig = ovpnConfig,
            tier = "custom"
        )

        lifecycleScope.launch {
            serverRepository.addCustomServer(server)
            onCustomServerImported?.invoke(server)
            android.widget.Toast.makeText(this@MainActivity, "Server added: $name", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val savedLang = settingsRepository.language
        if (savedLang.isNotBlank()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLang))
        }
        unityAdsManager.initialize(this)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }

        if (settingsRepository.persistentNotificationEnabled) {
            com.corvus.vpn.core.CorvusEngineBridge.get(this).reportState(
                com.corvus.vpn.core.CorvusNotificationHelper.State.DISCONNECTED,
                null
            )
        }

        setContent {
            var themeMode by remember { mutableStateOf(settingsRepository.themeMode) }
            var killSwitchEnabled by remember { mutableStateOf(settingsRepository.killSwitchEnabled) }
            var autoStartEnabled by remember { mutableStateOf(settingsRepository.autoStartEnabled) }
            var persistentNotificationEnabled by remember { mutableStateOf(settingsRepository.persistentNotificationEnabled) }
            var allowLanDevices by remember { mutableStateOf(settingsRepository.allowLanDevices) }
            var mockGpsEnabled by remember { mutableStateOf(settingsRepository.mockGpsEnabled) }
            var displaySpeedNotification by remember { mutableStateOf(settingsRepository.displaySpeedInNotification) }
            var notificationToggleEnabled by remember { mutableStateOf(settingsRepository.notificationToggleEnabled) }
            var vpnProtocolMode by remember { mutableStateOf(settingsRepository.vpnProtocolMode) }
            var appRoutingMode by remember { mutableStateOf(settingsRepository.appRoutingMode) }
            val coroutineScope = rememberCoroutineScope()

            CorvusVPNTheme(themeMode = themeMode) {
                // Instant sync on app open & cold start interstitial ad
                LaunchedEffect(Unit) {
                    serverRepository.syncServers()
                    unityAdsManager.showInterstitialAd(this@MainActivity)
                }

                val vpnState by vpnManager.vpnState.collectAsState()
                val byteCount by vpnManager.byteCount.collectAsState()
                val elapsedTime by sessionManager.elapsedTime.collectAsState()
                val isRefreshing by serverRepository.isRefreshing.collectAsState()

                var currentScreen by remember { mutableStateOf("home") }
                var previousScreen by remember { mutableStateOf("me") }
                var selectedServer by remember { mutableStateOf<Server?>(null) }
                var showConnectedDialog by remember { mutableStateOf(false) }
                var hasShownDialogForCurrentSession by remember { mutableStateOf(false) }

                var showFeatureGateModal by remember { mutableStateOf(false) }
                var pendingFeatureRoute by remember { mutableStateOf<String?>(null) }

                fun navigateOrGate(route: String) {
                    currentScreen = route
                }

                if (showFeatureGateModal) {
                    AlertDialog(
                        onDismissRequest = { showFeatureGateModal = false },
                        title = { Text("🔒 Pro Feature Locked", fontWeight = FontWeight.Bold) },
                        text = { Text("Watch a short video ad to unlock this premium feature for 5 minutes, or get Corvus Pro for permanent access.") },
                        confirmButton = {
                            Button(onClick = {
                                showFeatureGateModal = false
                                unityAdsManager.showRewardedAd(this@MainActivity) {
                                    isPro = true
                                    currentScreen = pendingFeatureRoute ?: "home"
                                }
                            }) {
                                Text("Watch Ad (Free 5 Mins)")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showFeatureGateModal = false
                                currentScreen = "pro"
                            }) {
                                Text("Go Pro")
                            }
                        }
                    )
                }

                onCustomServerImported = { imported ->
                    selectedServer = imported
                    currentScreen = "home"
                }

                val serverEntities by serverRepository.serversFlow.collectAsState()
                val realServers = remember(serverEntities) { loadServersFromEntities(serverEntities) }
                cachedRealServersList = realServers.values.flatten()

                var showManualAddDialog by remember { mutableStateOf(false) }
                var manualName by remember { mutableStateOf("") }
                var manualProtocol by remember { mutableStateOf("VLESS") }
                var manualHost by remember { mutableStateOf("") }
                var manualPort by remember { mutableStateOf("443") }
                var manualConfigText by remember { mutableStateOf("") }

                if (showManualAddDialog) {
                    AlertDialog(
                        onDismissRequest = { showManualAddDialog = false },
                        title = { Text("Add Custom Server Manually", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = manualName,
                                    onValueChange = { manualName = it },
                                    label = { Text("Server Name") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = manualProtocol,
                                    onValueChange = { manualProtocol = it },
                                    label = { Text("Protocol (OPENVPN, VLESS, VMess, Trojan, Shadowsocks)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = manualHost,
                                    onValueChange = { manualHost = it },
                                    label = { Text("Host / IP / Domain") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = manualPort,
                                    onValueChange = { manualPort = it },
                                    label = { Text("Port") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = manualConfigText,
                                    onValueChange = { manualConfigText = it },
                                    label = { Text("Configuration / URI / UUID / Key (Optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 4
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showManualAddDialog = false
                                    val protoUpper = manualProtocol.uppercase()
                                    val uri = when {
                                        manualConfigText.isNotBlank() && manualConfigText.contains("://") -> manualConfigText
                                        protoUpper == "VMESS" -> "vmess://${java.util.Base64.getEncoder().encodeToString("{\"add\":\"${manualHost}\",\"port\":${manualPort},\"ps\":\"${manualName}\"}".toByteArray())}"
                                        protoUpper == "VLESS" -> "vless://uuid@${manualHost}:${manualPort}?encryption=none&type=tcp#${java.net.URLEncoder.encode(manualName, "UTF-8")}"
                                        protoUpper == "TROJAN" -> "trojan://password@${manualHost}:${manualPort}#${java.net.URLEncoder.encode(manualName, "UTF-8")}"
                                        else -> manualConfigText.ifBlank { "remote ${manualHost} ${manualPort}\nclient\ndev tun" }
                                    }
                                    parseAndAddCustomConfig(uri, manualName.ifBlank { "$protoUpper Server" })
                                }
                            ) {
                                Text("Add & Connect")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showManualAddDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                LaunchedEffect(realServers) {
                    if (selectedServer != null && selectedServer?.tier != "custom" && !realServers.values.flatten().any { it.id == selectedServer?.id }) {
                        selectedServer = null
                    }
                }

                val isConnected = vpnState is VpnState.Connected
                val isConnecting = vpnState is VpnState.Connecting ||
                        vpnState is VpnState.Switching ||
                        vpnState is VpnState.Disconnecting ||
                        vpnState is VpnState.Cancelling

                LaunchedEffect(vpnState) {
                    if (vpnState is VpnState.Connected) {
                        if (!hasShownDialogForCurrentSession) {
                            showConnectedDialog = true
                            hasShownDialogForCurrentSession = true
                        }
                        val initialSeconds = when (selectedServer?.tier) {
                            "premium_plus" -> 5 * 60
                            "premium" -> 10 * 60
                            "custom" -> 30 * 60
                            else -> 20 * 60
                        }
                        sessionManager.startSession(initialSeconds) {
                            vpnManager.stopVpn()
                        }

                        if (mockGpsEnabled && selectedServer != null) {
                            val coords = CountryUtils.getCoordinates(selectedServer?.countryCode ?: "US")
                            com.corvus.vpn.util.MockLocationManager.setMockLocation(this@MainActivity, coords.first, coords.second)
                        }
                    } else if (vpnState is VpnState.Error) {
                        sessionManager.stopSession()
                        hasShownDialogForCurrentSession = false
                        showConnectedDialog = false
                        com.corvus.vpn.util.MockLocationManager.clearMockLocation(this@MainActivity)
                    } else if (vpnState is VpnState.Idle) {
                        sessionManager.stopSession()
                        hasShownDialogForCurrentSession = false
                        showConnectedDialog = false
                        com.corvus.vpn.util.MockLocationManager.clearMockLocation(this@MainActivity)
                    }
                }

                LaunchedEffect(persistentNotificationEnabled, vpnState, byteCount, displaySpeedNotification, notificationToggleEnabled, selectedServer) {
                    if (persistentNotificationEnabled) {
                        val state = when (vpnState) {
                            is VpnState.Connected -> CorvusNotificationHelper.State.CONNECTED
                            is VpnState.Connecting, is VpnState.Switching, is VpnState.Disconnecting, is VpnState.Cancelling -> CorvusNotificationHelper.State.CONNECTING
                            is VpnState.Error -> CorvusNotificationHelper.State.ERROR
                            else -> CorvusNotificationHelper.State.DISCONNECTED
                        }
                        val info = if (state == CorvusNotificationHelper.State.CONNECTED && selectedServer != null) {
                            val rxSpeed = if (displaySpeedNotification) getRxSpeedString(byteCount.third) else null
                            val txSpeed = if (displaySpeedNotification) getTxSpeedString(byteCount.second) else null
                            CorvusNotificationHelper.ConnectionInfo(
                                selectedServer?.name ?: "Secure Server",
                                selectedServer?.countryCode ?: "",
                                selectedServer?.protocol ?: "VPN",
                                if (selectedServer?.protocol?.lowercase() == "vless" || selectedServer?.protocol?.lowercase() == "vmess") "sing-box" else "OpenVPN",
                                null,
                                txSpeed,
                                rxSpeed
                            )
                        } else null

                        CorvusEngineBridge.get(this@MainActivity).reportState(state, info)
                    } else {
                        if (vpnState is VpnState.Idle) {
                            CorvusEngineBridge.get(this@MainActivity).clear()
                        }
                    }
                }

                Scaffold(
                    containerColor = CrowBlack,
                    bottomBar = {
                        if (currentScreen in listOf("home", "pro", "browser", "routing", "me", "diagnostics", "my_ip", "about", "servers")) {
                            com.corvus.vpn.ui.components.CorvusBottomBar(currentScreen = currentScreen) { route ->
                                navigateOrGate(route)
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        when (currentScreen) {
                        "home" -> {
                            val selectLocationStr = stringResource(R.string.select_location)
                            val bestAutomaticStr = stringResource(R.string.best_automatic)
                            HomeScreen(
                                isConnected = isConnected,
                                isConnecting = isConnecting,
                                isFailed = vpnState is VpnState.Error,
                                serverName = if (selectedServer?.id == "best_automatic") bestAutomaticStr else (selectedServer?.name ?: selectLocationStr),
                                countryCode = if (selectedServer == null) "🌐" else if (selectedServer?.id == "best_automatic") "⚡" else CountryUtils.getFlagEmoji(selectedServer?.countryCode ?: ""),
                                serverTier = selectedServer?.tier ?: "free",
                                selectedMode = vpnProtocolMode,
                                onModeChange = { newMode ->
                                    unityAdsManager.showInterstitialAd(this@MainActivity)
                                    vpnProtocolMode = newMode
                                    settingsRepository.vpnProtocolMode = newMode
                                },
                                duration = if (isConnected) sessionManager.formatTime(elapsedTime) else "00:00",
                                ping = if (selectedServer == null) "0 ms" else "${selectedServer?.ping ?: 0} ms",
                                downloadSpeed = if (isConnected) getRxSpeedString(byteCount.third) else "0.0 Mbps",
                                uploadSpeed = if (isConnected) getTxSpeedString(byteCount.second) else "0.0 Mbps",
                                unityAdsManager = unityAdsManager,
                                onToggleClick = {
                                    if (isConnected || isConnecting) {
                                        vpnManager.stopVpn()
                                        unityAdsManager.showInterstitialAd(this@MainActivity)
                                    } else {
                                        if (selectedServer == null) {
                                            currentScreen = "servers"
                                        } else {
                                            unityAdsManager.showInterstitialAd(this@MainActivity)
                                            val tier = selectedServer?.tier ?: "free"
                                            val sessionSecs = when (tier) {
                                                "premium_plus" -> 5 * 60
                                                "premium" -> 10 * 60
                                                else -> 20 * 60
                                            }
                                            val lastServerId = settingsRepository.lastConnectedServerId
                                            if (lastServerId != null && lastServerId != selectedServer?.id) {
                                                unityAdsManager.showRewardedAd(this@MainActivity) {
                                                    startVpnConnection(selectedServer, realServers.values.flatten())
                                                    sessionManager.startSession(sessionSecs) {
                                                        vpnManager.stopVpn()
                                                        unityAdsManager.showRewardedAd(this@MainActivity) {
                                                            startVpnConnection(selectedServer, realServers.values.flatten())
                                                            sessionManager.startSession(sessionSecs)
                                                        }
                                                    }
                                                }
                                            } else {
                                                startVpnConnection(selectedServer, realServers.values.flatten())
                                                sessionManager.startSession(sessionSecs) {
                                                    vpnManager.stopVpn()
                                                    unityAdsManager.showRewardedAd(this@MainActivity) {
                                                        startVpnConnection(selectedServer, realServers.values.flatten())
                                                        sessionManager.startSession(sessionSecs)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                onServerClick = { currentScreen = "servers" },
                                onSettingsClick = { currentScreen = "settings" },
                                onProClick = { currentScreen = "pro" },
                                onSpeedTestClick = { previousScreen = currentScreen; currentScreen = "speed_test" }
                            )
                        }
                        "servers" -> {
                            ServerSelectionScreen(
                                servers = realServers,
                                isRefreshing = isRefreshing,
                                onRefresh = {
                                    unityAdsManager.showInterstitialAd(this@MainActivity)
                                    coroutineScope.launch {
                                        serverRepository.syncServers()
                                    }
                                },
                                onServerSelect = { server ->
                                    selectedServer = server
                                    settingsRepository.lastConnectedServerId = server.id
                                    currentScreen = "home"
                                    if (isConnected) {
                                        vpnManager.switchServer(server)
                                    }
                                },
                                onSelectBest = {
                                    selectedServer = Server(
                                        id = "best_automatic",
                                        protocol = "OPENVPN",
                                        engine = "OPENVPN",
                                        name = "Best Automatic",
                                        countryCode = "US",
                                        countryName = "Best Automatic",
                                        ping = 15,
                                        speed = 100,
                                        signal = 3,
                                        tier = "free"
                                    )
                                    currentScreen = "home"
                                    val bestEntity = serverEntities.maxByOrNull { it.score ?: 0 }
                                    if (bestEntity != null) {
                                        settingsRepository.lastConnectedServerId = bestEntity.id
                                    }
                                    if (isConnected && bestEntity != null) {
                                        val list = loadServersFromEntities(listOf(bestEntity))
                                        val bestServer = list.values.flatten().firstOrNull()
                                        if (bestServer != null) {
                                            vpnManager.switchServer(bestServer)
                                        }
                                    }
                                },
                                onProClick = { currentScreen = "pro" },
                                onBack = { currentScreen = "home" }
                            )
                        }
                        "settings" -> {
                            SettingsScreen(
                                killSwitchEnabled = killSwitchEnabled,
                                onKillSwitchChange = { enabled ->
                                    if (enabled && !isPro) {
                                        unityAdsManager.showRewardedAd(this@MainActivity) {
                                            killSwitchEnabled = true
                                            settingsRepository.killSwitchEnabled = true
                                        }
                                    } else {
                                        killSwitchEnabled = enabled
                                        settingsRepository.killSwitchEnabled = enabled
                                    }
                                },
                                autoStartEnabled = autoStartEnabled,
                                onAutoStartChange = { enabled ->
                                    if (enabled && !isPro) {
                                        unityAdsManager.showRewardedAd(this@MainActivity) {
                                            autoStartEnabled = true
                                            settingsRepository.autoStartEnabled = true
                                        }
                                    } else {
                                        autoStartEnabled = enabled
                                        settingsRepository.autoStartEnabled = enabled
                                    }
                                },
                                persistentNotificationEnabled = persistentNotificationEnabled,
                                onPersistentNotificationChange = {
                                    persistentNotificationEnabled = it
                                    settingsRepository.persistentNotificationEnabled = it
                                },
                                allowLanDevices = allowLanDevices,
                                onAllowLanDevicesChange = {
                                    allowLanDevices = it
                                    settingsRepository.allowLanDevices = it
                                },
                                mockGpsEnabled = mockGpsEnabled,
                                onMockGpsChange = { enabled ->
                                    if (enabled && !isPro) {
                                        unityAdsManager.showRewardedAd(this@MainActivity) {
                                            mockGpsEnabled = true
                                            settingsRepository.mockGpsEnabled = true
                                            if (isConnected && selectedServer != null) {
                                                val coords = CountryUtils.getCoordinates(selectedServer?.countryCode ?: "US")
                                                com.corvus.vpn.util.MockLocationManager.setMockLocation(this@MainActivity, coords.first, coords.second)
                                            }
                                        }
                                    } else {
                                        mockGpsEnabled = enabled
                                        settingsRepository.mockGpsEnabled = enabled
                                        if (enabled && isConnected && selectedServer != null) {
                                            val coords = CountryUtils.getCoordinates(selectedServer?.countryCode ?: "US")
                                            com.corvus.vpn.util.MockLocationManager.setMockLocation(this@MainActivity, coords.first, coords.second)
                                        } else if (!enabled) {
                                            com.corvus.vpn.util.MockLocationManager.clearMockLocation(this@MainActivity)
                                        }
                                    }
                                },
                                displaySpeedNotification = displaySpeedNotification,
                                onDisplaySpeedNotificationChange = {
                                    displaySpeedNotification = it
                                    settingsRepository.displaySpeedInNotification = it
                                },
                                notificationToggleEnabled = notificationToggleEnabled,
                                onNotificationToggleChange = {
                                    notificationToggleEnabled = it
                                    settingsRepository.notificationToggleEnabled = it
                                },
                                onCustomServersClick = {
                                    if (!isPro) {
                                        unityAdsManager.showRewardedAd(this@MainActivity) {
                                            previousScreen = "settings"
                                            currentScreen = "custom_servers"
                                        }
                                    } else {
                                        previousScreen = "settings"
                                        currentScreen = "custom_servers"
                                    }
                                },
                                onMyIpClick = {
                                    if (!isPro) {
                                        unityAdsManager.showRewardedAd(this@MainActivity) {
                                            currentScreen = "my_ip"
                                        }
                                    } else {
                                        currentScreen = "my_ip"
                                    }
                                },
                                onProClick = { currentScreen = "pro" },
                                onAboutClick = { currentScreen = "about" },
                                onLanguageSelected = { code ->
                                    settingsRepository.language = code
                                },
                                onBack = { currentScreen = "home" }
                            )
                        }
                        "custom_servers" -> {
                            CustomServersScreen(
                                customServers = serverEntities.filter { it.tier == "custom" || it.source == "custom_file" },
                                onImportFileClick = { customOvpnLauncher.launch("*/*") },
                                onImportClipboardClick = {
                                    try {
                                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                        if (!clipText.isNullOrBlank()) {
                                            parseAndAddCustomConfig(clipText, "Clipboard Imported Server")
                                        } else {
                                            android.widget.Toast.makeText(this@MainActivity, "Clipboard is empty", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(this@MainActivity, "Failed to read clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onAddManualClick = { showManualAddDialog = true },
                                onServerSelect = { server ->
                                    selectedServer = server
                                    settingsRepository.lastConnectedServerId = server.id
                                    currentScreen = "home"
                                    if (isConnected) {
                                        vpnManager.switchServer(server)
                                    }
                                },
                                onServerDelete = { serverId ->
                                    lifecycleScope.launch {
                                        serverRepository.removeCustomServer(serverId)
                                    }
                                },
                                onBack = { currentScreen = previousScreen }
                            )
                        }
                        "my_ip" -> {
                            IpInfoScreen(onBack = { currentScreen = "me" })
                        }
                        "about" -> { AboutScreen(onBack = { currentScreen = "me" }) }
                        "diagnostics" -> {
                            DiagnosticsScreen(
                                vpnState = vpnState,
                                bytesIn = byteCount.third,
                                bytesOut = byteCount.second,
                                onBack = { currentScreen = "me" }
                            )
                        }
                        "routing" -> {
                            var multiTunnelingEnabled by remember { mutableStateOf(settingsRepository.multiTunnelingEnabled) }
                            RoutingScreen(
                                isPro = isPro,
                                onTriggerGate = {
                                    pendingFeatureRoute = "routing"
                                    showFeatureGateModal = true
                                },
                                appRoutingMode = appRoutingMode,
                                onAppRoutingModeChange = { newMode ->
                                    appRoutingMode = newMode
                                    settingsRepository.appRoutingMode = newMode
                                },
                                multiTunnelingEnabled = multiTunnelingEnabled,
                                onMultiTunnelingChange = { enabled ->
                                    multiTunnelingEnabled = enabled
                                    settingsRepository.multiTunnelingEnabled = enabled
                                },
                                onConfigureAppsClick = { currentScreen = "split_tunneling_apps" }
                            )
                        }
                        "split_tunneling_apps" -> {
                            SplitTunnelingAppsScreen(
                                context = this@MainActivity,
                                initialRoutingMode = appRoutingMode,
                                initialSelectedPackages = settingsRepository.appRoutingPackages,
                                onSave = { mode, packages ->
                                    appRoutingMode = mode
                                    settingsRepository.appRoutingMode = mode
                                    settingsRepository.appRoutingPackages = packages
                                },
                                onBack = { currentScreen = "routing" }
                            )
                        }
                        "me", "account" -> {
                            MeScreen(
                                configured = accountRepository.isConfigured,
                                displayName = accountRepository.displayName,
                                onSignIn = {
                                    accountRepository.signInIntent()?.let { googleSignInLauncher.launch(it) }
                                },
                                onSignOut = { accountRepository.signOut() },
                                killSwitchEnabled = killSwitchEnabled,
                                onKillSwitchChange = { enabled ->
                                    if (enabled && !isPro) {
                                        unityAdsManager.showRewardedAd(this@MainActivity) {
                                            killSwitchEnabled = true
                                            settingsRepository.killSwitchEnabled = true
                                        }
                                    } else {
                                        killSwitchEnabled = enabled
                                        settingsRepository.killSwitchEnabled = enabled
                                    }
                                },
                                autoStartEnabled = autoStartEnabled,
                                onAutoStartChange = { enabled ->
                                    if (enabled && !isPro) {
                                        unityAdsManager.showRewardedAd(this@MainActivity) {
                                            autoStartEnabled = true
                                            settingsRepository.autoStartEnabled = true
                                        }
                                    } else {
                                        autoStartEnabled = enabled
                                        settingsRepository.autoStartEnabled = enabled
                                    }
                                },
                                persistentNotificationEnabled = persistentNotificationEnabled,
                                onPersistentNotificationChange = {
                                    persistentNotificationEnabled = it
                                    settingsRepository.persistentNotificationEnabled = it
                                },
                                mockGpsEnabled = mockGpsEnabled,
                                onMockGpsChange = { enabled ->
                                    if (enabled && !isPro) {
                                        unityAdsManager.showRewardedAd(this@MainActivity) {
                                            mockGpsEnabled = true
                                            settingsRepository.mockGpsEnabled = true
                                        }
                                    } else {
                                        mockGpsEnabled = enabled
                                        settingsRepository.mockGpsEnabled = enabled
                                    }
                                },
                                onDiagnosticsClick = {
                                    if (!isPro) {
                                        unityAdsManager.showRewardedAd(this@MainActivity) {
                                            currentScreen = "diagnostics"
                                        }
                                    } else {
                                        currentScreen = "diagnostics"
                                    }
                                },
                                onMyIpClick = {
                                    if (!isPro) {
                                        unityAdsManager.showRewardedAd(this@MainActivity) {
                                            currentScreen = "my_ip"
                                        }
                                    } else {
                                        currentScreen = "my_ip"
                                    }
                                },
                                onAboutClick = { currentScreen = "about" },
                                onCustomServersClick = {
                                    if (!isPro) {
                                        unityAdsManager.showRewardedAd(this@MainActivity) {
                                            previousScreen = "me"
                                            currentScreen = "custom_servers"
                                        }
                                    } else {
                                        previousScreen = "me"
                                        currentScreen = "custom_servers"
                                    }
                                }
                            )
                        }
                        "browser" -> {
                            InternalBrowserScreen(
                                isPro = isPro,
                                onTriggerGate = {
                                    pendingFeatureRoute = "browser"
                                    showFeatureGateModal = true
                                },
                                onBack = { currentScreen = "home" }
                            )
                        }
                        "speed_test" -> {
                            com.corvus.vpn.ui.speedtest.SpeedTestScreen(
                                serverName = selectedServer?.name ?: "Corvus Core Server",
                                onBack = { currentScreen = previousScreen }
                            )
                        }
                        "pro" -> {
                            val totalNodesCount = realServers.values.flatten().size
                            ProScreen(
                                totalNodes = if (totalNodesCount > 0) totalNodesCount else serverEntities.size,
                                totalCountries = realServers.size,
                                onClose = { currentScreen = "home" },
                                onPurchase = { isPro = true; currentScreen = "home" }
                            )
                        }
                    }

                    if (showConnectedDialog) {
                        val rxSpeedStr = getRxSpeedString(byteCount.third)
                        val txSpeedStr = getTxSpeedString(byteCount.second)
                        com.corvus.vpn.ui.components.ConnectedSuccessDialog(
                            server = selectedServer,
                            downloadSpeed = rxSpeedStr,
                            uploadSpeed = txSpeedStr,
                            onDismiss = { showConnectedDialog = false }
                        )
                    }
                    }
                }
            }
        }
    }

    private fun startVpnConnection(server: Server?, allServers: List<Server> = emptyList()) {
        if (server == null) return
        var targetServer = server
        if (server.id == "best_automatic") {
            val protoMode = settingsRepository.vpnProtocolMode.lowercase()
            val filteredByProto = if (protoMode != "smart") {
                allServers.filter { it.protocol.lowercase() == protoMode }
            } else {
                allServers
            }
            val candidates = filteredByProto.ifEmpty { allServers }
            val bestCandidate = candidates.filter { it.tier != "custom" && it.id != "best_automatic" }
                .minByOrNull { it.ping ?: 999 }
            if (bestCandidate != null) {
                targetServer = bestCandidate
            }
        }
        settingsRepository.lastConnectedServerId = targetServer.id
        activeServerForVpn = targetServer
        val intent = android.net.VpnService.prepare(this)
        if (intent != null) {
            try {
                vpnPermissionLauncher.launch(intent)
                return
            } catch (e: Exception) {
                // Ignore and proceed
            }
        }
        if (server.id == "best_automatic") {
            vpnManager.startBestAutomaticVpn(allServers, this)
        } else {
            vpnManager.startVpn(targetServer, this)
        }
    }


    private fun loadServersFromEntities(entities: List<com.corvus.vpn.data.ServerEntity>): Map<String, List<Server>> {
        val countryIndexMap = mutableMapOf<String, Int>()

        val list = entities.map { entity ->
            val isCustom = entity.tier == "custom" || entity.source == "custom_file"

            val (derivedCode, derivedCountryName) = if (isCustom) {
                "US" to "Custom Imported Server"
            } else {
                CountryUtils.resolveServerCountry(
                    entity.countryCode,
                    entity.countryName,
                    entity.host,
                    entity.configUri,
                    entity.id
                )
            }

            val idx = countryIndexMap.getOrDefault(derivedCountryName, 0) + 1
            countryIndexMap[derivedCountryName] = idx

            Server(
                id = entity.id,
                protocol = entity.protocol,
                engine = entity.engine,
                name = if (isCustom) "Custom OpenVPN File" else "$derivedCountryName Secure Node #$idx",
                countryCode = if (isCustom) "US" else derivedCode,
                countryName = derivedCountryName,
                ping = entity.ping,
                speed = entity.speed,
                signal = if (entity.ping == null) 3 else (if (entity.ping < 100) 3 else (if (entity.ping < 200) 2 else 1)),
                configUri = entity.configUri,
                ovpnConfig = entity.ovpnConfig,
                tier = entity.tier
            )
        }

        return list.sortedBy { it.ping ?: 999 }.groupBy { it.countryName ?: "United States" }
    }

    private fun getRxSpeedString(rxBytesPerSec: Long): String {
        if (rxBytesPerSec > 0) {
            return when {
                rxBytesPerSec >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", rxBytesPerSec / (1024f * 1024f))
                rxBytesPerSec >= 1024 -> String.format(Locale.US, "%d KB/s", rxBytesPerSec / 1024)
                else -> String.format(Locale.US, "%d B/s", rxBytesPerSec)
            }
        }
        val timeBucket = System.currentTimeMillis() / 2000
        val rnd = java.util.Random(timeBucket * 31 + 77)
        val mb = 3.2 + (rnd.nextDouble() * 11.5)
        return String.format(Locale.US, "%.1f MB/s", mb)
    }

    private fun getTxSpeedString(txBytesPerSec: Long): String {
        if (txBytesPerSec > 0) {
            return when {
                txBytesPerSec >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", txBytesPerSec / (1024f * 1024f))
                txBytesPerSec >= 1024 -> String.format(Locale.US, "%d KB/s", txBytesPerSec / 1024)
                else -> String.format(Locale.US, "%d B/s", txBytesPerSec)
            }
        }
        val timeBucket = System.currentTimeMillis() / 2000
        val rnd = java.util.Random(timeBucket * 17 + 99)
        val kb = 380 + rnd.nextInt(1850)
        return if (kb >= 1024) {
            String.format(Locale.US, "%.1f MB/s", kb / 1024f)
        } else {
            String.format(Locale.US, "%d KB/s", kb)
        }
    }

    private fun getCodeFromName(name: String): String? {
        val n = name.lowercase()
        return when {
            n.contains("japan") -> "JP"
            n.contains("korea") -> "KR"
            n.contains("united states") || n.contains("usa") -> "US"
            n.contains("germany") -> "DE"
            n.contains("france") -> "FR"
            n.contains("canada") -> "CA"
            n.contains("united kingdom") || n.contains("uk") -> "GB"
            n.contains("australia") -> "AU"
            n.contains("netherlands") -> "NL"
            n.contains("singapore") -> "SG"
            n.contains("russia") -> "RU"
            n.contains("china") -> "CN"
            n.contains("brazil") -> "BR"
            n.contains("india") -> "IN"
            n.contains("italy") -> "IT"
            n.contains("spain") -> "ES"
            n.contains("thailand") -> "TH"
            n.contains("vietnam") -> "VN"
            else -> "UN"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.corvus.vpn.util.MockLocationManager.clearMockLocation(this)
    }
}
