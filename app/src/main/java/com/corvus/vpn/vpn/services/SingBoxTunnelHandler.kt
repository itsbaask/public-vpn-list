package com.corvus.vpn.vpn.services

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * SingBoxTunnelHandler — Parses connection URIs for modern VPN protocols and generates
 * sing-box JSON configs for use when libbox.aar is available.
 *
 * Current mode: raw TCP/TLS socket relay (works without libbox).
 *
 * When libbox.aar is placed in app/libs/, SingBoxVpnService will use
 * the full sing-box engine with proper protocol framing for all protocols.
 *
 * Supported URI schemes: vless://, vmess://, trojan://, ss://, hy2://, tuic://, wg://
 */
class SingBoxTunnelHandler private constructor(
    val scheme: String,
    val remoteHost: String,
    val remotePort: Int,
    private val rawUri: String
) : AutoCloseable {

    companion object {
        private const val TAG = "SingBoxTunnelHandler"
        private const val BUFFER_SIZE = 8192
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 60_000

        private val SUPPORTED_SCHEMES = setOf(
            "vless", "vmess", "trojan", "ss", "shadowsocks",
            "hysteria2", "hy2", "tuic", "wireguard", "wg"
        )

        fun fromUri(uri: String): SingBoxTunnelHandler {
            return try {
                val scheme = uri.substringBefore("://").lowercase()
                val rest = uri.substringAfter("://")
                val withoutQuery = rest.substringBefore("?").substringBefore("#")
                val hostPart = withoutQuery.substringBefore("/")
                val hostOnly = if ("@" in hostPart) hostPart.substringAfterLast("@") else hostPart
                val host = hostOnly.trimEnd(':').substringBefore(":")
                val port = hostOnly.substringAfterLast(":", "").toIntOrNull() ?: defaultPortForScheme(scheme)
                SingBoxTunnelHandler(scheme, host, port, uri)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse URI: ${e.message}")
                SingBoxTunnelHandler("unknown", "", 0, uri)
            }
        }

        private fun defaultPortForScheme(scheme: String): Int = when (scheme) {
            "vless", "vmess", "trojan" -> 443
            "ss", "shadowsocks" -> 8388
            "hy2", "hysteria2", "tuic" -> 443
            "wg", "wireguard" -> 51820
            else -> 443
        }
    }

    private var socket: Socket? = null
    private var remoteIn: InputStream? = null
    private var remoteOut: OutputStream? = null
    @Volatile private var relayRunning = false

    /**
     * Builds a sing-box JSON config from this URI.
     * Used by SingBoxVpnService when libbox.aar is present.
     */
    fun buildSingBoxConfig(): String = buildBaseSingBoxConfig(scheme, remoteHost, remotePort, rawUri)

    /**
     * Establishes a TCP or TLS connection to the remote server.
     * Used in raw-relay mode (when libbox is not available).
     */
    fun connect(vpnService: VpnService, host: String, port: Int): Boolean {
        return try {
            val useTls = scheme in setOf("vless", "trojan", "vmess", "hy2", "hysteria2", "tuic")
                || rawUri.contains("security=tls") || rawUri.contains("security=reality")
            if (useTls) connectTls(vpnService, host, port) else connectPlain(vpnService, host, port)
        } catch (e: Exception) {
            Log.e(TAG, "connect() error: ${e.message}")
            false
        }
    }

    private fun connectPlain(vpnService: VpnService, host: String, port: Int): Boolean {
        val sock = Socket()
        vpnService.protect(sock) // exclude from VPN routing loop
        sock.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
        sock.soTimeout = READ_TIMEOUT_MS
        socket = sock
        remoteIn = sock.getInputStream()
        remoteOut = sock.getOutputStream()
        Log.d(TAG, "TCP connected to $host:$port")
        return true
    }

    private fun connectTls(vpnService: VpnService, host: String, port: Int): Boolean {
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        })
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, trustAll, SecureRandom())

        val raw = Socket()
        vpnService.protect(raw)
        raw.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
        raw.soTimeout = READ_TIMEOUT_MS

        val sni = extractSni() ?: host
        val ssl = ctx.socketFactory.createSocket(raw, sni, port, true) as SSLSocket
        ssl.startHandshake()
        socket = ssl
        remoteIn = ssl.getInputStream()
        remoteOut = ssl.getOutputStream()
        Log.d(TAG, "TLS connected to $host:$port (SNI=$sni)")
        return true
    }

    private fun extractSni(): String? {
        val params = parseQueryParams(rawUri)
        return params["sni"] ?: params["servername"] ?: params["peer"]
    }

    private fun parseQueryParams(uri: String): Map<String, String> {
        val query = uri.substringAfter("?", "").substringBefore("#")
        return query.split("&").mapNotNull { kv ->
            val parts = kv.split("=", limit = 2)
            if (parts.size == 2) parts[0].lowercase() to
                try { java.net.URLDecoder.decode(parts[1], "UTF-8") } catch (_: Exception) { parts[1] }
            else null
        }.toMap()
    }

    /**
     * Bidirectional relay: TUN fd ↔ Remote server socket.
     * Blocks until the tunnel is closed.
     */
    fun relay(tunFd: ParcelFileDescriptor) {
        relayRunning = true
        val tunIn = FileInputStream(tunFd.fileDescriptor)
        val tunOut = FileOutputStream(tunFd.fileDescriptor)
        val rIn = remoteIn ?: return
        val rOut = remoteOut ?: return

        // TUN → Remote
        val toRemote = Thread {
            try {
                val buf = ByteArray(BUFFER_SIZE)
                while (relayRunning) {
                    val n = tunIn.read(buf)
                    if (n < 0) break
                    if (n > 0) { rOut.write(buf, 0, n); rOut.flush() }
                }
            } catch (_: Exception) {}
        }.also { it.isDaemon = true; it.name = "SB-ToRemote"; it.start() }

        // Remote → TUN
        try {
            val buf = ByteArray(BUFFER_SIZE)
            while (relayRunning) {
                val n = rIn.read(buf)
                if (n < 0) break
                if (n > 0) tunOut.write(buf, 0, n)
            }
        } catch (_: Exception) {
        } finally {
            relayRunning = false
            toRemote.interrupt()
        }
    }

    override fun close() {
        relayRunning = false
        try { socket?.close() } catch (_: Exception) {}
        socket = null; remoteIn = null; remoteOut = null
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// sing-box JSON config builder — generates a complete config from a URI.
// This is used by SingBoxVpnService when libbox.aar is present.
// ─────────────────────────────────────────────────────────────────────────────

internal fun buildBaseSingBoxConfig(scheme: String, host: String, port: Int, rawUri: String): String {
    fun params(): Map<String, String> {
        val q = rawUri.substringAfter("?", "").substringBefore("#")
        return q.split("&").mapNotNull { kv ->
            val p = kv.split("=", limit = 2)
            if (p.size == 2) p[0].lowercase() to try { java.net.URLDecoder.decode(p[1], "UTF-8") } catch (_: Exception) { p[1] }
            else null
        }.toMap()
    }

    fun userInfo(): String {
        val rest = rawUri.substringAfter("://").substringBefore("?")
        return if ("@" in rest) rest.substringBefore("@") else ""
    }

    fun buildOutbound(): JSONObject {
        val p = params()
        val ui = userInfo()
        return when (scheme) {
            "vless" -> JSONObject().apply {
                put("type", "vless"); put("tag", "proxy")
                put("server", host); put("server_port", port)
                put("uuid", ui)
                val flow = p["flow"] ?: ""; if (flow.isNotBlank()) put("flow", flow)
                val net = p["type"] ?: "tcp"; val sec = p["security"] ?: "none"
                val sni = p["sni"] ?: p["servername"] ?: host
                if (net.isNotBlank() && net != "tcp") put("transport", buildTransport(net, p))
                put("tls", buildTls(sec, sni, p))
            }
            "vmess" -> buildVmessOutbound(rawUri, host, port)
            "trojan" -> JSONObject().apply {
                put("type", "trojan"); put("tag", "proxy")
                put("server", host); put("server_port", port)
                put("password", ui); val sni = p["sni"] ?: p["peer"] ?: host
                val net = p["type"] ?: "tcp"
                if (net.isNotBlank() && net != "tcp") put("transport", buildTransport(net, p))
                put("tls", buildTls("tls", sni, p))
            }
            "ss", "shadowsocks" -> buildShadowsocksOutbound(rawUri, host, port)
            "hy2", "hysteria2" -> JSONObject().apply {
                put("type", "hysteria2"); put("tag", "proxy")
                put("server", host); put("server_port", port)
                put("password", ui.ifBlank { p["auth"] ?: "" })
                put("tls", buildTls("tls", p["sni"] ?: host, p))
                val obfs = p["obfs"]; if (!obfs.isNullOrBlank()) put("obfs", JSONObject().apply {
                    put("type", obfs); put("password", p["obfs-password"] ?: "")
                })
            }
            "tuic" -> JSONObject().apply {
                put("type", "tuic"); put("tag", "proxy")
                put("server", host); put("server_port", port)
                put("uuid", ui.substringBefore(":")); put("password", ui.substringAfter(":", ""))
                put("congestion_control", p["congestion_control"] ?: "bbr")
                put("tls", buildTls("tls", p["sni"] ?: host, p).also { it.put("alpn", JSONArray().apply { put("h3") }) })
            }
            "wg", "wireguard" -> JSONObject().apply {
                put("type", "wireguard"); put("tag", "proxy")
                put("server", host); put("server_port", port)
                put("private_key", ui.ifBlank { p["privatekey"] ?: "" })
                put("peer_public_key", p["publickey"] ?: "")
                val psk = p["presharedkey"] ?: ""; if (psk.isNotBlank()) put("pre_shared_key", psk)
                put("local_address", JSONArray().apply { put(p["ip"] ?: "10.0.0.2/32") })
                put("mtu", 1420)
            }
            else -> JSONObject().apply { put("type", "direct"); put("tag", "proxy") }
        }
    }

    return JSONObject().apply {
        put("log", JSONObject().apply { put("level", "warn") })
        put("inbounds", JSONArray().apply {
            put(JSONObject().apply {
                put("type", "tun"); put("tag", "tun-in")
                put("inet4_address", "172.19.0.1/30"); put("inet6_address", "fdfe:dcba:9876::1/126")
                put("mtu", 9000); put("auto_route", true); put("strict_route", true)
                put("endpoint_independent_nat", true); put("stack", "mixed")
                put("sniff", true); put("sniff_override_destination", true)
            })
        })
        put("outbounds", JSONArray().apply {
            put(buildOutbound())
            put(JSONObject().apply { put("type", "direct"); put("tag", "direct") })
            put(JSONObject().apply { put("type", "block"); put("tag", "block") })
            put(JSONObject().apply { put("type", "dns"); put("tag", "dns-out") })
        })
        put("route", JSONObject().apply {
            put("rules", JSONArray().apply {
                put(JSONObject().apply { put("protocol", "dns"); put("outbound", "dns-out") })
            })
            put("final", "proxy"); put("auto_detect_interface", true)
        })
        put("dns", JSONObject().apply {
            put("servers", JSONArray().apply {
                put(JSONObject().apply { put("tag", "cf"); put("address", "https://1.1.1.1/dns-query") })
                put(JSONObject().apply { put("tag", "local"); put("address", "8.8.8.8"); put("detour", "direct") })
            })
            put("final", "cf"); put("strategy", "prefer_ipv4")
        })
    }.toString()
}

private fun buildTransport(net: String, p: Map<String, String>): JSONObject = when (net.lowercase()) {
    "ws", "websocket" -> JSONObject().apply {
        put("type", "ws"); put("path", p["path"] ?: "/")
        val h = p["host"] ?: ""; if (h.isNotBlank()) put("headers", JSONObject().apply { put("Host", h) })
    }
    "grpc" -> JSONObject().apply { put("type", "grpc"); put("service_name", p["servicename"] ?: p["service-name"] ?: "") }
    "http", "h2" -> JSONObject().apply { put("type", "http"); put("path", p["path"] ?: "/") }
    else -> JSONObject()
}

private fun buildTls(security: String, sni: String, p: Map<String, String>): JSONObject = JSONObject().apply {
    put("enabled", security in setOf("tls", "reality", "xtls"))
    put("server_name", sni); put("insecure", true)
    if (security == "reality") {
        put("reality", JSONObject().apply { put("enabled", true); put("public_key", p["pbk"] ?: ""); put("short_id", p["sid"] ?: "") })
        put("utls", JSONObject().apply { put("enabled", true); put("fingerprint", p["fp"] ?: "chrome") })
    }
}

private fun buildVmessOutbound(rawUri: String, host: String, port: Int): JSONObject {
    return try {
        val b64 = rawUri.substringAfter("vmess://").substringBefore("#")
        val json = JSONObject(String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT)))
        JSONObject().apply {
            put("type", "vmess"); put("tag", "proxy")
            put("server", json.optString("add", host)); put("server_port", json.optString("port", "$port").toIntOrNull() ?: port)
            put("uuid", json.optString("id")); put("alter_id", json.optInt("aid", 0)); put("security", "auto")
            val net = json.optString("net", "tcp"); val tls = json.optString("tls", "")
            if (net.isNotBlank() && net != "tcp") put("transport", buildTransport(net, mapOf("path" to json.optString("path", "/"), "host" to json.optString("host", ""))))
            if (tls == "tls") put("tls", JSONObject().apply { put("enabled", true); put("server_name", json.optString("sni", host)); put("insecure", true) })
        }
    } catch (_: Exception) {
        JSONObject().apply { put("type", "vmess"); put("tag", "proxy"); put("server", host); put("server_port", port); put("uuid", ""); put("security", "auto") }
    }
}

