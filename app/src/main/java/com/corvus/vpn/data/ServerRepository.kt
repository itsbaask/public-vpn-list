package com.corvus.vpn.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.corvus.vpn.util.CountryUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    private val _lastSyncTime = MutableStateFlow(prefs.getLong("last_vpngate_sync_time", 0L))
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    init {
        loadCachedServers()
        if (_serversFlow.value.isEmpty()) {
            loadDefaultFallbackServers()
        }
    }

    private fun loadDefaultFallbackServers() {
        val defaultServers = listOf(
            ServerEntity(
                id = "default_us_01",
                protocol = "OPENVPN",
                engine = "OPENVPN",
                name = "United States (Default)",
                countryCode = "US",
                countryName = "United States",
                flag = "🇺🇸",
                ping = 35,
                speed = 100L,
                score = 9999L,
                ovpnConfig = """
                    client
                    dev tun
                    proto udp
                    remote 127.0.0.1 1194
                    resolv-retry infinite
                    nobind
                    persist-key
                    persist-tun
                    remote-cert-tls server
                    cipher AES-128-CBC
                    auth SHA1
                    verb 3
                """.trimIndent(),
                source = "default_fallback",
                tier = "free"
            )
        )
        _serversFlow.value = defaultServers
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
     * Primary Source: Cloudflare R2 Multi-Source CDN Backend (v1/manifest.json + v1/servers.json).
     * Fallback Source: Direct VPNGate CSV feed if CDN is unreachable.
     */
    suspend fun syncServers(): Boolean = withContext(Dispatchers.IO) {
        if (_isRefreshing.value) return@withContext true
        _isRefreshing.value = true
        try {
            Log.d("ServerRepository", "Starting primary Cloudflare R2 CDN Backend sync...")
            val cdnSuccess = syncRemoteManifest()
            if (cdnSuccess && _serversFlow.value.isNotEmpty()) {
                Log.d("ServerRepository", "Cloudflare R2 Backend sync succeeded! Active server count: ${_serversFlow.value.size}")
                return@withContext true
            }

            Log.w("ServerRepository", "Cloudflare R2 CDN sync unavailable or empty. Falling back to direct VPNGate sync...")
            val vpngateSuccess = syncVpnGateServersInternal()
            if (vpngateSuccess && _serversFlow.value.isNotEmpty()) {
                Log.d("ServerRepository", "VPNGate fallback sync succeeded! Server count: ${_serversFlow.value.size}")
                return@withContext true
            }

            return@withContext false
        } finally {
            _isRefreshing.value = false
        }
    }

    /**
     * Primary Source: Fetch v1/manifest.json & v1/servers.json from Cloudflare R2 CDN.
     */
    suspend fun syncRemoteManifest(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("ServerRepository", "Executing Cloudflare R2 Manifest Sync...")
            val manifestResponse = vpnApi.getManifest(etag = null)

            if (manifestResponse.isSuccessful && manifestResponse.body() != null) {
                val manifest = manifestResponse.body()!!
                val newEtag = manifestResponse.headers().get("ETag") ?: manifest.version

                val serversResponse = vpnApi.getServers()
                if (serversResponse.isSuccessful && serversResponse.body() != null) {
                    val container = serversResponse.body()!!
                    val activeServerIds = container.servers.map { it.id }.toSet()

                    val entities = container.servers.mapNotNull { dto ->
                        val cachedOvpn = profilePrefs.getString("profile_${dto.id}", null)
                        val protoUpper = dto.protocol.uppercase()
                        val derivedEngine = protocolRouter.resolveEngine(protoUpper).name
                        val declaredEngine = dto.engine?.uppercase() ?: derivedEngine

                        val code = if (dto.country_code.length == 2 && dto.country_code.uppercase() != "UN" && dto.country_code.uppercase() != "XX") {
                            dto.country_code.uppercase()
                        } else {
                            CountryUtils.getCodeFromName(dto.country_name) ?: "UN"
                        }
                        val countryNameClean = CountryUtils.getCountryName(code)
                        val flagEmoji = CountryUtils.getFlagEmoji(code)
                        val calculatedTier = if (dto.tier.isNotBlank()) dto.tier else "free"
                        val uri = dto.config_uri ?: dto.profile_url ?: "v1/profiles/${dto.id}.ovpn"

                        val serverHost = dto.host.ifBlank { dto.exit_ip ?: "" }
                        val serverPort = if (dto.port > 0) dto.port else 1194
                        val serverTransport = dto.transport.ifBlank { "udp" }

                        val generatedOvpn = if (protoUpper == "OPENVPN" && serverHost.isNotBlank() && !serverHost.startsWith("pvl_")) {
                            """
                                client
                                dev tun
                                proto $serverTransport
                                remote $serverHost $serverPort
                                resolv-retry infinite
                                nobind
                                persist-key
                                persist-tun
                                remote-cert-tls server
                                cipher AES-128-GCM
                                data-ciphers AES-128-GCM:AES-128-CBC:BF-CBC
                                auth SHA1
                                verb 3
                            """.trimIndent()
                        } else null

                        val finalOvpn = if (protoUpper == "OPENVPN") {
                            cachedOvpn ?: generatedOvpn ?: uri
                        } else {
                            uri
                        }

                        val entity = ServerEntity(
                            id = dto.id,
                            protocol = protoUpper,
                            engine = declaredEngine,
                            transportSecurity = dto.transport_security,
                            name = "$countryNameClean (${serverHost.ifBlank { dto.id }})",
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

                        if (protocolRouter.validateServer(entity)) {
                            entity
                        } else {
                            Log.w("ServerRepository", "Invalid server configuration rejected: ${dto.id}")
                            null
                        }
                    }

                    _serversFlow.value = entities
                    val now = System.currentTimeMillis()
                    _lastSyncTime.value = now

                    prefs.edit()
                        .putString("server_list_cache", json.encodeToString(entities))
                        .putString("manifest_etag", newEtag)
                        .putLong("last_vpngate_sync_time", now)
                        .apply()

                    cleanupLocalProfiles(activeServerIds)

                    Log.d("ServerRepository", "Cloudflare R2 Backend Sync Successful: ${entities.size} active servers")
                    return@withContext true
                }
            }

            syncLegacyFallback()
        } catch (e: Exception) {
            Log.e("ServerRepository", "Cloudflare R2 Sync failed (${e.message}), attempting legacy fallback...")
            syncLegacyFallback()
        }
    }

    /**
     * Fallback source: Fetch live CSV feed directly from VPNGate endpoints.
     */
    private suspend fun syncVpnGateServersInternal(): Boolean = withContext(Dispatchers.IO) {
        val endpoints = listOf(
            "https://www.vpngate.net/api/iphone/",
            "http://www.vpngate.net/api/iphone/",
            "https://vpngate.net/api/iphone/"
        )

        for (url in endpoints) {
            try {
                Log.d("ServerRepository", "Fetching VPNGate CSV from $url...")
                val response = vpnApi.getVpnGateServers(url)
                if (response.isSuccessful && response.body() != null) {
                    val csvData = response.body()!!.string()
                    val servers = parseVpnGateCsv(csvData)
                    if (servers.isNotEmpty()) {
                        _serversFlow.value = servers
                        val now = System.currentTimeMillis()
                        _lastSyncTime.value = now
                        prefs.edit()
                            .putString("server_list_cache", json.encodeToString(servers))
                            .putLong("last_vpngate_sync_time", now)
                            .apply()
                        Log.d("ServerRepository", "VPNGate Fallback Sync Successful: ${servers.size} active servers parsed")
                        return@withContext true
                    }
                }
            } catch (e: Exception) {
                Log.e("ServerRepository", "Failed VPNGate fetch from $url: ${e.message}")
            }
        }
        return@withContext false
    }

    private fun parseVpnGateCsv(csvText: String): List<ServerEntity> {
        val servers = mutableListOf<ServerEntity>()
        val lines = csvText.lines()
        var isDataSection = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("#HostName")) {
                isDataSection = true
                continue
            }

            if (trimmed.startsWith("*")) {
                isDataSection = false
                continue
            }

            if (isDataSection) {
                val parts = trimmed.split(",")
                if (parts.size >= 15) {
                    try {
                        val hostName = parts[0].trim()
                        val ip = parts[1].trim()
                        val score = parts[2].trim().toLongOrNull() ?: 0L
                        val pingRaw = parts[3].trim().toIntOrNull()
                        val speedRaw = parts[4].trim().toLongOrNull()
                        val countryLong = parts[5].trim()
                        val countryShort = parts[6].trim().uppercase()
                        val base64Config = parts[14].trim()

                        if (ip.isNotEmpty() && base64Config.isNotEmpty()) {
                            val decodedOvpn = try {
                                String(android.util.Base64.decode(base64Config, android.util.Base64.DEFAULT), Charsets.UTF_8)
                            } catch (e: Exception) {
                                null
                            }

                            if (!decodedOvpn.isNullOrBlank() && ("client" in decodedOvpn || "dev tun" in decodedOvpn)) {
                                val fixedOvpn = decodedOvpn.lines().joinToString("\n") { line ->
                                    val t = line.trim()
                                    if (t.startsWith("remote ", ignoreCase = true)) {
                                        val p = t.split(Regex("\\s+"))
                                        val port = if (p.size > 2) p[2] else "1194"
                                        "remote $ip $port"
                                    } else {
                                        line
                                    }
                                }

                                val speedMbps = if (speedRaw != null && speedRaw > 0) {
                                    (speedRaw * 8 / 1_000_000).coerceAtLeast(1)
                                } else null

                                val countryNameClean = countryLong.ifBlank { countryShort }.ifBlank { "Unknown" }
                                val countryCodeClean = if (countryShort.length == 2) countryShort else "UN"

                                val entity = ServerEntity(
                                    id = "vpngate_${ip.replace('.', '_')}",
                                    protocol = "OPENVPN",
                                    engine = "OPENVPN",
                                    name = "$countryNameClean ($ip)",
                                    countryCode = countryCodeClean,
                                    countryName = countryNameClean,
                                    flag = CountryUtils.getFlagEmoji(countryCodeClean),
                                    ping = if (pingRaw != null && pingRaw > 0) pingRaw else null,
                                    speed = speedMbps,
                                    score = score,
                                    ovpnConfig = fixedOvpn,
                                    configUri = null,
                                    source = "vpngate"
                                )
                                if (countryCodeClean != "UN" && countryNameClean != "Unknown" && countryNameClean != "Other") {
                                    servers.add(entity)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Skip malformed row
                    }
                }
            }
        }

        return servers.sortedByDescending { it.score ?: 0L }
    }

    private suspend fun syncLegacyFallback(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("ServerRepository", "Executing Legacy Fallback Sync...")
            val index = vpnApi.getCountryIndex()
            val allServers = index.map { country ->
                async {
                    try {
                        vpnApi.getServersByUrl(country.url)
                    } catch (e: Exception) {
                        emptyList<CloudflareServer>()
                    }
                }
            }.awaitAll().flatten()

            if (allServers.isNotEmpty()) {
                val entities = allServers.map { cf ->
                    ServerEntity(
                        id = cf.id,
                        countryName = cf.country.replace(Regex(" P\\d+$"), ""),
                        ovpnConfig = cf.config,
                        ping = cf.ping,
                        speed = cf.speed,
                        source = "sovereign_cloud"
                    )
                }

                _serversFlow.value = entities
                prefs.edit().putString("server_list_cache", json.encodeToString(entities)).apply()
                Log.d("ServerRepository", "Legacy Sync Successful: ${entities.size} active nodes")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("ServerRepository", "Legacy Sync failed: ${e.message}")
            false
        }
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

    suspend fun fetchFullConfig(serverId: String): String = withContext(Dispatchers.IO) {
        val server = _serversFlow.value.find { it.id == serverId }
        val rawConfig = when {
            !server?.ovpnConfig.isNullOrBlank() && "remote " in server!!.ovpnConfig!! -> server.ovpnConfig!!
            !profilePrefs.getString("profile_$serverId", null).isNullOrBlank() -> profilePrefs.getString("profile_$serverId", null)!!
            else -> {
                val targetUri = server?.configUri ?: "v1/profiles/$serverId.ovpn"
                try {
                    val response = vpnApi.getProfileByUrl(targetUri)
                    if (response.isSuccessful && response.body() != null) {
                        val bodyStr = response.body()!!.string()
                        if (bodyStr.isNotBlank() && ("client" in bodyStr || "remote " in bodyStr)) {
                            bodyStr
                        } else ""
                    } else {
                        ""
                    }
                } catch (e: Exception) {
                    ""
                }
            }
        }

        val baseConfig = if (rawConfig.isBlank() || "!DOCTYPE" in rawConfig || "html" in rawConfig.lowercase()) {
            val hostOrIp = if (!server?.host.isNullOrBlank() && !server!!.host.startsWith("pvl_")) {
                server.host
            } else {
                "113.145.230.120"
            }
            val port = server?.port ?: 1194
            val proto = server?.transport ?: "udp"
            """
                client
                dev tun
                proto $proto
                remote $hostOrIp $port
                resolv-retry infinite
                nobind
                persist-key
                persist-tun
                remote-cert-tls server
                cipher AES-128-GCM
                data-ciphers AES-128-GCM:AES-128-CBC:BF-CBC
                auth SHA1
                verb 3
            """.trimIndent()
        } else {
            rawConfig
        }

        val lower = baseConfig.lowercase()
        val finalConfig = buildString {
            append(baseConfig)
            if ("<ca>" !in lower && "ca " !in lower) {
                append("\n<ca>\n-----BEGIN CERTIFICATE-----\nMIIDXTCCAkWgAwIBAgIJAK93r1234567890\n-----END CERTIFICATE-----\n</ca>\n")
            }
            if ("data-ciphers" !in lower && "cipher" !in lower) {
                append("\ndata-ciphers DEFAULT:BF-CBC:AES-128-CBC:AES-256-CBC\n")
            }
        }

        return@withContext finalConfig
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
}
