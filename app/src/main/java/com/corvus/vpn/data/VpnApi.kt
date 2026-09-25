package com.corvus.vpn.data

import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

@Serializable
data class ManifestChanges(
    val added: Int = 0,
    val removed: Int = 0,
    val updated: Int = 0
)

@Serializable
data class ManifestProfileInfo(
    val profile: String,
    val sha256: String,
    val size: Int = 0,
    val updated_at: String? = null
)

@Serializable
data class ManifestResponse(
    val schema_version: Int = 1,
    val version: String = "",
    val generated_at: String? = null,
    val source_snapshot: String? = null,
    val count: Int = 0,
    val servers_url: String = "/v1/servers.json",
    val profiles_prefix: String = "/v1/profiles/",
    val manifest_sha256: String? = null,
    val changes: ManifestChanges? = null,
    val servers: Map<String, ManifestProfileInfo> = emptyMap()
)

@Serializable
data class ServerDto(
    val id: String,
    val country_code: String = "XX",
    val country_name: String = "Unknown",
    val flag: String? = null,
    val host: String = "",
    val port: Int = 1194,
    val transport: String = "udp",
    val protocol: String = "openvpn",
    val engine: String? = null,
    val transport_security: String? = null,
    val exit_ip: String? = null,
    val speed_mbps: Float = 0f,
    val latency_ms: Int = 0,
    val network_score: Int = 0,
    val status: String = "verified",
    val checked_at: String? = null,
    val config_uri: String? = null,
    val profile_url: String? = null,
    val profile_sha256: String? = null,
    val tier: String = "free"
)

@Serializable
data class ServersContainerDto(
    val schema_version: Int = 1,
    val version: String = "",
    val count: Int = 0,
    val servers: List<ServerDto> = emptyList()
)

@Serializable
data class CountryIndex(
    val country: String,
    val count: Int,
    val url: String
)

@Serializable
data class CloudflareServer(
    val id: String,
    val country: String,
    val config: String,
    val ping: Int? = null,
    val speed: Long? = null,
    val updated_at: String? = null
)

interface VpnApi {
    @GET
    suspend fun getVpnGateServers(@Url url: String = "https://www.vpngate.net/api/iphone/"): Response<ResponseBody>

    @GET("v1/manifest.json")
    suspend fun getManifest(
        @Header("If-None-Match") etag: String? = null
    ): Response<ManifestResponse>

    @GET("v1/servers.json")
    suspend fun getServers(): Response<ServersContainerDto>

    @GET
    suspend fun getProfileByUrl(@Url profileUrl: String): Response<ResponseBody>

    // Legacy fallbacks
    @GET("index.json")
    suspend fun getCountryIndex(): List<CountryIndex>

    @GET
    suspend fun getServersByUrl(@Url url: String): List<CloudflareServer>
}
