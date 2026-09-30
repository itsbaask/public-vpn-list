package com.corvus.vpn.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.corvus.vpn.util.CountryUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class ServerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vpnApi: VpnApi,
    private val json: Json,
    private val protocolRouter: com.corvus.vpn.vpn.ProtocolRouter
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sovereign_cache_v5", Context.MODE_PRIVATE)
    private val profilePrefs: SharedPreferences = context.getSharedPreferences("ovpn_profiles_cache", Context.MODE_PRIVATE)

    private val _serversFlow = MutableStateFlow<List<ServerEntity>>(emptyList())
    val serversFlow: StateFlow<List<ServerEntity>> = _serversFlow.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(prefs.getLong("last_cdn_sync_time", 0L))
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    init {
        loadCachedServers()
    }

    private fun loadCachedServers() {
        val cachedJson = prefs.getString("server_list_cache", null)
        if (cachedJson != null) {
            try {
                val servers = json.decodeFromString<List<ServerEntity>>(cachedJson)
                _serversFlow.value = servers
            } catch (e: Exception) {
                Log.e("ServerRepository", "Cache corrupt", e)
            }
        }
    }

    /**
     * Main sync entrypoint.
     * GitHub Actions is the only source orchestrator; the app consumes the
     * validated artifact published by the Cloudflare CDN backend.
     */
    suspend fun syncServers(): Boolean = withContext(Dispatchers.IO) {
        if (_isRefreshing.value) return@withContext true
        _isRefreshing.value = true
        try {
            Log.d("ServerRepository", "Starting GitHub artifact → Cloudflare CDN sync...")

            val cdnServers = fetchCdnListInternal()

            if (cdnServers.isNotEmpty()) {
                _serversFlow.value = cdnServers
                val now = System.currentTimeMillis()
                _lastSyncTime.value = now

                prefs.edit()
                    .putString("server_list_cache", json.encodeToString(cdnServers))
                    .putLong("last_cdn_sync_time", now)
                    .apply()

                Log.d("ServerRepository", "GitHub artifact sync successful: ${cdnServers.size} active real servers loaded")
                return@withContext true
            }

            return@withContext false
        } catch (e: Exception) {
            Log.e("ServerRepository", "Error in syncServers: ${e.message}")
            return@withContext false
        } finally {
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchCdnListInternal(): List<ServerEntity> = withContext(Dispatchers.IO) {
        try {
            Log.d("ServerRepository", "Executing Cloudflare R2 Manifest Sync...")
            val serversResponse = vpnApi.getServers()
            if (serversResponse.isSuccessful && serversResponse.body() != null) {
                val container = serversResponse.body()!!
                return@withContext container.servers.mapNotNull { dto ->
                    val cachedOvpn = profilePrefs.getString("profile_${dto.id}", null)
                    val protoUpper = dto.protocol.uppercase()
                    val derivedEngine = protocolRouter.resolveEngine(protoUpper).name
                    val declaredEngine = dto.engine?.uppercase() ?: derivedEngine

                    val (code, countryNameClean) = CountryUtils.resolveServerCountry(
                        dto.country_code,
                        dto.country_name,
                        dto.host,
                        dto.config_uri ?: dto.profile_url,
                        dto.id
                    )
                    val flagEmoji = CountryUtils.getFlagEmoji(code)
                    val isModernProto = protoUpper in setOf("VLESS", "VMESS", "TROJAN", "SHADOWSOCKS", "HYSTERIA2", "TUIC", "WIREGUARD", "SINGBOX")
                    val calculatedTier = when {
                        dto.tier.isNotBlank() -> dto.tier.lowercase()
                        isModernProto -> "premium"
                        else -> "free"
                    }

                    val cleanId = dto.id.removePrefix("pvl_")
                    val rawPath = dto.config_uri?.takeIf { it.isNotBlank() }
                        ?: dto.profile_url?.takeIf { it.isNotBlank() }
                        ?: "v1/profiles/$cleanId.ovpn"
                    val uri = rawPath.replace("/v1/profiles/pvl_", "/v1/profiles/")
                        .replace("v1/profiles/pvl_", "v1/profiles/")

                    val serverHost = dto.host.ifBlank { dto.exit_ip ?: "" }
                    val serverPort = if (dto.port > 0) dto.port else 1194
                    val serverTransport = dto.transport.ifBlank { "udp" }

                    val finalOvpn = if (protoUpper == "OPENVPN") {
                        cachedOvpn
                    } else {
                        uri
                    }

                    val serverNameDisplay = when {
                        serverHost.isNotBlank() && !serverHost.startsWith("pvl_") -> "$countryNameClean ($serverHost)"
                        else -> "$countryNameClean (${dto.id})"
                    }

                    val entity = ServerEntity(
                        id = dto.id,
                        protocol = protoUpper,
                        engine = declaredEngine,
                        transportSecurity = dto.transport_security,
                        name = serverNameDisplay,
                        countryCode = code,
                        countryName = countryNameClean,
                        flag = flagEmoji,
                        ping = if (dto.latency_ms > 0) dto.latency_ms else null,
                        speed = if (dto.speed_mbps > 0) dto.speed_mbps.toLong() else null,
                        score = dto.network_score.toLong(),
                        ovpnConfig = finalOvpn,
                        configUri = uri,
                        source = "cdn_r2",
                        tier = calculatedTier,
                        host = serverHost,
                        port = serverPort,
                        transport = serverTransport
                    )

                    if (protocolRouter.validateServer(entity)) entity else null
                }
            }
        } catch (e: Exception) {
            Log.e("ServerRepository", "CDN fetch failed: ${e.message}")
        }
        return@withContext emptyList()
    }

    private fun cleanupLocalProfiles(activeServerIds: Set<String>) {
        val editor = profilePrefs.edit()
        val allKeys = profilePrefs.all.keys
        for (key in allKeys) {
            if (key.startsWith("profile_")) {
                val serverId = key.removePrefix("profile_")
                if (serverId !in activeServerIds) {
                    editor.remove(key)
                }
            }
        }
        editor.apply()
    }

    fun getServers(): List<ServerEntity> = _serversFlow.value

    private fun extractRemoteHostPort(ovpnText: String): Pair<String, Int> {
        val remoteRegex = Regex("""(?i)^\s*remote\s+([^\s]+)(?:\s+(\d+))?""", RegexOption.MULTILINE)
        val match = remoteRegex.find(ovpnText)
        val host = match?.groupValues?.get(1)?.trim() ?: ""
        val port = match?.groupValues?.get(2)?.toIntOrNull() ?: 1194
        return Pair(host, port)
    }

    /**
     * Fetches the complete OpenVPN config for a server.
     *
     * Priority order:
     * 1. Local cache (profilePrefs) — fastest, avoids redundant network calls.
     * 2. Real .ovpn download from R2 CDN via [VpnApi.getProfileByUrl].
     * 3. A validated embedded profile from the Worker artifact.
     *
     * Returns null if no valid config could be obtained.
     * The caller (OpenVpnEngine) must handle null by showing an error to the user.
     */
    suspend fun fetchFullConfig(serverId: String): String? = withContext(Dispatchers.IO) {
        val cleanServerId = serverId
            .removePrefix("test_openvpn_udp_")
            .removePrefix("test_openvpn_tcp_")
            .removePrefix("test_custom_file_")
            .removePrefix("test_")

        val server = _serversFlow.value.find { it.id == serverId || it.id == cleanServerId }
        val protoUpper = (server?.protocol ?: "").uppercase()
        val isModernProto = protoUpper in setOf("VLESS", "VMESS", "TROJAN", "SHADOWSOCKS", "HYSTERIA2", "TUIC", "WIREGUARD", "SINGBOX")

        // 1. Return cached profile if available and non-blank
        val cached = profilePrefs.getString("profile_$cleanServerId", null)
            ?: profilePrefs.getString("profile_$serverId", null)
        if (!cached.isNullOrBlank()) {
            if (isModernProto && cached.contains("://") && !cached.startsWith("http")) {
                Log.d("ServerRepository", "Using cached modern URI for server=$serverId")
                return@withContext cached.trim()
            }
            if (cached.contains("client") || cached.contains("dev tun")) {
                Log.d("ServerRepository", "Using cached profile for server=$serverId")
                return@withContext cached
            }
        }

        // 2. If the server already has a full embedded config / URI, use it directly
        val embedded = server?.ovpnConfig ?: server?.configUri
        if (!embedded.isNullOrBlank()) {
            if (isModernProto && embedded.contains("://") && !embedded.startsWith("http") && !embedded.startsWith("v1/")) {
                Log.d("ServerRepository", "Using embedded modern URI for server=$serverId")
                profilePrefs.edit().putString("profile_$serverId", embedded.trim()).apply()
                return@withContext embedded.trim()
            }
            if (isValidOvpnConfig(embedded)) {
                Log.d("ServerRepository", "Using embedded ovpn config for server=$serverId")
                profilePrefs.edit().putString("profile_$serverId", embedded).apply()
                return@withContext embedded
            }
        }

        // 3. Download from the Corvus Worker gateway or a validated HTTPS URL
        val CDN_BASE = "https://vpn-backend.xocqoc.workers.dev/"
        val cleanId = serverId.removePrefix("pvl_")
        val cleanStorageId = serverId.lowercase()
            .replace(Regex("[^a-z0-9\\-]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')

        val candidatePaths = listOfNotNull(
            server?.configUri?.takeIf { it.isNotBlank() }?.replace("/v1/profiles/pvl_", "/v1/profiles/"),
            if (isModernProto) null else "v1/profiles/$cleanId.ovpn",
            if (isModernProto) null else "v1/profiles/$cleanStorageId.ovpn",
            server?.configUri?.takeIf { it.isNotBlank() },
            server?.ovpnConfig?.takeIf { it.startsWith("v1/profiles/") },
            if (isModernProto) null else "v1/profiles/$serverId.ovpn"
        ).distinct()

        for (candidate in candidatePaths) {
            val profileUrl = when {
                candidate.startsWith("http://") || candidate.startsWith("https://") -> candidate
                else -> CDN_BASE + candidate.trimStart('/')
            }

            Log.d("ServerRepository", "Attempting profile download: $profileUrl (server=$serverId)")
            try {
                val response = vpnApi.getProfileByUrl(profileUrl)
                if (response.isSuccessful && response.body() != null) {
                    val contentText = response.body()!!.string().trim()
                    if (isModernProto && contentText.contains("://") && !contentText.startsWith("http")) {
                        Log.d("ServerRepository", "Real modern URI downloaded from $profileUrl for server=$serverId (${contentText.length} chars)")
                        profilePrefs.edit().putString("profile_$serverId", contentText).apply()
                        return@withContext contentText
                    }
                    if (isValidOvpnConfig(contentText)) {
                        Log.d("ServerRepository", "Real .ovpn downloaded successfully from $profileUrl for server=$serverId (${contentText.length} chars)")
                        profilePrefs.edit().putString("profile_$serverId", contentText).apply()
                        return@withContext contentText
                    } else {
                        Log.w("ServerRepository", "Content from $profileUrl did not contain valid profile directives")
                    }
                } else {
                    Log.w("ServerRepository", "HTTP ${response.code()} from $profileUrl, trying next candidate...")
                }
            } catch (e: Exception) {
                Log.w("ServerRepository", "Failed from $profileUrl: ${e.message}")
            }
        }

        // 4. Fallback: If embedded config is non-blank and not a path, use it
        if (!embedded.isNullOrBlank() && !embedded.startsWith("v1/")) {
            Log.d("ServerRepository", "Using raw embedded config fallback for server=$serverId")
            profilePrefs.edit().putString("profile_$serverId", embedded).apply()
            return@withContext embedded
        }

        Log.e("ServerRepository", "No valid full config or CA certificate found for server=$serverId")
        return@withContext null
    }

    /**
     * Returns the connection URI for a modern-protocol server (vless://, vmess://, ss://, trojan://, etc.)
     * Used by XrayEngine.
     */
    fun getConnectionUri(serverId: String): String? {
        val server = _serversFlow.value.find { it.id == serverId } ?: return null

        val cached = profilePrefs.getString("profile_$serverId", null)
        if (!cached.isNullOrBlank() && cached.contains("://") && !cached.startsWith("http")) {
            return cached.trim()
        }

        val uri = server.ovpnConfig ?: server.configUri ?: return null
        val isDirectVpnUri = uri.contains("://") && !uri.startsWith("http://") && !uri.startsWith("https://") && !uri.startsWith("v1/")
        return if (isDirectVpnUri) uri.trim() else null
    }

    private fun isValidOvpnConfig(text: String): Boolean {
        if (text.isBlank() || text.length < 25) return false
        if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("v1/profiles/")) return false
        val lower = text.lowercase()
        return lower.contains("client") || lower.contains("dev tun") || lower.contains("remote ") || lower.contains("proto ")
    }


    suspend fun addCustomServer(customServer: com.corvus.vpn.ui.servers.Server) = withContext(Dispatchers.IO) {
        val entity = ServerEntity(
            id = customServer.id,
            protocol = customServer.protocol,
            engine = customServer.engine,
            name = customServer.name,
            countryCode = customServer.countryCode ?: "UN",
            countryName = customServer.countryName ?: "Custom",
            flag = "⚙️",
            ping = customServer.ping,
            speed = customServer.speed,
            score = 9999999L,
            ovpnConfig = customServer.ovpnConfig,
            source = "custom_file",
            tier = "custom"
        )
        val currentList = _serversFlow.value.toMutableList()
        currentList.add(0, entity)
        _serversFlow.value = currentList
        prefs.edit().putString("server_list_cache", json.encodeToString(currentList)).apply()
        if (!customServer.ovpnConfig.isNullOrBlank()) {
            profilePrefs.edit().putString("profile_${customServer.id}", customServer.ovpnConfig).apply()
        }
    }

    suspend fun removeCustomServer(serverId: String) = withContext(Dispatchers.IO) {
        val currentList = _serversFlow.value.toMutableList()
        currentList.removeAll { it.id == serverId }
        _serversFlow.value = currentList
        prefs.edit().putString("server_list_cache", json.encodeToString(currentList)).apply()
        profilePrefs.edit().remove("profile_$serverId").apply()
    }
}