private fun buildShadowsocksOutbound(rawUri: String, host: String, port: Int): JSONObject {
    return try {
        val rest = rawUri.substringAfter("ss://").substringBefore("#").substringBefore("?")
        val (mp, hp) = if ("@" in rest) {
            val raw = try { String(android.util.Base64.decode(rest.substringBefore("@"), android.util.Base64.DEFAULT)) }
                      catch (_: Exception) { rest.substringBefore("@") }
            raw to rest.substringAfterLast("@")
        } else {
            val decoded = String(android.util.Base64.decode(rest, android.util.Base64.DEFAULT))
            val at = decoded.lastIndexOf('@')
            decoded.substring(0, at) to decoded.substring(at + 1)
        }
        val method = mp.substringBefore(":"); val pass = mp.substringAfter(":")
        val h = hp.substringBefore(":"); val p = hp.substringAfterLast(":").toIntOrNull() ?: 8388
        JSONObject().apply { put("type", "shadowsocks"); put("tag", "proxy"); put("server", h); put("server_port", p); put("method", method.ifBlank { "aes-128-gcm" }); put("password", pass) }
    } catch (_: Exception) {
        JSONObject().apply { put("type", "shadowsocks"); put("tag", "proxy"); put("server", host); put("server_port", port); put("method", "aes-128-gcm"); put("password", "") }
    }
}
