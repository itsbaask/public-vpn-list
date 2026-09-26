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
import com.corvus.vpn.ui.servers.Server
import com.corvus.vpn.ui.servers.ServerSelectionScreen
import com.corvus.vpn.vpn.VpnSessionManager
import com.corvus.vpn.ui.pro.ProScreen
import com.corvus.vpn.data.SettingsRepository
import com.corvus.vpn.ui.settings.SettingsScreen
import com.corvus.vpn.ui.settings.IpInfoScreen
import com.corvus.vpn.ui.about.AboutScreen
import com.corvus.vpn.vpn.model.VpnState
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

import com.corvus.vpn.ads.UnityAdsManager
import androidx.activity.result.contract.ActivityResultContracts

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

    private var isPro: Boolean = false
    private var onCustomServerImported: ((Server) -> Unit)? = null
    private var activeServerForVpn: Server? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val server = activeServerForVpn
            if (server != null) {
                vpnManager.startVpn(server, this)
            }
        }
    }

    private val customOvpnLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val ovpnText = stream.bufferedReader().readText()
                    if (ovpnText.isNotBlank() && ("client" in ovpnText || "dev tun" in ovpnText || "remote" in ovpnText)) {
                        val customServer = Server(
                            id = "custom_${System.currentTimeMillis()}",
                            protocol = "OPENVPN",
                            engine = "OPENVPN",
                            name = "Custom OVPN Profile",
                            countryCode = "US",
                            countryName = "Custom Imported Server",
                            ping = 20,
                            signal = 3,
                            ovpnConfig = ovpnText,
                            tier = "custom"
                        )
                        lifecycleScope.launch {
                            serverRepository.addCustomServer(customServer)
                            onCustomServerImported?.invoke(customServer)
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle read error
            }
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
            val coroutineScope = rememberCoroutineScope()

            CorvusVPNTheme(themeMode = themeMode) {
                // Instant sync on app open (VPNGate as primary source)
                LaunchedEffect(Unit) {
                    serverRepository.syncServers()
                }

                val vpnState by vpnManager.vpnState.collectAsState()
                val byteCount by vpnManager.byteCount.collectAsState()
                val elapsedTime by sessionManager.elapsedTime.collectAsState()
                val isRefreshing by serverRepository.isRefreshing.collectAsState()

                var currentScreen by remember { mutableStateOf("home") }
                var selectedServer by remember { mutableStateOf<Server?>(null) }
                var showConnectedDialog by remember { mutableStateOf(false) }
                var hasShownDialogForCurrentSession by remember { mutableStateOf(false) }

                onCustomServerImported = { imported ->
                    selectedServer = imported
                    currentScreen = "home"
                }

                val serverEntities by serverRepository.serversFlow.collectAsState()
                val realServers = remember(serverEntities) { loadServersFromEntities(serverEntities) }

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
                    } else if (vpnState is VpnState.Idle || vpnState is VpnState.Error) {
                        sessionManager.stopSession()
                        hasShownDialogForCurrentSession = false
                        showConnectedDialog = false
                    }
                }

                if (showConnectedDialog) {
                    com.corvus.vpn.ui.components.ConnectedSuccessDialog(
                        server = selectedServer,
                        onDismiss = { showConnectedDialog = false }
                    )
                }

                when (currentScreen) {
                    "home" -> {
                        val selectLocationStr = stringResource(R.string.select_location)
                        val bestAutomaticStr = stringResource(R.string.best_automatic)
                        HomeScreen(
                            isConnected = isConnected,
                            isConnecting = isConnecting,
                            serverName = if (selectedServer?.id == "best_automatic") bestAutomaticStr else (selectedServer?.name ?: selectLocationStr),
                            countryCode = if (selectedServer == null) "🌐" else if (selectedServer?.id == "best_automatic") "⚡" else CountryUtils.getFlagEmoji(selectedServer?.countryCode ?: ""),
                            serverTier = selectedServer?.tier ?: "free",
                            selectedMode = vpnProtocolMode,
                            onModeChange = { newMode ->
                                vpnProtocolMode = newMode
                                settingsRepository.vpnProtocolMode = newMode
                            },
                            duration = if (isConnected) sessionManager.formatTime(elapsedTime) else "00:00",
                            ping = if (selectedServer == null) "0 ms" else "${selectedServer?.ping ?: 0} ms",
                            downloadSpeed = if (isConnected) "${byteCount.third / 1024} KB/s" else "0.0 Mbps",
                            uploadSpeed = if (isConnected) "Active" else "0.0 Mbps",
                            unityAdsManager = unityAdsManager,
                            onToggleClick = {
                                if (isConnected || isConnecting) {
                                    vpnManager.stopVpn()
                                } else {
                                    if (selectedServer == null) {
                                        currentScreen = "servers"
                                    } else {
                                        startVpnConnection(selectedServer)
                                    }
                                }
                            },
                            onServerClick = { currentScreen = "servers" },
                            onSettingsClick = { currentScreen = "settings" },
                            onProClick = { currentScreen = "pro" },
                            onAddTimeClick = {
                                val mins = when (selectedServer?.tier) {
                                    "premium_plus" -> 5
                                    "premium" -> 10
                                    "custom" -> 30
                                    else -> 20
                                }
                                unityAdsManager.showRewardedAd(this@MainActivity) {
                                    sessionManager.addTime(mins * 60)
                                }
                            }
                        )
                    }
                    "servers" -> {
                        ServerSelectionScreen(
                            servers = realServers,
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                coroutineScope.launch {
                                    serverRepository.syncServers()
                                }
                            },
                            onServerSelect = { server ->
                                selectedServer = server
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
                            onKillSwitchChange = {
                                killSwitchEnabled = it
                                settingsRepository.killSwitchEnabled = it
                            },
                            autoStartEnabled = autoStartEnabled,
                            onAutoStartChange = {
                                autoStartEnabled = it
                                settingsRepository.autoStartEnabled = it
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
                            onMockGpsChange = {
                                mockGpsEnabled = it
                                settingsRepository.mockGpsEnabled = it
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
                            onImportCustomOvpnClick = {
                                unityAdsManager.showRewardedAd(this@MainActivity) {
                                    customOvpnLauncher.launch("*/*")
                                }
                            },
                            onMyIpClick = {
                                unityAdsManager.showInterstitialAd(this@MainActivity)
                                currentScreen = "my_ip"
                            },
                            onProClick = { currentScreen = "pro" },
                            onAboutClick = { currentScreen = "about" },
                            onLanguageSelected = { code ->
                                settingsRepository.language = code
                            },
                            onBack = { currentScreen = "home" }
                        )
                    }
                    "my_ip" -> {
                        IpInfoScreen(onBack = { currentScreen = "settings" })
                    }
                    "about" -> { AboutScreen(onBack = { currentScreen = "settings" }) }
                    "pro" -> {
                        ProScreen(
                            totalNodes = serverEntities.size,
                            totalCountries = realServers.size,
                            onClose = { currentScreen = "home" },
                            onPurchase = { isPro = true; currentScreen = "home" }
                        )
                    }
                }
            }
        }
    }

    private fun startVpnConnection(server: Server?) {
        if (server == null) return
        var targetServer = server
        if (server.id == "best_automatic") {
            val entities = serverRepository.serversFlow.value
            val bestEntity = entities.maxByOrNull { it.score ?: 0 }
            if (bestEntity != null) {
                val list = loadServersFromEntities(listOf(bestEntity))
                targetServer = list.values.flatten().firstOrNull() ?: server
            }
        }
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
        vpnManager.startVpn(targetServer, this)
    }


    private fun loadServersFromEntities(entities: List<com.corvus.vpn.data.ServerEntity>): Map<String, List<Server>> {
        val validEntities = entities.filter { entity ->
            entity.tier == "custom" || entity.source == "custom_file" || (
                entity.countryCode.length == 2 &&
                entity.countryCode != "UN" &&
                !entity.countryName.equals("Unknown", ignoreCase = true) &&
                !entity.countryName.equals("Other", ignoreCase = true) &&
                entity.countryName.isNotBlank()
            )
        }

        val countryIndexMap = mutableMapOf<String, Int>()

        val list = validEntities.map { entity ->
            val isCustom = entity.tier == "custom" || entity.source == "custom_file"
            val country = if (isCustom) "Custom" else entity.countryName
            val idx = countryIndexMap.getOrDefault(country, 0) + 1
            countryIndexMap[country] = idx

            val code = if (entity.countryCode.length == 2 && entity.countryCode != "UN") {
                entity.countryCode
            } else {
                getCodeFromName(entity.countryName) ?: "US"
            }

            Server(
                id = entity.id,
                protocol = entity.protocol,
                engine = entity.engine,
                name = if (isCustom) "Custom OpenVPN File" else "$country Secure Node #$idx",
                countryCode = if (isCustom) "US" else code,
                countryName = if (isCustom) "Custom Imported Server" else entity.countryName,
                ping = entity.ping,
                speed = entity.speed,
                signal = if (entity.ping == null) 3 else (if (entity.ping < 100) 3 else (if (entity.ping < 200) 2 else 1)),
                configUri = entity.configUri,
                ovpnConfig = entity.ovpnConfig,
                tier = entity.tier
            )
        }
        return list.sortedBy { it.ping ?: 999 }.groupBy { it.countryName ?: "Custom Imported Server" }
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
}
